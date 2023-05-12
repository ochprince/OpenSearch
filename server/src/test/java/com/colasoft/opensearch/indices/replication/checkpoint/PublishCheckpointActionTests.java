/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.indices.replication.checkpoint;

import org.apache.lucene.codecs.Codec;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.ActionTestUtils;
import com.colasoft.opensearch.action.support.PlainActionFuture;
import com.colasoft.opensearch.action.support.replication.ReplicationMode;
import com.colasoft.opensearch.action.support.replication.TransportReplicationAction;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.core.internal.io.IOUtils;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.replication.SegmentReplicationTargetService;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.transport.CapturingTransport;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static com.colasoft.opensearch.test.ClusterServiceUtils.createClusterService;

public class PublishCheckpointActionTests extends OpenSearchTestCase {

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

    public void testPublishCheckpointActionOnPrimary() {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);

        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final SegmentReplicationTargetService mockTargetService = mock(SegmentReplicationTargetService.class);

        final PublishCheckpointAction action = new PublishCheckpointAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet()),
            mockTargetService
        );

        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(indexShard.shardId(), 1111, 11, 1, Codec.getDefault().getName());
        final PublishCheckpointRequest request = new PublishCheckpointRequest(checkpoint);

        action.shardOperationOnPrimary(request, indexShard, ActionTestUtils.assertNoFailureListener(result -> {
            // we should forward the request containing the current publish checkpoint to the replica
            assertThat(result.replicaRequest(), sameInstance(request));
        }));
    }

    public void testPublishCheckpointActionOnReplica() {
        final IndicesService indicesService = mock(IndicesService.class);

        final Index index = new Index("index", "uuid");
        final IndexService indexService = mock(IndexService.class);
        when(indicesService.indexServiceSafe(index)).thenReturn(indexService);
        final int id = randomIntBetween(0, 4);
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexService.getShard(id)).thenReturn(indexShard);

        final ShardId shardId = new ShardId(index, id);
        when(indexShard.shardId()).thenReturn(shardId);

        final SegmentReplicationTargetService mockTargetService = mock(SegmentReplicationTargetService.class);

        final PublishCheckpointAction action = new PublishCheckpointAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet()),
            mockTargetService
        );

        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(indexShard.shardId(), 1111, 11, 1, Codec.getDefault().getName());

        final PublishCheckpointRequest request = new PublishCheckpointRequest(checkpoint);

        final PlainActionFuture<TransportReplicationAction.ReplicaResult> listener = PlainActionFuture.newFuture();
        action.shardOperationOnReplica(request, indexShard, listener);
        final TransportReplicationAction.ReplicaResult result = listener.actionGet();

        // onNewCheckpoint should be called on shard with checkpoint request
        verify(mockTargetService, times(1)).onNewCheckpoint(checkpoint, indexShard);

        // the result should indicate success
        final AtomicBoolean success = new AtomicBoolean();
        result.runPostReplicaActions(ActionListener.wrap(r -> success.set(true), e -> fail(e.toString())));
        assertTrue(success.get());

    }

    public void testGetReplicationModeWithRemoteTranslog() {
        final PublishCheckpointAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(true);
        assertEquals(ReplicationMode.FULL_REPLICATION, action.getReplicationMode(indexShard));
    }

    public void testGetReplicationModeWithLocalTranslog() {
        final PublishCheckpointAction action = createAction();
        final IndexShard indexShard = mock(IndexShard.class);
        when(indexShard.isRemoteTranslogEnabled()).thenReturn(false);
        assertEquals(ReplicationMode.FULL_REPLICATION, action.getReplicationMode(indexShard));
    }

    private PublishCheckpointAction createAction() {
        return new PublishCheckpointAction(
            Settings.EMPTY,
            transportService,
            clusterService,
            mock(IndicesService.class),
            threadPool,
            shardStateAction,
            new ActionFilters(Collections.emptySet()),
            mock(SegmentReplicationTargetService.class)
        );
    }

}
