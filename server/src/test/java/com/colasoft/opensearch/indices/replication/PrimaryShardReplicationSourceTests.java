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

package com.colasoft.opensearch.indices.replication;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.CancellableThreads;
import com.colasoft.opensearch.core.internal.io.IOUtils;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.IndexShardTestCase;
import com.colasoft.opensearch.index.store.Store;
import com.colasoft.opensearch.index.store.StoreFileMetadata;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import com.colasoft.opensearch.test.ClusterServiceUtils;
import com.colasoft.opensearch.test.transport.CapturingTransport;
import com.colasoft.opensearch.transport.TransportService;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class PrimaryShardReplicationSourceTests extends IndexShardTestCase {

    private static final long PRIMARY_TERM = 1L;
    private static final long SEGMENTS_GEN = 2L;
    private static final long SEQ_NO = 3L;
    private static final long VERSION = 4L;
    private static final long REPLICATION_ID = 123L;

    private CapturingTransport transport;
    private ClusterService clusterService;
    private TransportService transportService;
    private PrimaryShardReplicationSource replicationSource;
    private IndexShard indexShard;
    private DiscoveryNode sourceNode;

    private RecoverySettings recoverySettings;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Settings settings = Settings.builder().put("node.name", SegmentReplicationTargetServiceTests.class.getSimpleName()).build();
        final ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        final RecoverySettings recoverySettings = new RecoverySettings(settings, clusterSettings);
        transport = new CapturingTransport();
        sourceNode = newDiscoveryNode("sourceNode");
        final DiscoveryNode localNode = newDiscoveryNode("localNode");
        clusterService = ClusterServiceUtils.createClusterService(threadPool, localNode);
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

        indexShard = newStartedShard(true);

        this.recoverySettings = recoverySettings;
        replicationSource = new PrimaryShardReplicationSource(
            localNode,
            indexShard.routingEntry().allocationId().toString(),
            transportService,
            recoverySettings,
            sourceNode
        );
    }

    @Override
    public void tearDown() throws Exception {
        IOUtils.close(transportService, clusterService, transport);
        closeShards(indexShard);
        super.tearDown();
    }

    public void testGetCheckpointMetadata() {
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );
        replicationSource.getCheckpointMetadata(REPLICATION_ID, checkpoint, mock(ActionListener.class));
        CapturingTransport.CapturedRequest[] requestList = transport.getCapturedRequestsAndClear();
        assertEquals(1, requestList.length);
        CapturingTransport.CapturedRequest capturedRequest = requestList[0];
        assertEquals(SegmentReplicationSourceService.Actions.GET_CHECKPOINT_INFO, capturedRequest.action);
        assertEquals(sourceNode, capturedRequest.node);
        assertTrue(capturedRequest.request instanceof CheckpointInfoRequest);
    }

    public void testGetSegmentFiles() {
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );
        StoreFileMetadata testMetadata = new StoreFileMetadata("testFile", 1L, "checksum", Version.LATEST);
        replicationSource.getSegmentFiles(
            REPLICATION_ID,
            checkpoint,
            Arrays.asList(testMetadata),
            mock(Store.class),
            mock(ActionListener.class)
        );
        CapturingTransport.CapturedRequest[] requestList = transport.getCapturedRequestsAndClear();
        assertEquals(1, requestList.length);
        CapturingTransport.CapturedRequest capturedRequest = requestList[0];
        assertEquals(SegmentReplicationSourceService.Actions.GET_SEGMENT_FILES, capturedRequest.action);
        assertEquals(sourceNode, capturedRequest.node);
        assertTrue(capturedRequest.request instanceof GetSegmentFilesRequest);
    }

    /**
     * This test verifies the transport request timeout value for fetching the segment files.
     */
    public void testTransportTimeoutForGetSegmentFilesAction() {
        long fileSize = (long) (Math.pow(10, 9));
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );
        StoreFileMetadata testMetadata = new StoreFileMetadata("testFile", fileSize, "checksum", Version.LATEST);
        replicationSource.getSegmentFiles(
            REPLICATION_ID,
            checkpoint,
            Arrays.asList(testMetadata),
            mock(Store.class),
            mock(ActionListener.class)
        );
        CapturingTransport.CapturedRequest[] requestList = transport.getCapturedRequestsAndClear();
        assertEquals(1, requestList.length);
        CapturingTransport.CapturedRequest capturedRequest = requestList[0];
        assertEquals(SegmentReplicationSourceService.Actions.GET_SEGMENT_FILES, capturedRequest.action);
        assertEquals(sourceNode, capturedRequest.node);
        assertEquals(recoverySettings.internalActionLongTimeout(), capturedRequest.options.timeout());
    }

    public void testGetSegmentFiles_CancelWhileRequestOpen() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final ReplicationCheckpoint checkpoint = new ReplicationCheckpoint(
            indexShard.shardId(),
            PRIMARY_TERM,
            SEGMENTS_GEN,
            VERSION,
            Codec.getDefault().getName()
        );
        StoreFileMetadata testMetadata = new StoreFileMetadata("testFile", 1L, "checksum", Version.LATEST);
        replicationSource.getSegmentFiles(
            REPLICATION_ID,
            checkpoint,
            Arrays.asList(testMetadata),
            mock(Store.class),
            new ActionListener<>() {
                @Override
                public void onResponse(GetSegmentFilesResponse getSegmentFilesResponse) {
                    Assert.fail("onFailure response expected.");
                }

                @Override
                public void onFailure(Exception e) {
                    assertEquals(e.getClass(), CancellableThreads.ExecutionCancelledException.class);
                    latch.countDown();
                }
            }
        );
        replicationSource.cancel();
        latch.await(2, TimeUnit.SECONDS);
        assertEquals("listener should have resolved in a failure", 0, latch.getCount());
    }

    private DiscoveryNode newDiscoveryNode(String nodeName) {
        return new DiscoveryNode(
            nodeName,
            randomAlphaOfLength(10),
            buildNewFakeTransportAddress(),
            Collections.emptyMap(),
            Collections.singleton(DiscoveryNodeRole.CLUSTER_MANAGER_ROLE),
            com.colasoft.opensearch.Version.CURRENT
        );
    }
}
