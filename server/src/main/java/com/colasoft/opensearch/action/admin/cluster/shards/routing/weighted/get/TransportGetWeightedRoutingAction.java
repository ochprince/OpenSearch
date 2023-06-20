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

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get;

import com.colasoft.opensearch.action.ActionListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeReadAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;

import com.colasoft.opensearch.cluster.metadata.WeightedRoutingMetadata;
import com.colasoft.opensearch.cluster.routing.WeightedRouting;
import com.colasoft.opensearch.cluster.routing.WeightedRoutingService;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;

import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for getting weights for weighted round-robin search routing policy
 *
 * @opensearch.internal
 */
public class TransportGetWeightedRoutingAction extends TransportClusterManagerNodeReadAction<
    ClusterGetWeightedRoutingRequest,
    ClusterGetWeightedRoutingResponse> {
    private static final Logger logger = LogManager.getLogger(TransportGetWeightedRoutingAction.class);
    private final WeightedRoutingService weightedRoutingService;

    @Inject
    public TransportGetWeightedRoutingAction(
        TransportService transportService,
        ClusterService clusterService,
        WeightedRoutingService weightedRoutingService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ClusterGetWeightedRoutingAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            ClusterGetWeightedRoutingRequest::new,
            indexNameExpressionResolver
        );
        this.weightedRoutingService = weightedRoutingService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterGetWeightedRoutingResponse read(StreamInput in) throws IOException {
        return new ClusterGetWeightedRoutingResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(ClusterGetWeightedRoutingRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected void clusterManagerOperation(
        final ClusterGetWeightedRoutingRequest request,
        ClusterState state,
        final ActionListener<ClusterGetWeightedRoutingResponse> listener
    ) {
        try {
            weightedRoutingService.verifyAwarenessAttribute(request.getAwarenessAttribute());
            WeightedRoutingMetadata weightedRoutingMetadata = state.metadata().custom(WeightedRoutingMetadata.TYPE);
            ClusterGetWeightedRoutingResponse clusterGetWeightedRoutingResponse = new ClusterGetWeightedRoutingResponse();
            if (weightedRoutingMetadata != null && weightedRoutingMetadata.getWeightedRouting() != null) {
                WeightedRouting weightedRouting = weightedRoutingMetadata.getWeightedRouting();
                clusterGetWeightedRoutingResponse = new ClusterGetWeightedRoutingResponse(
                    weightedRouting,
                    state.nodes().getClusterManagerNodeId() != null,
                    weightedRoutingMetadata.getVersion()
                );
            }
            listener.onResponse(clusterGetWeightedRoutingResponse);
        } catch (Exception ex) {
            listener.onFailure(ex);
        }
    }
}
