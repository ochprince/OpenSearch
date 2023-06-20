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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.decommission.DecommissionService;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for delete decommission.
 *
 * @opensearch.internal
 */
public class TransportDeleteDecommissionStateAction extends TransportClusterManagerNodeAction<
    DeleteDecommissionStateRequest,
    DeleteDecommissionStateResponse> {

    private static final Logger logger = LogManager.getLogger(TransportDeleteDecommissionStateAction.class);
    private final DecommissionService decommissionService;

    @Inject
    public TransportDeleteDecommissionStateAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        DecommissionService decommissionService
    ) {
        super(
            DeleteDecommissionStateAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            DeleteDecommissionStateRequest::new,
            indexNameExpressionResolver
        );
        this.decommissionService = decommissionService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected DeleteDecommissionStateResponse read(StreamInput in) throws IOException {
        return new DeleteDecommissionStateResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(DeleteDecommissionStateRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected void clusterManagerOperation(
        DeleteDecommissionStateRequest request,
        ClusterState state,
        ActionListener<DeleteDecommissionStateResponse> listener
    ) {
        logger.info("Received delete decommission Request [{}]", request);
        this.decommissionService.startRecommissionAction(listener);
    }
}
