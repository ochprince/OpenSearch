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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeReadAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttributeMetadata;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for getting decommission status
 *
 * @opensearch.internal
 */
public class TransportGetDecommissionStateAction extends TransportClusterManagerNodeReadAction<
    GetDecommissionStateRequest,
    GetDecommissionStateResponse> {

    @Inject
    public TransportGetDecommissionStateAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            GetDecommissionStateAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            GetDecommissionStateRequest::new,
            indexNameExpressionResolver
        );
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected GetDecommissionStateResponse read(StreamInput in) throws IOException {
        return new GetDecommissionStateResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        GetDecommissionStateRequest request,
        ClusterState state,
        ActionListener<GetDecommissionStateResponse> listener
    ) throws Exception {
        DecommissionAttributeMetadata decommissionAttributeMetadata = state.metadata().decommissionAttributeMetadata();
        if (decommissionAttributeMetadata != null
            && request.attributeName().equals(decommissionAttributeMetadata.decommissionAttribute().attributeName())) {
            listener.onResponse(
                new GetDecommissionStateResponse(
                    decommissionAttributeMetadata.decommissionAttribute().attributeValue(),
                    decommissionAttributeMetadata.status()
                )
            );
        } else {
            listener.onResponse(new GetDecommissionStateResponse());
        }
    }

    @Override
    protected ClusterBlockException checkBlock(GetDecommissionStateRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
