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

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRequestValidationException;
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
 * Transport action for updating weights for weighted round-robin search routing policy
 *
 * @opensearch.internal
 */
public class TransportAddWeightedRoutingAction extends TransportClusterManagerNodeAction<
    ClusterPutWeightedRoutingRequest,
    ClusterPutWeightedRoutingResponse> {

    private final WeightedRoutingService weightedRoutingService;

    @Inject
    public TransportAddWeightedRoutingAction(
        TransportService transportService,
        ClusterService clusterService,
        WeightedRoutingService weightedRoutingService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ClusterAddWeightedRoutingAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            ClusterPutWeightedRoutingRequest::new,
            indexNameExpressionResolver
        );
        this.weightedRoutingService = weightedRoutingService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterPutWeightedRoutingResponse read(StreamInput in) throws IOException {
        return new ClusterPutWeightedRoutingResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        ClusterPutWeightedRoutingRequest request,
        ClusterState state,
        ActionListener<ClusterPutWeightedRoutingResponse> listener
    ) throws Exception {
        try {
            weightedRoutingService.verifyAwarenessAttribute(request.getWeightedRouting().attributeName());
        } catch (ActionRequestValidationException ex) {
            listener.onFailure(ex);
            return;
        }
        weightedRoutingService.registerWeightedRoutingMetadata(
            request,
            ActionListener.delegateFailure(listener, (delegatedListener, response) -> {
                delegatedListener.onResponse(new ClusterPutWeightedRoutingResponse(response.isAcknowledged()));
            })
        );
    }

    @Override
    protected ClusterBlockException checkBlock(ClusterPutWeightedRoutingRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
