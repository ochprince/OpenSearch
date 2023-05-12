/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.routing.WeightedRoutingService;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for deleting weights for weighted round-robin search routing policy
 *
 * @opensearch.internal
 */
public class TransportDeleteWeightedRoutingAction extends TransportClusterManagerNodeAction<
    ClusterDeleteWeightedRoutingRequest,
    ClusterDeleteWeightedRoutingResponse> {

    private static final Logger logger = LogManager.getLogger(TransportDeleteWeightedRoutingAction.class);

    private final WeightedRoutingService weightedRoutingService;

    @Inject
    public TransportDeleteWeightedRoutingAction(
        TransportService transportService,
        ClusterService clusterService,
        WeightedRoutingService weightedRoutingService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ClusterDeleteWeightedRoutingAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            ClusterDeleteWeightedRoutingRequest::new,
            indexNameExpressionResolver
        );
        this.weightedRoutingService = weightedRoutingService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterDeleteWeightedRoutingResponse read(StreamInput in) throws IOException {
        return new ClusterDeleteWeightedRoutingResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(ClusterDeleteWeightedRoutingRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected void clusterManagerOperation(
        ClusterDeleteWeightedRoutingRequest request,
        ClusterState state,
        ActionListener<ClusterDeleteWeightedRoutingResponse> listener
    ) throws Exception {
        weightedRoutingService.deleteWeightedRoutingMetadata(request, listener);
    }
}
