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

package com.colasoft.opensearch.index.seqno;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.LatchedActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.ActionTestUtils;
import com.colasoft.opensearch.action.support.PlainActionFuture;
import com.colasoft.opensearch.action.support.replication.ReplicationMode;
import com.colasoft.opensearch.action.support.replication.TransportReplicationAction;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.core.internal.io.IOUtils;
import com.colasoft.opensearch.gateway.WriteStateException;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.transport.CapturingTransport;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.colasoft.opensearch.test.ClusterServiceUtils.createClusterService;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetentionLeaseBackgroundSyncActionTests extends OpenSearchTestCase {

    private ThreadPool threadPool;
    private CapturingTransport transport;
    private ClusterService clusterService;
    private TransportService transportService;
    private ShardStateAction shardStateAction;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool(getClass().getName());
        transport = new CapturingTransport();
        clusterService = createClusterService(threadPool);
        transportService = transport.createTransportService(
            clusterService.getSettings(),
            threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> clusterService.localNode(),
            null,
            Collections.emptySet()
        );
        transportService.start();
        transportService.acceptIncomingRequests();
        shardStateAction = new ShardStateAction(clusterService, transportService, null, null, threadPool);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            IOUtils.close(transportService, clusterService, transport);
        } finally {
            terminate(threadPool);
        }
        super.tearDown();
    }

    public void testRetentionLeaseBackgroundSyncActionOnPrimary() throws InterruptedException {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet())
        );
        final RetentionLeases retentionLeases = mock(RetentionLeases.class);
        final RetentionLeaseBackgroundSyncAction.Request request = new RetentionLeaseBackgroundSyncAction.Request(
            indexShard.shardId(),
            retentionLeases
        );

        final CountDownLatch latch = new CountDownLatch(1);
        action.shardOperationOnPrimary(request, indexShard, new LatchedActionListener<>(ActionTestUtils.assertNoFailureListener(result -> {
            // the retention leases on the shard should be persisted
            verify(indexShard).persistRetentionLeases();
            // we should forward the request containing the current retention leases to the replica
            assertThat(result.replicaRequest(), sameInstance(request));
        }), latch));
        latch.await();
    }

    public void testRetentionLeaseBackgroundSyncActionOnReplica() throws WriteStateException {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet())
        );
        final RetentionLeases retentionLeases = mock(RetentionLeases.class);
        final RetentionLeaseBackgroundSyncAction.Request request = new RetentionLeaseBackgroundSyncAction.Request(
            indexShard.shardId(),
            retentionLeases
        );

        final PlainActionFuture<TransportReplicationAction.ReplicaResult> listener = PlainActionFuture.newFuture();
        action.shardOperationOnReplica(request, indexShard, listener);
        final TransportReplicationAction.ReplicaResult result = listener.actionGet();
        // the retention leases on the shard should be updated
        verify(indexShard).updateRetentionLeasesOnReplica(retentionLeases);
        // the retention leases on the shard should be persisted
        verify(indexShard).persistRetentionLeases();
        // the result should indicate success
        final AtomicBoolean success = new AtomicBoolean();
        result.runPostReplicaActions(ActionListener.wrap(r -> success.set(true), e -> fail(e.toString())));
        assertTrue(success.get());
    }

    public void testBlocks() {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final RetentionLeaseBackgroundSyncAction action = new RetentionLeaseBackgroundSyncAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet())
        );

        assertNull(action.indexBlockLevel());
    }

    public void testGetReplicationModeWithRemoteTranslog() {
        final RetentionLeaseBackgroundSyncAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(true);
        assertEquals(ReplicationMode.NO_REPLICATION, action.getReplicationMode(indexShard));
    }

    public void testGetReplicationModeWithLocalTranslog() {
        final RetentionLeaseBackgroundSyncAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(false);
        assertEquals(ReplicationMode.FULL_REPLICATION, action.getReplicationMode(indexShard));
    }

    private RetentionLeaseBackgroundSyncAction createAction() {
        return new RetentionLeaseBackgroundSyncAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            mock(IndicesService.class),
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet())
        );
    }

}
