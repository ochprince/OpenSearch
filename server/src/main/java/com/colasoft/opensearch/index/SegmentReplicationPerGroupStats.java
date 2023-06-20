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

package com.colasoft.opensearch.index;

import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.ToXContentFragment;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.index.shard.ShardId;

import java.io.IOException;
import java.util.Set;

/**
 * Return Segment Replication stats for a Replication Group.
 *
 * @opensearch.internal
 */
public class SegmentReplicationPerGroupStats implements Writeable, ToXContentFragment {

    private final ShardId shardId;
    private final Set<SegmentReplicationShardStats> replicaStats;
    private final long rejectedRequestCount;

    public SegmentReplicationPerGroupStats(ShardId shardId, Set<SegmentReplicationShardStats> replicaStats, long rejectedRequestCount) {
        this.shardId = shardId;
        this.replicaStats = replicaStats;
        this.rejectedRequestCount = rejectedRequestCount;
    }

    public SegmentReplicationPerGroupStats(StreamInput in) throws IOException {
        this.shardId = new ShardId(in);
        this.replicaStats = in.readSet(SegmentReplicationShardStats::new);
        this.rejectedRequestCount = in.readVLong();
    }

    public Set<SegmentReplicationShardStats> getReplicaStats() {
        return replicaStats;
    }

    public long getRejectedRequestCount() {
        return rejectedRequestCount;
    }

    public ShardId getShardId() {
        return shardId;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.field("rejected_requests", rejectedRequestCount);
        builder.startArray("replicas");
        for (SegmentReplicationShardStats stats : replicaStats) {
            stats.toXContent(builder, params);
        }
        builder.endArray();
        return builder;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        shardId.writeTo(out);
        out.writeCollection(replicaStats);
        out.writeVLong(rejectedRequestCount);
    }

    @Override
    public String toString() {
        return "SegmentReplicationPerGroupStats{" + "replicaStats=" + replicaStats + ", rejectedRequestCount=" + rejectedRequestCount + '}';
    }
}
