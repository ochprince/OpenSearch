/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.indices.replication;

import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;
import com.colasoft.opensearch.indices.replication.common.SegmentReplicationTransportRequest;

import java.io.IOException;

/**
 * Request object for fetching segment metadata for a {@link ReplicationCheckpoint} from
 * a {@link SegmentReplicationSource}. This object is created by the target node and sent
 * to the source node.
 *
 * @opensearch.internal
 */
public class CheckpointInfoRequest extends SegmentReplicationTransportRequest {

    private final ReplicationCheckpoint checkpoint;

    public CheckpointInfoRequest(StreamInput in) throws IOException {
        super(in);
        checkpoint = new ReplicationCheckpoint(in);
    }

    public CheckpointInfoRequest(
        long replicationId,
        String targetAllocationId,
        DiscoveryNode targetNode,
        ReplicationCheckpoint checkpoint
    ) {
        super(replicationId, targetAllocationId, targetNode);
        this.checkpoint = checkpoint;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        checkpoint.writeTo(out);
    }

    public ReplicationCheckpoint getCheckpoint() {
        return checkpoint;
    }
}
