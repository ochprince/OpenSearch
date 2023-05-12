/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

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
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.action.admin.indices.create;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.ActiveShardCount;
import com.colasoft.opensearch.action.support.ActiveShardsObserver;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.AckedClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ack.ClusterStateUpdateResponse;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.metadata.ComposableIndexTemplate;
import com.colasoft.opensearch.cluster.metadata.ComposableIndexTemplate.DataStreamTemplate;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.metadata.MetadataCreateDataStreamService;
import com.colasoft.opensearch.cluster.metadata.MetadataCreateDataStreamService.CreateDataStreamClusterStateUpdateRequest;
import com.colasoft.opensearch.cluster.metadata.MetadataCreateIndexService;
import com.colasoft.opensearch.cluster.metadata.MetadataIndexTemplateService;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskKeys;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskThrottler;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Priority;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Api that auto creates an index or data stream that originate from requests that write into an index that doesn't yet exist.
 *
 * @opensearch.internal
 */
public final class AutoCreateAction extends ActionType<CreateIndexResponse> {

    public static final AutoCreateAction INSTANCE = new AutoCreateAction();
    public static final String NAME = "indices:admin/auto_create";

    private AutoCreateAction() {
        super(NAME, CreateIndexResponse::new);
    }

    /**
     * Transport Action for Auto Create
     *
     * @opensearch.internal
     */
    public static final class TransportAction extends TransportClusterManagerNodeAction<CreateIndexRequest, CreateIndexResponse> {

        private final ActiveShardsObserver activeShardsObserver;
        private final MetadataCreateIndexService createIndexService;
        private final MetadataCreateDataStreamService metadataCreateDataStreamService;
        private final ClusterManagerTaskThrottler.ThrottlingKey autoCreateTaskKey;

        @Inject
        public TransportAction(
            TransportService transportService,
            ClusterService clusterService,
            ThreadPool threadPool,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            MetadataCreateIndexService createIndexService,
            MetadataCreateDataStreamService metadataCreateDataStreamService
        ) {
            super(NAME, transportService, clusterService, threadPool, actionFilters, CreateIndexRequest::new, indexNameExpressionResolver);
            this.activeShardsObserver = new ActiveShardsObserver(clusterService, threadPool);
            this.createIndexService = createIndexService;
            this.metadataCreateDataStreamService = metadataCreateDataStreamService;

            // Task is onboarded for throttling, it will get retried from associated TransportClusterManagerNodeAction.
            autoCreateTaskKey = clusterService.registerClusterManagerTask(ClusterManagerTaskKeys.AUTO_CREATE_KEY, true);
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        protected CreateIndexResponse read(StreamInput in) throws IOException {
            return new CreateIndexResponse(in);
        }

        @Override
        protected void clusterManagerOperation(
            CreateIndexRequest request,
            ClusterState state,
            ActionListener<CreateIndexResponse> finalListener
        ) {
            AtomicReference<String> indexNameRef = new AtomicReference<>();
            ActionListener<ClusterStateUpdateResponse> listener = ActionListener.wrap(response -> {
                String indexName = indexNameRef.get();
                assert indexName != null;
                if (response.isAcknowledged()) {
                    activeShardsObserver.waitForActiveShards(
                        new String[] { indexName },
                        ActiveShardCount.DEFAULT,
                        request.timeout(),
                        shardsAcked -> {
                            finalListener.onResponse(new CreateIndexResponse(true, shardsAcked, indexName));
                        },
                        finalListener::onFailure
                    );
                } else {
                    finalListener.onResponse(new CreateIndexResponse(false, false, indexName));
                }
            }, finalListener::onFailure);
            clusterService.submitStateUpdateTask(
                "auto create [" + request.index() + "]",
                new AckedClusterStateUpdateTask<ClusterStateUpdateResponse>(Priority.URGENT, request, listener) {

                    @Override
                    protected ClusterStateUpdateResponse newResponse(boolean acknowledged) {
                        return new ClusterStateUpdateResponse(acknowledged);
                    }

                    @Override
                    public ClusterManagerTaskThrottler.ThrottlingKey getClusterManagerThrottlingKey() {
                        return autoCreateTaskKey;
                    }

                    @Override
                    public ClusterState execute(ClusterState currentState) throws Exception {
                        DataStreamTemplate dataStreamTemplate = resolveAutoCreateDataStream(request, currentState.metadata());
                        if (dataStreamTemplate != null) {
                            CreateDataStreamClusterStateUpdateRequest createRequest = new CreateDataStreamClusterStateUpdateRequest(
                                request.index(),
                                request.clusterManagerNodeTimeout(),
                                request.timeout()
                            );
                            ClusterState clusterState = metadataCreateDataStreamService.createDataStream(createRequest, currentState);
                            indexNameRef.set(clusterState.metadata().dataStreams().get(request.index()).getIndices().get(0).getName());
                            return clusterState;
                        } else {
                            String indexName = indexNameExpressionResolver.resolveDateMathExpression(request.index());
                            indexNameRef.set(indexName);
                            CreateIndexClusterStateUpdateRequest updateRequest = new CreateIndexClusterStateUpdateRequest(
                                request.cause(),
                                indexName,
                                request.index()
                            ).ackTimeout(request.timeout()).masterNodeTimeout(request.clusterManagerNodeTimeout());
                            return createIndexService.applyCreateIndexRequest(currentState, updateRequest, false);
                        }
                    }
                }
            );
        }

        @Override
        protected ClusterBlockException checkBlock(CreateIndexRequest request, ClusterState state) {
            return state.blocks().indexBlockedException(ClusterBlockLevel.METADATA_WRITE, request.index());
        }
    }

    static DataStreamTemplate resolveAutoCreateDataStream(CreateIndexRequest request, Metadata metadata) {
        String v2Template = MetadataIndexTemplateService.findV2Template(metadata, request.index(), false);
        if (v2Template != null) {
            ComposableIndexTemplate composableIndexTemplate = metadata.templatesV2().get(v2Template);
            if (composableIndexTemplate.getDataStreamTemplate() != null) {
                return composableIndexTemplate.getDataStreamTemplate();
            }
        }

        return null;
    }

}
