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
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.search.pipeline.SearchPipelineService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Perform the action of deleting a search pipeline
 *
 * @opensearch.internal
 */
public class DeleteSearchPipelineTransportAction extends TransportClusterManagerNodeAction<
    DeleteSearchPipelineRequest,
    AcknowledgedResponse> {
    private final SearchPipelineService searchPipelineService;

    @Inject
    public DeleteSearchPipelineTransportAction(
        ThreadPool threadPool,
        SearchPipelineService searchPipelineService,
        TransportService transportService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            DeleteSearchPipelineAction.NAME,
            transportService,
            searchPipelineService.getClusterService(),
            threadPool,
            actionFilters,
            DeleteSearchPipelineRequest::new,
            indexNameExpressionResolver
        );
        this.searchPipelineService = searchPipelineService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        DeleteSearchPipelineRequest request,
        ClusterState state,
        ActionListener<AcknowledgedResponse> listener
    ) throws Exception {
        searchPipelineService.deletePipeline(request, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(DeleteSearchPipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
