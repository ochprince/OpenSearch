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

package org.elasticsearch.test.client;

import org.opensearch.OpenSearchException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.TransportAction;
import org.opensearch.client.Client;
import org.opensearch.client.node.NodeClient;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskListener;
import org.opensearch.threadpool.TestThreadPool;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.RemoteClusterService;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Client that always response with {@code null} to every request. Override {@link #doExecute(ActionType, ActionRequest, ActionListener)},
 * {@link #executeLocally(ActionType, ActionRequest, ActionListener)}, or {@link #executeLocally(ActionType, ActionRequest, TaskListener)}
 * for testing.
 *
 * See also {@link NoOpClient} if you do not specifically need a {@link NodeClient}.
 */
public class NoOpNodeClient extends NodeClient {

    /**
     * Build with {@link ThreadPool}. This {@linkplain ThreadPool} is terminated on {@link #close()}.
     */
    public NoOpNodeClient(ThreadPool threadPool) {
        super(Settings.EMPTY, threadPool);
    }

    /**
     * Create a new {@link TestThreadPool} for this client. This {@linkplain TestThreadPool} is terminated on {@link #close()}.
     */
    public NoOpNodeClient(String testName) {
        super(Settings.EMPTY, new TestThreadPool(testName));
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse>
    void doExecute(ActionType<Response> action, Request request, ActionListener<Response> listener) {
        listener.onResponse(null);
    }

    @Override
    public void initialize(Map<ActionType, TransportAction> actions, Supplier<String> localNodeId,
                           RemoteClusterService remoteClusterService, NamedWriteableRegistry namedWriteableRegistry) {
        throw new UnsupportedOperationException("cannot initialize " + this.getClass().getSimpleName());
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse>
    Task executeLocally(ActionType<Response> action, Request request, ActionListener<Response> listener) {
        listener.onResponse(null);
        return null;
    }

    @Override
    public <Request extends ActionRequest, Response extends ActionResponse>
    Task executeLocally(ActionType<Response> action, Request request, TaskListener<Response> listener) {
        listener.onResponse(null, null);
        return null;
    }

    @Override
    public String getLocalNodeId() {
        return null;
    }

    @Override
    public Client getRemoteClusterClient(String clusterAlias) {
        return null;
    }

    @Override
    public void close() {
        try {
            ThreadPool.terminate(threadPool(), 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new OpenSearchException(e.getMessage(), e);
        }
    }
}
