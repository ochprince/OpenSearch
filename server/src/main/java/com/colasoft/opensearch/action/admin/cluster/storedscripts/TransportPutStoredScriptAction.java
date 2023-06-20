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

package com.colasoft.opensearch.action.admin.cluster.storedscripts;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskKeys;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskThrottler;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for putting stored script
 *
 * @opensearch.internal
 */
public class TransportPutStoredScriptAction extends TransportClusterManagerNodeAction<PutStoredScriptRequest, AcknowledgedResponse> {

    private final ScriptService scriptService;
    private final ClusterManagerTaskThrottler.ThrottlingKey putScriptTaskKey;

    @Inject
    public TransportPutStoredScriptAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        ScriptService scriptService
    ) {
        super(
            PutStoredScriptAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            PutStoredScriptRequest::new,
            indexNameExpressionResolver
        );
        this.scriptService = scriptService;
        // Task is onboarded for throttling, it will get retried from associated TransportClusterManagerNodeAction.
        putScriptTaskKey = clusterService.registerClusterManagerTask(ClusterManagerTaskKeys.PUT_SCRIPT_KEY, true);
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
        PutStoredScriptRequest request,
        ClusterState state,
        ActionListener<AcknowledgedResponse> listener
    ) throws Exception {
        scriptService.putStoredScript(clusterService, request, putScriptTaskKey, listener);
    }

    @Override
    protected ClusterBlockException checkBlock(PutStoredScriptRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

}
