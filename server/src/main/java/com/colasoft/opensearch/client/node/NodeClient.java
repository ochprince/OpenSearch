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

package com.colasoft.opensearch.client.node;

import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionModule.DynamicActionRegistry;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.action.support.TransportAction;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.client.support.AbstractClient;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.tasks.TaskListener;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.RemoteClusterService;

import java.util.function.Supplier;

/**
 * Client that executes actions on the local node.
 *
 * @opensearch.internal
 */
public class NodeClient extends AbstractClient {

    private DynamicActionRegistry actionRegistry;
    /**
     * The id of the local {@link DiscoveryNode}. Useful for generating task ids from tasks returned by
     * {@link #executeLocally(ActionType, ActionRequest, TaskListener)}.
     */
    private Supplier<String> localNodeId;
    private RemoteClusterService remoteClusterService;
    private NamedWriteableRegistry namedWriteableRegistry;

    public NodeClient(Settings settings, ThreadPool threadPool) {
        super(settings, threadPool);
    }

    public void initialize(
        DynamicActionRegistry actionRegistry,
        Supplier<String> localNodeId,
        RemoteClusterService remoteClusterService,
        NamedWriteableRegistry namedWriteableRegistry
    ) {
        this.actionRegistry = actionRegistry;
        this.localNodeId = localNodeId;
        this.remoteClusterService = remoteClusterService;
        this.namedWriteableRegistry = namedWriteableRegistry;
    }

    @Override
    public void close() {
        // nothing really to do
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    ) {
        // Discard the task because the Client interface doesn't use it.
        executeLocally(action, request, listener);
    }

    /**
     * Execute an {@link ActionType} locally, returning that {@link Task} used to track it, and linking an {@link ActionListener}.
     * Prefer this method if you don't need access to the task when listening for the response. This is the method used to implement
     * the {@link Client} interface.
     */
    public <Request extends ActionRequest, Response extends ActionResponse> Task executeLocally(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    ) {
        return transportAction(action).execute(request, listener);
    }

    /**
     * Execute an {@link ActionType} locally, returning that {@link Task} used to track it, and linking an {@link TaskListener}. Prefer this
     * method if you need access to the task when listening for the response.
     */
    public <Request extends ActionRequest, Response extends ActionResponse> Task executeLocally(
        ActionType<Response> action,
        Request request,
        TaskListener<Response> listener
    ) {
        return transportAction(action).execute(request, listener);
    }

    /**
     * The id of the local {@link DiscoveryNode}. Useful for generating task ids from tasks returned by
     * {@link #executeLocally(ActionType, ActionRequest, TaskListener)}.
     */
    public String getLocalNodeId() {
        return localNodeId.get();
    }

    /**
     * Get the {@link TransportAction} for an {@link ActionType}, throwing exceptions if the action isn't available.
     */
    @SuppressWarnings("unchecked")
    private <Request extends ActionRequest, Response extends ActionResponse> TransportAction<Request, Response> transportAction(
        ActionType<Response> action
    ) {
        if (actionRegistry == null) {
            throw new IllegalStateException("NodeClient has not been initialized");
        }
        TransportAction<Request, Response> transportAction = (TransportAction<Request, Response>) actionRegistry.get(action);
        if (transportAction == null) {
            throw new IllegalStateException("failed to find action [" + action + "] to execute");
        }
        return transportAction;
    }

    @Override
    public Client getRemoteClusterClient(String clusterAlias) {
        return remoteClusterService.getRemoteClusterClient(threadPool(), clusterAlias);
    }

    public NamedWriteableRegistry getNamedWriteableRegistry() {
        return namedWriteableRegistry;
    }
}
