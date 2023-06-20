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

package com.colasoft.opensearch.action.admin.indices.replication;

import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.index.SegmentReplicationPerGroupStats;
import com.colasoft.opensearch.indices.replication.SegmentReplicationState;

import java.io.IOException;

/**
 * Segment Replication specific response object for fetching stats from either a primary
 * or replica shard. The stats returned are different depending on primary or replica.
 *
 * @opensearch.internal
 */
public class SegmentReplicationShardStatsResponse implements Writeable {

    @Nullable
    private final SegmentReplicationPerGroupStats primaryStats;

    @Nullable
    private final SegmentReplicationState replicaStats;

    public SegmentReplicationShardStatsResponse(StreamInput in) throws IOException {
        this.primaryStats = in.readOptionalWriteable(SegmentReplicationPerGroupStats::new);
        this.replicaStats = in.readOptionalWriteable(SegmentReplicationState::new);
    }

    public SegmentReplicationShardStatsResponse(SegmentReplicationPerGroupStats primaryStats) {
        this.primaryStats = primaryStats;
        this.replicaStats = null;
    }

    public SegmentReplicationShardStatsResponse(SegmentReplicationState replicaStats) {
        this.replicaStats = replicaStats;
        this.primaryStats = null;
    }

    public SegmentReplicationPerGroupStats getPrimaryStats() {
        return primaryStats;
    }

    public SegmentReplicationState getReplicaStats() {
        return replicaStats;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalWriteable(primaryStats);
        out.writeOptionalWriteable(replicaStats);
    }

    @Override
    public String toString() {
        return "SegmentReplicationShardStatsResponse{" + "primaryStats=" + primaryStats + ", replicaStats=" + replicaStats + '}';
    }
}
