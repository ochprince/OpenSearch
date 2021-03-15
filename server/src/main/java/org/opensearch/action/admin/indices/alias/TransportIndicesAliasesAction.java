/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.opensearch.action.admin.indices.alias;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.RequestValidators;
import org.elasticsearch.action.support.ActionFilters;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.action.support.master.TransportMasterNodeAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ack.ClusterStateUpdateResponse;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.block.ClusterBlockLevel;
import org.opensearch.cluster.metadata.AliasAction;
import org.opensearch.cluster.metadata.AliasMetadata;
import org.opensearch.cluster.metadata.IndexAbstraction;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.MetadataIndexAliasesService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.index.Index;
import org.opensearch.rest.action.admin.indices.AliasesNotFoundException;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.unmodifiableList;

/**
 * Add/remove aliases action
 */
public class TransportIndicesAliasesAction extends TransportMasterNodeAction<IndicesAliasesRequest, AcknowledgedResponse> {

    private static final Logger logger = LogManager.getLogger(TransportIndicesAliasesAction.class);

    private final MetadataIndexAliasesService indexAliasesService;
    private final RequestValidators<IndicesAliasesRequest> requestValidators;

    @Inject
    public TransportIndicesAliasesAction(
            final TransportService transportService,
            final ClusterService clusterService,
            final ThreadPool threadPool,
            final MetadataIndexAliasesService indexAliasesService,
            final ActionFilters actionFilters,
            final IndexNameExpressionResolver indexNameExpressionResolver,
            final RequestValidators<IndicesAliasesRequest> requestValidators) {
        super(IndicesAliasesAction.NAME, transportService, clusterService, threadPool, actionFilters, IndicesAliasesRequest::new,
            indexNameExpressionResolver);
        this.indexAliasesService = indexAliasesService;
        this.requestValidators = Objects.requireNonNull(requestValidators);
    }

    @Override
    protected String executor() {
        // we go async right away...
        return ThreadPool.Names.SAME;
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(IndicesAliasesRequest request, ClusterState state) {
        Set<String> indices = new HashSet<>();
        for (IndicesAliasesRequest.AliasActions aliasAction : request.aliasActions()) {
            Collections.addAll(indices, aliasAction.indices());
        }
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, indices.toArray(new String[indices.size()]));
    }

    @Override
    protected void masterOperation(final IndicesAliasesRequest request, final ClusterState state,
                                   final ActionListener<AcknowledgedResponse> listener) {

        //Expand the indices names
        List<IndicesAliasesRequest.AliasActions> actions = request.aliasActions();
        List<AliasAction> finalActions = new ArrayList<>();
        // Resolve all the AliasActions into AliasAction instances and gather all the aliases
        Set<String> aliases = new HashSet<>();
        for (IndicesAliasesRequest.AliasActions action : actions) {
            final Index[] concreteIndices = indexNameExpressionResolver.concreteIndices(state, request.indicesOptions(), false,
                action.indices());
            for (Index concreteIndex : concreteIndices) {
                IndexAbstraction indexAbstraction = state.metadata().getIndicesLookup().get(concreteIndex.getName());
                assert indexAbstraction != null : "invalid cluster metadata. index [" + concreteIndex.getName() + "] was not found";
                if (indexAbstraction.getParentDataStream() != null) {
                    throw new IllegalArgumentException("The provided expressions [" + String.join(",", action.indices())
                        + "] match a backing index belonging to data stream [" + indexAbstraction.getParentDataStream().getName()
                        + "]. Data streams and their backing indices don't support aliases.");
                }
            }
            final Optional<Exception> maybeException = requestValidators.validateRequest(request, state, concreteIndices);
            if (maybeException.isPresent()) {
                listener.onFailure(maybeException.get());
                return;
            }

            Collections.addAll(aliases, action.getOriginalAliases());
            for (final Index index : concreteIndices) {
                switch (action.actionType()) {
                case ADD:
                    for (String alias : concreteAliases(action, state.metadata(), index.getName())) {
                        finalActions.add(new AliasAction.Add(index.getName(), alias, action.filter(), action.indexRouting(),
                            action.searchRouting(), action.writeIndex(), action.isHidden()));
                    }
                    break;
                case REMOVE:
                    for (String alias : concreteAliases(action, state.metadata(), index.getName())) {
                        finalActions.add(new AliasAction.Remove(index.getName(), alias, action.mustExist()));
                    }
                    break;
                case REMOVE_INDEX:
                    finalActions.add(new AliasAction.RemoveIndex(index.getName()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported action [" + action.actionType() + "]");
                }
            }
        }
        if (finalActions.isEmpty() && false == actions.isEmpty()) {
            throw new AliasesNotFoundException(aliases.toArray(new String[aliases.size()]));
        }
        request.aliasActions().clear();
        IndicesAliasesClusterStateUpdateRequest updateRequest = new IndicesAliasesClusterStateUpdateRequest(unmodifiableList(finalActions))
                .ackTimeout(request.timeout()).masterNodeTimeout(request.masterNodeTimeout());

        indexAliasesService.indicesAliases(updateRequest, new ActionListener<ClusterStateUpdateResponse>() {
            @Override
            public void onResponse(ClusterStateUpdateResponse response) {
                listener.onResponse(new AcknowledgedResponse(response.isAcknowledged()));
            }

            @Override
            public void onFailure(Exception t) {
                logger.debug("failed to perform aliases", t);
                listener.onFailure(t);
            }
        });
    }

    private static String[] concreteAliases(IndicesAliasesRequest.AliasActions action, Metadata metadata, String concreteIndex) {
        if (action.expandAliasesWildcards()) {
            //for DELETE we expand the aliases
            String[] indexAsArray = {concreteIndex};
            ImmutableOpenMap<String, List<AliasMetadata>> aliasMetadata = metadata.findAliases(action, indexAsArray);
            List<String> finalAliases = new ArrayList<>();
            for (ObjectCursor<List<AliasMetadata>> curAliases : aliasMetadata.values()) {
                for (AliasMetadata aliasMeta: curAliases.value) {
                    finalAliases.add(aliasMeta.alias());
                }
            }
            return finalAliases.toArray(new String[finalAliases.size()]);
        } else {
            //for ADD and REMOVE_INDEX we just return the current aliases
            return action.aliases();
        }
    }
}
