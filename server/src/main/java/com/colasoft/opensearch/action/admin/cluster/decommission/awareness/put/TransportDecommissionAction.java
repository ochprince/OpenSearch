/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put;

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
 * Transport action for registering decommission
 *
 * @opensearch.internal
 */
public class TransportDecommissionAction extends TransportClusterManagerNodeAction<DecommissionRequest, DecommissionResponse> {

    private static final Logger logger = LogManager.getLogger(TransportDecommissionAction.class);
    private final DecommissionService decommissionService;

    @Inject
    public TransportDecommissionAction(
        TransportService transportService,
        ClusterService clusterService,
        DecommissionService decommissionService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            DecommissionAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            DecommissionRequest::new,
            indexNameExpressionResolver
        );
        this.decommissionService = decommissionService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected DecommissionResponse read(StreamInput in) throws IOException {
        return new DecommissionResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(DecommissionRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected void clusterManagerOperation(DecommissionRequest request, ClusterState state, ActionListener<DecommissionResponse> listener)
        throws Exception {
        logger.info("starting awareness attribute [{}] decommissioning", request.getDecommissionAttribute().toString());
        decommissionService.startDecommissionAction(request, listener);
    }
}
