/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.indices.replication;

import org.apache.lucene.codecs.Codec;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import com.colasoft.opensearch.indices.replication.common.CopyStateTests;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.transport.CapturingTransport;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SegmentReplicationSourceServiceTests extends OpenSearchTestCase {

    private ReplicationCheckpoint testCheckpoint;
    private TestThreadPool testThreadPool;
    private TransportService transportService;
    private DiscoveryNode localNode;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // setup mocks
        IndexShard mockIndexShard = CopyStateTests.createMockIndexShard();
        ShardId testShardId = mockIndexShard.shardId();
        IndicesService mockIndicesService = mock(IndicesService.class);
        IndexService mockIndexService = mock(IndexService.class);
        when(mockIndicesService.indexServiceSafe(testShardId.getIndex())).thenReturn(mockIndexService);
        when(mockIndexService.getShard(testShardId.id())).thenReturn(mockIndexShard);

        // This mirrors the creation of the ReplicationCheckpoint inside CopyState
        testCheckpoint = new ReplicationCheckpoint(
            testShardId,
            mockIndexShard.getOperationPrimaryTerm(),
            0L,
            0L,
            Codec.getDefault().getName()
        );
        testThreadPool = new TestThreadPool("test", Settings.EMPTY);
        CapturingTransport transport = new CapturingTransport();
        localNode = new DiscoveryNode("local", buildNewFakeTransportAddress(), Version.CURRENT);
        transportService = transport.createTransportService(
            Settings.EMPTY,
            testThreadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> localNode,
            null,
            Collections.emptySet()
        );
        transportService.start();
        transportService.acceptIncomingRequests();

        final Settings settings = Settings.builder().put("node.name", SegmentReplicationTargetServiceTests.class.getSimpleName()).build();
        final ClusterSettings clusterSettings = new ClusterSettings(settings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        final RecoverySettings recoverySettings = new RecoverySettings(settings, clusterSettings);

        SegmentReplicationSourceService segmentReplicationSourceService = new SegmentReplicationSourceService(
            mockIndicesService,
            transportService,
            recoverySettings
        );
    }

    @Override
    public void tearDown() throws Exception {
        ThreadPool.terminate(testThreadPool, 30, TimeUnit.SECONDS);
        testThreadPool = null;
        super.tearDown();
    }

    public void testGetSegmentFiles() {
        final GetSegmentFilesRequest request = new GetSegmentFilesRequest(
            1,
            "allocationId",
            localNode,
            Collections.emptyList(),
            testCheckpoint
        );
        executeGetSegmentFiles(request, new ActionListener<>() {
            @Override
            public void onResponse(GetSegmentFilesResponse response) {
                assertEquals(0, response.files.size());
            }

            @Override
            public void onFailure(Exception e) {
                fail("unexpected exception: " + e);
            }
        });
    }

    public void testCheckpointInfo() {
        executeGetCheckpointInfo(new ActionListener<>() {
            @Override
            public void onResponse(CheckpointInfoResponse response) {
                assertEquals(testCheckpoint, response.getCheckpoint());
                assertNotNull(response.getInfosBytes());
                assertEquals(1, response.getMetadataMap().size());
            }

            @Override
            public void onFailure(Exception e) {
                fail("unexpected exception: " + e);
            }
        });
    }

    private void executeGetCheckpointInfo(ActionListener<CheckpointInfoResponse> listener) {
        final CheckpointInfoRequest request = new CheckpointInfoRequest(1L, "testAllocationId", localNode, testCheckpoint);
        transportService.sendRequest(
            localNode,
            SegmentReplicationSourceService.Actions.GET_CHECKPOINT_INFO,
            request,
            new TransportResponseHandler<CheckpointInfoResponse>() {
                @Override
                public void handleResponse(CheckpointInfoResponse response) {
                    listener.onResponse(response);
                }

                @Override
                public void handleException(TransportException e) {
                    listener.onFailure(e);
                }

                @Override
                public String executor() {
                    return ThreadPool.Names.SAME;
                }

                @Override
                public CheckpointInfoResponse read(StreamInput in) throws IOException {
                    return new CheckpointInfoResponse(in);
                }
            }
        );
    }

    private void executeGetSegmentFiles(GetSegmentFilesRequest request, ActionListener<GetSegmentFilesResponse> listener) {
        transportService.sendRequest(
            localNode,
            SegmentReplicationSourceService.Actions.GET_SEGMENT_FILES,
            request,
            new TransportResponseHandler<GetSegmentFilesResponse>() {
                @Override
                public void handleResponse(GetSegmentFilesResponse response) {
                    listener.onResponse(response);
                }

                @Override
                public void handleException(TransportException e) {
                    listener.onFailure(e);
                }

                @Override
                public String executor() {
                    return ThreadPool.Names.SAME;
                }

                @Override
                public GetSegmentFilesResponse read(StreamInput in) throws IOException {
                    return new GetSegmentFilesResponse(in);
                }
            }
        );
    }
}
