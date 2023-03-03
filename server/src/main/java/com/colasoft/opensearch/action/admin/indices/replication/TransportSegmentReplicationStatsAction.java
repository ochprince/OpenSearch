/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.indices.replication;

import com.colasoft.opensearch.OpenSearchStatusException;
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
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.replication.SegmentReplicationState;
import com.colasoft.opensearch.indices.replication.SegmentReplicationTargetService;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Transport action for shard segment replication operation. This transport action does not actually
 * perform segment replication, it only reports on metrics/stats of segment replication event (both active and complete).
 *
 * @opensearch.internal
 */
public class TransportSegmentReplicationStatsAction extends TransportBroadcastByNodeAction<
    SegmentReplicationStatsRequest,
    SegmentReplicationStatsResponse,
    SegmentReplicationState> {

    private final SegmentReplicationTargetService targetService;
    private final IndicesService indicesService;
    private String singleIndexWithSegmentReplicationDisabled = null;

    @Inject
    public TransportSegmentReplicationStatsAction(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        SegmentReplicationTargetService targetService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
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
    }

    @Override
    protected SegmentReplicationState readShardResult(StreamInput in) throws IOException {
        return new SegmentReplicationState(in);
    }

    @Override
    protected SegmentReplicationStatsResponse newResponse(
        SegmentReplicationStatsRequest request,
        int totalShards,
        int successfulShards,
        int failedShards,
        List<SegmentReplicationState> responses,
        List<DefaultShardOperationFailedException> shardFailures,
        ClusterState clusterState
    ) {
        // throw exception if API call is made on single index with segment replication disabled.
        if (singleIndexWithSegmentReplicationDisabled != null) {
            String index = singleIndexWithSegmentReplicationDisabled;
            singleIndexWithSegmentReplicationDisabled = null;
            throw new OpenSearchStatusException("Segment Replication is not enabled on Index: " + index, RestStatus.BAD_REQUEST);
        }
        String[] shards = request.shards();
        Set<String> set = new HashSet<>();
        if (shards.length > 0) {
            for (String shard : shards) {
                set.add(shard);
            }
        }
        Map<String, List<SegmentReplicationState>> shardResponses = new HashMap<>();
        for (SegmentReplicationState segmentReplicationState : responses) {
            if (segmentReplicationState == null) {
                continue;
            }

            // Limit responses to only specific shard id's passed in query paramter shards.
            int shardId = segmentReplicationState.getShardRouting().shardId().id();
            if (shards.length > 0 && set.contains(Integer.toString(shardId)) == false) {
                continue;
            }
            String indexName = segmentReplicationState.getShardRouting().getIndexName();
            if (!shardResponses.containsKey(indexName)) {
                shardResponses.put(indexName, new ArrayList<>());
            }
            shardResponses.get(indexName).add(segmentReplicationState);
        }
        return new SegmentReplicationStatsResponse(totalShards, successfulShards, failedShards, shardResponses, shardFailures);
    }

    @Override
    protected SegmentReplicationStatsRequest readRequestFrom(StreamInput in) throws IOException {
        return new SegmentReplicationStatsRequest(in);
    }

    @Override
    protected SegmentReplicationState shardOperation(SegmentReplicationStatsRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.getShard(shardRouting.shardId().id());
        ShardId shardId = shardRouting.shardId();

        // check if API call is made on single index with segment replication disabled.
        if (request.indices().length == 1 && indexShard.indexSettings().isSegRepEnabled() == false) {
            singleIndexWithSegmentReplicationDisabled = shardRouting.getIndexName();
            return null;
        }
        if (indexShard.indexSettings().isSegRepEnabled() == false) {
            return null;
        }

        // return information about only on-going segment replication events.
        if (request.activeOnly()) {
            return targetService.getOngoingEventSegmentReplicationState(shardId);
        }

        // return information about only latest completed segment replication events.
        if (request.completedOnly()) {
            return targetService.getlatestCompletedEventSegmentReplicationState(shardId);
        }
        return targetService.getSegmentReplicationState(shardId);
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
