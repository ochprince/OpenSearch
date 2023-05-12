/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.indices.replication;

import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.DefaultShardOperationFailedException;
import com.colasoft.opensearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardsIterator;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.SegmentReplicationPerGroupStats;
import com.colasoft.opensearch.index.SegmentReplicationPressureService;
import com.colasoft.opensearch.index.SegmentReplicationShardStats;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.replication.SegmentReplicationState;
import com.colasoft.opensearch.indices.replication.SegmentReplicationTargetService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Transport action for shard segment replication operation. This transport action does not actually
 * perform segment replication, it only reports on metrics/stats of segment replication event (both active and complete).
 *
 * @opensearch.internal
 */
public class TransportSegmentReplicationStatsAction extends TransportBroadcastByNodeAction<
    SegmentReplicationStatsRequest,
    SegmentReplicationStatsResponse,
    SegmentReplicationShardStatsResponse> {

    private final SegmentReplicationTargetService targetService;
    private final IndicesService indicesService;
    private final SegmentReplicationPressureService pressureService;

    @Inject
    public TransportSegmentReplicationStatsAction(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        SegmentReplicationTargetService targetService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        SegmentReplicationPressureService pressureService
    ) {
        super(
            SegmentReplicationStatsAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            SegmentReplicationStatsRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.indicesService = indicesService;
        this.targetService = targetService;
        this.pressureService = pressureService;
    }

    @Override
    protected SegmentReplicationShardStatsResponse readShardResult(StreamInput in) throws IOException {
        return new SegmentReplicationShardStatsResponse(in);
    }

    @Override
    protected SegmentReplicationStatsResponse newResponse(
        SegmentReplicationStatsRequest request,
        int totalShards,
        int successfulShards,
        int failedShards,
        List<SegmentReplicationShardStatsResponse> responses,
        List<DefaultShardOperationFailedException> shardFailures,
        ClusterState clusterState
    ) {
        String[] shards = request.shards();
        final List<Integer> shardsToFetch = Arrays.stream(shards).map(Integer::valueOf).collect(Collectors.toList());

        // organize replica responses by allocationId.
        final Map<String, SegmentReplicationState> replicaStats = new HashMap<>();
        // map of index name to list of replication group stats.
        final Map<String, List<SegmentReplicationPerGroupStats>> primaryStats = new HashMap<>();
        for (SegmentReplicationShardStatsResponse response : responses) {
            if (response != null) {
                if (response.getReplicaStats() != null) {
                    final ShardRouting shardRouting = response.getReplicaStats().getShardRouting();
                    if (shardsToFetch.isEmpty() || shardsToFetch.contains(shardRouting.shardId().getId())) {
                        replicaStats.putIfAbsent(shardRouting.allocationId().getId(), response.getReplicaStats());
                    }
                }
                if (response.getPrimaryStats() != null) {
                    final ShardId shardId = response.getPrimaryStats().getShardId();
                    if (shardsToFetch.isEmpty() || shardsToFetch.contains(shardId.getId())) {
                        primaryStats.compute(shardId.getIndexName(), (k, v) -> {
                            if (v == null) {
                                final ArrayList<SegmentReplicationPerGroupStats> list = new ArrayList<>();
                                list.add(response.getPrimaryStats());
                                return list;
                            } else {
                                v.add(response.getPrimaryStats());
                                return v;
                            }
                        });
                    }
                }
            }
        }
        // combine the replica stats to the shard stat entry in each group.
        for (Map.Entry<String, List<SegmentReplicationPerGroupStats>> entry : primaryStats.entrySet()) {
            for (SegmentReplicationPerGroupStats group : entry.getValue()) {
                for (SegmentReplicationShardStats replicaStat : group.getReplicaStats()) {
                    replicaStat.setCurrentReplicationState(replicaStats.getOrDefault(replicaStat.getAllocationId(), null));
                }
            }
        }
        return new SegmentReplicationStatsResponse(totalShards, successfulShards, failedShards, primaryStats, shardFailures);
    }

    @Override
    protected SegmentReplicationStatsRequest readRequestFrom(StreamInput in) throws IOException {
        return new SegmentReplicationStatsRequest(in);
    }

    @Override
    protected SegmentReplicationShardStatsResponse shardOperation(SegmentReplicationStatsRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.getShard(shardRouting.shardId().id());
        ShardId shardId = shardRouting.shardId();

        if (indexShard.indexSettings().isSegRepEnabled() == false) {
            return null;
        }

        if (shardRouting.primary()) {
            return new SegmentReplicationShardStatsResponse(pressureService.getStatsForShard(indexShard));
        }

        // return information about only on-going segment replication events.
        if (request.activeOnly()) {
            return new SegmentReplicationShardStatsResponse(targetService.getOngoingEventSegmentReplicationState(shardId));
        }
        return new SegmentReplicationShardStatsResponse(targetService.getSegmentReplicationState(shardId));
    }

    @Override
    protected ShardsIterator shards(ClusterState state, SegmentReplicationStatsRequest request, String[] concreteIndices) {
        return state.routingTable().allShardsIncludingRelocationTargets(concreteIndices);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, SegmentReplicationStatsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(
        ClusterState state,
        SegmentReplicationStatsRequest request,
        String[] concreteIndices
    ) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }
}
