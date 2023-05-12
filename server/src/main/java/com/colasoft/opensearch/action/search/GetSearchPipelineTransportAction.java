/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeReadAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.search.pipeline.SearchPipelineService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Perform the action of getting a search pipeline
 *
 * @opensearch.internal
 */
public class GetSearchPipelineTransportAction extends TransportClusterManagerNodeReadAction<
    GetSearchPipelineRequest,
    GetSearchPipelineResponse> {

    @Inject
    public GetSearchPipelineTransportAction(
        ThreadPool threadPool,
        ClusterService clusterService,
        TransportService transportService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            GetSearchPipelineAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            GetSearchPipelineRequest::new,
            indexNameExpressionResolver
        );
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetSearchPipelineResponse read(StreamInput in) throws IOException {
        return new GetSearchPipelineResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        GetSearchPipelineRequest request,
        ClusterState state,
        ActionListener<GetSearchPipelineResponse> listener
    ) throws Exception {
        listener.onResponse(new GetSearchPipelineResponse(SearchPipelineService.getPipelines(state, request.getIds())));
    }

    @Override
    protected ClusterBlockException checkBlock(GetSearchPipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
