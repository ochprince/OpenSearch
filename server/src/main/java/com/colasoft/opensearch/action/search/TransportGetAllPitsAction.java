/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The ColaSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.nodes.TransportNodesAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.search.SearchService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

/**
 * Transport action to get all active PIT contexts across all nodes
 */
public class TransportGetAllPitsAction extends TransportNodesAction<
    GetAllPitNodesRequest,
    GetAllPitNodesResponse,
    GetAllPitNodeRequest,
    GetAllPitNodeResponse> {
    private final SearchService searchService;

    @Inject
    public TransportGetAllPitsAction(
        ThreadPool threadPool,
        ClusterService clusterService,
        TransportService transportService,
        ActionFilters actionFilters,
        SearchService searchService
    ) {
        super(
            GetAllPitsAction.NAME,
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            GetAllPitNodesRequest::new,
            GetAllPitNodeRequest::new,
            ThreadPool.Names.SAME,
            GetAllPitNodeResponse.class
        );
        this.searchService = searchService;
    }

    @Override
    protected GetAllPitNodesResponse newResponse(
        GetAllPitNodesRequest request,
        List<GetAllPitNodeResponse> getAllPitNodeResponses,
        List<FailedNodeException> failures
    ) {
        return new GetAllPitNodesResponse(clusterService.getClusterName(), getAllPitNodeResponses, failures);
    }

    @Override
    protected GetAllPitNodeRequest newNodeRequest(GetAllPitNodesRequest request) {
        return new GetAllPitNodeRequest();
    }

    @Override
    protected GetAllPitNodeResponse newNodeResponse(StreamInput in) throws IOException {
        return new GetAllPitNodeResponse(in);
    }

    /**
     * This retrieves all active PITs in the node
     */
    @Override
    protected GetAllPitNodeResponse nodeOperation(GetAllPitNodeRequest request) {
        GetAllPitNodeResponse nodeResponse = new GetAllPitNodeResponse(
            transportService.getLocalNode(),
            searchService.getAllPITReaderContexts()
        );
        return nodeResponse;
    }
}
