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

import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.transport.TransportService;

/**
 * Factory to build {@link SegmentReplicationSource} used by {@link SegmentReplicationTargetService}.
 *
 * @opensearch.internal
 */
public class SegmentReplicationSourceFactory {

    private final TransportService transportService;
    private final RecoverySettings recoverySettings;
    private final ClusterService clusterService;

    public SegmentReplicationSourceFactory(
        TransportService transportService,
        RecoverySettings recoverySettings,
        ClusterService clusterService
    ) {
        this.transportService = transportService;
        this.recoverySettings = recoverySettings;
        this.clusterService = clusterService;
    }

    public SegmentReplicationSource get(IndexShard shard) {
        return new PrimaryShardReplicationSource(
            shard.recoveryState().getTargetNode(),
            shard.routingEntry().allocationId().getId(),
            transportService,
            recoverySettings,
            getPrimaryNode(shard.shardId())
        );
    }

    private DiscoveryNode getPrimaryNode(ShardId shardId) {
        ShardRouting primaryShard = clusterService.state().routingTable().shardRoutingTable(shardId).primaryShard();
        return clusterService.state().nodes().get(primaryShard.currentNodeId());
    }
}
