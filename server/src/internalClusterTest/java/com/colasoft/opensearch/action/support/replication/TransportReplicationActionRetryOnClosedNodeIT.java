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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.action.support.replication;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.plugins.ActionPlugin;
import com.colasoft.opensearch.plugins.NetworkPlugin;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.PluginsService;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.InternalTestCluster;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.Transport;
import com.colasoft.opensearch.transport.TransportInterceptor;
import com.colasoft.opensearch.transport.TransportRequest;
import com.colasoft.opensearch.transport.TransportRequestOptions;
import com.colasoft.opensearch.transport.TransportResponse;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;

import org.hamcrest.Matchers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 0)
public class TransportReplicationActionRetryOnClosedNodeIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(TestPlugin.class, MockTransportService.TestPlugin.class);
    }

    public static class Request extends ReplicationRequest<Request> {
        public Request(ShardId shardId) {
            super(shardId);
        }

        public Request(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public String toString() {
            return "test-request";
        }
    }

    public static class Response extends ReplicationResponse {
        public Response() {}

        public Response(StreamInput in) throws IOException {
            super(in);
        }
    }

    public static class TestAction extends TransportReplicationAction<Request, Request, Response> {
        private static final String ACTION_NAME = "internal:test-replication-action";
        private static final ActionType<Response> TYPE = new ActionType<>(ACTION_NAME, Response::new);

        @Inject
        public TestAction(
            Settings settings,
            TransportService transportService,
            ClusterService clusterService,
            IndicesService indicesService,
            ThreadPool threadPool,
            ShardStateAction shardStateAction,
            ActionFilters actionFilters
        ) {
            super(
                settings,
                ACTION_NAME,
                transportService,
                clusterService,
                indicesService,
                threadPool,
                shardStateAction,
                actionFilters,
                Request::new,
                Request::new,
                ThreadPool.Names.GENERIC
            );
        }

        @Override
        protected Response newResponseInstance(StreamInput in) throws IOException {
            return new Response(in);
        }

        @Override
        protected void shardOperationOnPrimary(
            Request shardRequest,
            IndexShard primary,
            ActionListener<PrimaryResult<Request, Response>> listener
        ) {
            listener.onResponse(new PrimaryResult<>(shardRequest, new Response()));
        }

        @Override
        protected void shardOperationOnReplica(Request shardRequest, IndexShard replica, ActionListener<ReplicaResult> listener) {
            listener.onResponse(new ReplicaResult());
        }
    }

    public static class TestPlugin extends Plugin implements ActionPlugin, NetworkPlugin {
        private CountDownLatch actionRunningLatch = new CountDownLatch(1);
        private CountDownLatch actionWaitLatch = new CountDownLatch(1);
        private volatile String testActionName;

        public TestPlugin() {}

        @Override
        public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
            return Arrays.asList(new ActionHandler<>(TestAction.TYPE, TestAction.class));
        }

        @Override
        public List<TransportInterceptor> getTransportInterceptors(
            NamedWriteableRegistry namedWriteableRegistry,
            ThreadContext threadContext
        ) {
            return Arrays.asList(new TransportInterceptor() {
                @Override
                public AsyncSender interceptSender(AsyncSender sender) {
                    return new AsyncSender() {
                        @Override
                        public <T extends TransportResponse> void sendRequest(
                            Transport.Connection connection,
                            String action,
                            TransportRequest request,
                            TransportRequestOptions options,
                            TransportResponseHandler<T> handler
                        ) {
                            // only activated on primary
                            if (action.equals(testActionName)) {
                                actionRunningLatch.countDown();
                                try {
                                    actionWaitLatch.await(10, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    throw new AssertionError(e);
                                }
                            }
                            sender.sendRequest(connection, action, request, options, handler);
                        }
                    };
                }
            });
        }
    }

    public void testRetryOnStoppedTransportService() throws Exception {
        internalCluster().startClusterManagerOnlyNodes(2);
        String primary = internalCluster().startDataOnlyNode();
        assertAcked(
            prepareCreate("test").setSettings(
                Settings.builder()
                    .put(indexSettings())
                    .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1)
                    .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 1)
            )
        );

        String replica = internalCluster().startDataOnlyNode();
        String coordinator = internalCluster().startCoordinatingOnlyNode(Settings.EMPTY);
        ensureGreen("test");

        TestPlugin primaryTestPlugin = getTestPlugin(primary);
        // this test only provoked an issue for the primary action, but for completeness, we pick the action randomly
        primaryTestPlugin.testActionName = TestAction.ACTION_NAME + (randomBoolean() ? "[p]" : "[r]");
        logger.info("--> Test action {}, primary {}, replica {}", primaryTestPlugin.testActionName, primary, replica);

        AtomicReference<Object> response = new AtomicReference<>();
        CountDownLatch doneLatch = new CountDownLatch(1);
        client(coordinator).execute(
            TestAction.TYPE,
            new Request(new ShardId(resolveIndex("test"), 0)),
            ActionListener.runAfter(
                ActionListener.wrap(r -> assertTrue(response.compareAndSet(null, r)), e -> assertTrue(response.compareAndSet(null, e))),
                doneLatch::countDown
            )
        );

        assertTrue(primaryTestPlugin.actionRunningLatch.await(10, TimeUnit.SECONDS));

        MockTransportService primaryTransportService = (MockTransportService) internalCluster().getInstance(
            TransportService.class,
            primary
        );
        // we pause node after TransportService has moved to stopped, but before closing connections, since if connections are closed
        // we would not hit the transport service closed case.
        primaryTransportService.addOnStopListener(() -> {
            primaryTestPlugin.actionWaitLatch.countDown();
            try {
                assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        });
        internalCluster().stopRandomNode(InternalTestCluster.nameFilter(primary));

        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        if (response.get() instanceof Exception) {
            throw new AssertionError(response.get());
        }
    }

    private TestPlugin getTestPlugin(String node) {
        PluginsService pluginsService = internalCluster().getInstance(PluginsService.class, node);
        List<TestPlugin> testPlugins = pluginsService.filterPlugins(TestPlugin.class);
        assertThat(testPlugins, Matchers.hasSize(1));
        return testPlugins.get(0);
    }
}
