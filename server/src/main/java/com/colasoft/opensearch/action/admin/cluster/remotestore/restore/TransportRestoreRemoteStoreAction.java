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

package com.colasoft.opensearch.action.admin.cluster.remotestore.restore;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreClusterStateListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.snapshots.RestoreService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for restore remote store operation
 *
 * @opensearch.internal
 */
public final class TransportRestoreRemoteStoreAction extends TransportClusterManagerNodeAction<
    RestoreRemoteStoreRequest,
    RestoreRemoteStoreResponse> {
    private final RestoreService restoreService;

    @Inject
    public TransportRestoreRemoteStoreAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        RestoreService restoreService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            RestoreRemoteStoreAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            RestoreRemoteStoreRequest::new,
            indexNameExpressionResolver
        );
        this.restoreService = restoreService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected RestoreRemoteStoreResponse read(StreamInput in) throws IOException {
        return new RestoreRemoteStoreResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(RestoreRemoteStoreRequest request, ClusterState state) {
        // Restoring a remote store might change the global state and create/change an index,
        // so we need to check for METADATA_WRITE and WRITE blocks
        ClusterBlockException blockException = state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        if (blockException != null) {
            return blockException;
        }
        return state.blocks().globalBlockedException(ClusterBlockLevel.WRITE);

    }

    @Override
    protected void clusterManagerOperation(
        final RestoreRemoteStoreRequest request,
        final ClusterState state,
        final ActionListener<RestoreRemoteStoreResponse> listener
    ) {
        restoreService.restoreFromRemoteStore(
            request,
            ActionListener.delegateFailure(listener, (delegatedListener, restoreCompletionResponse) -> {
                if (restoreCompletionResponse.getRestoreInfo() == null && request.waitForCompletion()) {
                    RestoreClusterStateListener.createAndRegisterListener(
                        clusterService,
                        restoreCompletionResponse,
                        delegatedListener,
                        RestoreRemoteStoreResponse::new
                    );
                } else {
                    delegatedListener.onResponse(new RestoreRemoteStoreResponse(restoreCompletionResponse.getRestoreInfo()));
                }
            })
        );
    }
}
