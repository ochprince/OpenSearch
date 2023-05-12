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

package com.colasoft.opensearch.action.resync;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.PlainActionFuture;
import com.colasoft.opensearch.action.support.replication.PendingReplicationActions;
import com.colasoft.opensearch.action.support.replication.ReplicationMode;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.block.ClusterBlocks;
import com.colasoft.opensearch.cluster.coordination.NoClusterManagerBlockService;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.routing.IndexShardRoutingTable;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardRoutingState;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.lease.Releasable;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.IndexingPressureService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ReplicationGroup;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.index.translog.Translog;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.SystemIndices;
import com.colasoft.opensearch.indices.breaker.NoneCircuitBreakerService;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;
import com.colasoft.opensearch.transport.nio.MockNioTransport;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.colasoft.opensearch.action.support.replication.ClusterStateCreationUtils.state;
import static com.colasoft.opensearch.test.ClusterServiceUtils.createClusterService;
import static com.colasoft.opensearch.test.ClusterServiceUtils.setState;
import static com.colasoft.opensearch.transport.TransportService.NOOP_TRANSPORT_INTERCEPTOR;

public class TransportResyncReplicationActionTests extends OpenSearchTestCase {

    private static ThreadPool threadPool;

    @BeforeClass
    public static void beforeClass() {
        threadPool = new TestThreadPool("ShardReplicationTests");
    }

    @AfterClass
    public static void afterClass() {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        threadPool = null;
    }

    public void testResyncDoesNotBlockOnPrimaryAction() throws Exception {
        try (ClusterService clusterService = createClusterService(threadPool)) {
            final String indexName = randomAlphaOfLength(5);
            setState(clusterService, state(indexName, true, ShardRoutingState.STARTED));

            setState(
                clusterService,
                ClusterState.builder(clusterService.state())
                    .blocks(
                        ClusterBlocks.builder()
                            .addGlobalBlock(NoClusterManagerBlockService.NO_CLUSTER_MANAGER_BLOCK_ALL)
                            .addIndexBlock(indexName, IndexMetadata.INDEX_WRITE_BLOCK)
                    )
            );

            try (
                MockNioTransport transport = new MockNioTransport(
                    Settings.EMPTY,
                    Version.CURRENT,
                    threadPool,
                    new NetworkService(emptyList()),
                    PageCacheRecycler.NON_RECYCLING_INSTANCE,
                    new NamedWriteableRegistry(emptyList()),
                    new NoneCircuitBreakerService()
                )
            ) {

                final MockTransportService transportService = new MockTransportService(
                    Settings.EMPTY,
                    transport,
                    threadPool,
                    NOOP_TRANSPORT_INTERCEPTOR,
                    x -> clusterService.localNode(),
                    null,
                    Collections.emptySet()
                );
                transportService.start();
                transportService.acceptIncomingRequests();
                final ShardStateAction shardStateAction = new ShardStateAction(clusterService, transportService, null, null, threadPool);

                final IndexMetadata indexMetadata = clusterService.state().metadata().index(indexName);
                final Index index = indexMetadata.getIndex();
                final ShardId shardId = new ShardId(index, 0);
                final IndexShardRoutingTable shardRoutingTable = clusterService.state().routingTable().shardRoutingTable(shardId);
                final ShardRouting primaryShardRouting = clusterService.state().routingTable().shardRoutingTable(shardId).primaryShard();
                final String allocationId = primaryShardRouting.allocationId().getId();
                final long primaryTerm = indexMetadata.primaryTerm(shardId.id());

                final AtomicInteger acquiredPermits = new AtomicInteger();
                final IndexShard indexShard = mock(IndexShard.class);
                final PendingReplicationActions replicationActions = new PendingReplicationActions(shardId, threadPool);
                when(indexShard.indexSettings()).thenReturn(new IndexSettings(indexMetadata, Settings.EMPTY));
                when(indexShard.shardId()).thenReturn(shardId);
                when(indexShard.routingEntry()).thenReturn(primaryShardRouting);
                when(indexShard.getPendingPrimaryTerm()).thenReturn(primaryTerm);
                when(indexShard.getOperationPrimaryTerm()).thenReturn(primaryTerm);
                when(indexShard.getActiveOperationsCount()).then(i -> acquiredPermits.get());
                when(indexShard.getPendingReplicationActions()).thenReturn(replicationActions);
                doAnswer(invocation -> {
                    ActionListener<Releasable> callback = (ActionListener<Releasable>) invocation.getArguments()[0];
                    acquiredPermits.incrementAndGet();
                    callback.onResponse(acquiredPermits::decrementAndGet);
                    return null;
                }).when(indexShard).acquirePrimaryOperationPermit(any(ActionListener.class), anyString(), any(), eq(true));
                Set<String> trackedAllocationIds = shardRoutingTable.getAllAllocationIds();
                when(indexShard.getReplicationGroup()).thenReturn(
                    new ReplicationGroup(
                        shardRoutingTable,
                        clusterService.state().metadata().index(index).inSyncAllocationIds(shardId.id()),
                        trackedAllocationIds,
                        0
                    )
                );

                final IndexService indexService = mock(IndexService.class);
                when(indexService.getShard(eq(shardId.id()))).thenReturn(indexShard);

                final IndicesService indexServices = mock(IndicesService.class);
                when(indexServices.indexServiceSafe(eq(index))).thenReturn(indexService);

                final TransportResyncReplicationAction action = new TransportResyncReplicationAction(
                    Settings.EMPTY,
                    transportService,
                    clusterService,
                    indexServices,
                    threadPool,
                    shardStateAction,
                    new ActionFilters(new HashSet<>()),
                    new IndexingPressureService(Settings.EMPTY, clusterService),
                    new SystemIndices(emptyMap())
                );

                assertThat(action.globalBlockLevel(), nullValue());
                assertThat(action.indexBlockLevel(), nullValue());

                final Task task = mock(Task.class);
                when(task.getId()).thenReturn(randomNonNegativeLong());

                final byte[] bytes = "{}".getBytes(Charset.forName("UTF-8"));
                final ResyncReplicationRequest request = new ResyncReplicationRequest(
                    shardId,
                    42L,
                    100,
                    new Translog.Operation[] { new Translog.Index("id", 0, primaryTerm, 0L, bytes, null, -1) }
                );

                final PlainActionFuture<ResyncReplicationResponse> listener = new PlainActionFuture<>();
                action.sync(request, task, allocationId, primaryTerm, listener);

                assertThat(listener.get().getShardInfo().getFailed(), equalTo(0));
                assertThat(listener.isDone(), is(true));
            }
        }
    }

    public void testGetReplicationModeWithRemoteTranslog() {
        final TransportResyncReplicationAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(true);
        assertEquals(ReplicationMode.NO_REPLICATION, action.getReplicationMode(indexShard));
    }

    public void testGetReplicationModeWithLocalTranslog() {
        final TransportResyncReplicationAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(false);
        assertEquals(ReplicationMode.FULL_REPLICATION, action.getReplicationMode(indexShard));
    }

    private TransportResyncReplicationAction createAction() {
        ClusterService clusterService = mock(ClusterService.class);
        ClusterSettings clusterSettings = new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        when(clusterService.getClusterSettings()).thenReturn(clusterSettings);
        return new TransportResyncReplicationAction(
            Settings.EMPTY,
            mock(TransportService.class),
            clusterService,
            mock(IndicesService.class),
            threadPool,
            mock(ShardStateAction.class),
            new ActionFilters(new HashSet<>()),
            mock(IndexingPressureService.class),
            new SystemIndices(emptyMap())
        );
    }
}
