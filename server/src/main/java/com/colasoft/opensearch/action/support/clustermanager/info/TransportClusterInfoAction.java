/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.action.support.clustermanager.info;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeReadAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

/**
 * Perform cluster information action
 *
 * @opensearch.internal
 */
public abstract class TransportClusterInfoAction<Request extends ClusterInfoRequest<Request>, Response extends ActionResponse> extends
    TransportClusterManagerNodeReadAction<Request, Response> {

    public TransportClusterInfoAction(
        String actionName,
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        Writeable.Reader<Request> request,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(actionName, transportService, clusterService, threadPool, actionFilters, request, indexNameExpressionResolver);
    }

    @Override
    protected String executor() {
        // read operation, lightweight...
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterBlockException checkBlock(Request request, ClusterState state) {
        return state.blocks()
            .indicesBlockedException(ClusterBlockLevel.METADATA_READ, indexNameExpressionResolver.concreteIndexNames(state, request));
    }

    /** @deprecated As of 2.2, because supporting inclusive language, replaced by {@link #clusterManagerOperation(ClusterInfoRequest, ClusterState, ActionListener)} */
    @Deprecated
    protected final void masterOperation(final Request request, final ClusterState state, final ActionListener<Response> listener) {
        clusterManagerOperation(request, state, listener);
    }

    @Override
    protected final void clusterManagerOperation(final Request request, final ClusterState state, final ActionListener<Response> listener) {
        String[] concreteIndices = indexNameExpressionResolver.concreteIndexNames(state, request);
        doClusterManagerOperation(request, concreteIndices, state, listener);
    }

    // TODO: Add abstract keyword after removing the deprecated doMasterOperation()
    protected void doClusterManagerOperation(
        Request request,
        String[] concreteIndices,
        ClusterState state,
        ActionListener<Response> listener
    ) {
        doMasterOperation(request, concreteIndices, state, listener);
    }

    /**
     * @deprecated As of 2.2, because supporting inclusive language, replaced by {@link #doClusterManagerOperation(ClusterInfoRequest, String[], ClusterState, ActionListener)}
     */
    @Deprecated
    protected void doMasterOperation(Request request, String[] concreteIndices, ClusterState state, ActionListener<Response> listener) {
        throw new UnsupportedOperationException("Must be overridden");
    }

}
