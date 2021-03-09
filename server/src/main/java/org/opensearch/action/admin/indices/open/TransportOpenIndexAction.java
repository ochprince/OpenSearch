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

package org.opensearch.action.admin.indices.open;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DestructiveOperations;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ack.OpenIndexClusterStateUpdateResponse;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.metadata.MetadataIndexStateService;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.index.Index;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;

/**
 * Open index action
 */
public class TransportOpenIndexAction extends TransportMasterNodeAction<OpenIndexRequest, OpenIndexResponse> {

    private static final Logger logger = LogManager.getLogger(TransportOpenIndexAction.class);

    private final MetadataIndexStateService indexStateService;
    private final DestructiveOperations destructiveOperations;

    @Inject
    public TransportOpenIndexAction(TransportService transportService, ClusterService clusterService,
                                    ThreadPool threadPool, MetadataIndexStateService indexStateService,
                                    ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver,
                                    DestructiveOperations destructiveOperations) {
        super(OpenIndexAction.NAME, transportService, clusterService, threadPool, actionFilters, OpenIndexRequest::new,
            indexNameExpressionResolver);
        this.indexStateService = indexStateService;
        this.destructiveOperations = destructiveOperations;
    }

    @Override
    protected String executor() {
        // we go async right away...
        return ThreadPool.Names.SAME;
    }

    @Override
    protected OpenIndexResponse read(StreamInput in) throws IOException {
        return new OpenIndexResponse(in);
    }

    @Override
    protected void doExecute(Task task, OpenIndexRequest request, ActionListener<OpenIndexResponse> listener) {
        destructiveOperations.failDestructive(request.indices());
        super.doExecute(task, request, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(OpenIndexRequest request, ClusterState state) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE,
            indexNameExpressionResolver.concreteIndexNames(state, request));
    }

    @Override
    protected void masterOperation(final OpenIndexRequest request, final ClusterState state,
                                   final ActionListener<OpenIndexResponse> listener) {
        final Index[] concreteIndices = indexNameExpressionResolver.concreteIndices(state, request);
        if (concreteIndices == null || concreteIndices.length == 0) {
            listener.onResponse(new OpenIndexResponse(true, true));
            return;
        }
        OpenIndexClusterStateUpdateRequest updateRequest = new OpenIndexClusterStateUpdateRequest()
                .ackTimeout(request.timeout()).masterNodeTimeout(request.masterNodeTimeout())
                .indices(concreteIndices).waitForActiveShards(request.waitForActiveShards());

        indexStateService.openIndex(updateRequest, new ActionListener<OpenIndexClusterStateUpdateResponse>() {

            @Override
            public void onResponse(OpenIndexClusterStateUpdateResponse response) {
                listener.onResponse(new OpenIndexResponse(response.isAcknowledged(), response.isShardsAcknowledged()));
            }

            @Override
            public void onFailure(Exception t) {
                logger.debug(() -> new ParameterizedMessage("failed to open indices [{}]", (Object) concreteIndices), t);
                listener.onFailure(t);
            }
        });
    }
}
