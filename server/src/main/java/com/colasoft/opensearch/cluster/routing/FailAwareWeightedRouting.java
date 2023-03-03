/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.routing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.action.search.SearchShardIterator;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.search.SearchShardTarget;

import java.util.List;

/**
 * This class contains logic to find next shard to retry search request in case of failure from other shard copy.
 * This decides if retryable shard search requests can be tried on shard copies present in data
 * nodes whose attribute value weight for weighted shard routing is set to zero.
 */

public class FailAwareWeightedRouting {

    public static final FailAwareWeightedRouting INSTANCE = new FailAwareWeightedRouting();
    private static final Logger logger = LogManager.getLogger(FailAwareWeightedRouting.class);

    private final static List<RestStatus> internalErrorRestStatusList = List.of(
        RestStatus.INTERNAL_SERVER_ERROR,
        RestStatus.BAD_GATEWAY,
        RestStatus.SERVICE_UNAVAILABLE,
        RestStatus.GATEWAY_TIMEOUT
    );

    public static FailAwareWeightedRouting getInstance() {
        return INSTANCE;
    }

    /**
     * *
     * @return true if exception is due to cluster availability issues
     */
    private boolean isInternalFailure(Exception exception) {
        if (exception instanceof OpenSearchException) {
            // checking for 5xx failures
            return internalErrorRestStatusList.contains(((OpenSearchException) exception).status());
        }
        return false;
    }

    /**
     * This function returns next shard copy to retry search request in case of failure from previous copy returned
     * by the iterator. It has the logic to fail open ie request shard copies present in nodes with weighted shard
     * routing weight set to zero
     *
     * @param shardIt Shard Iterator containing order in which shard copies for a shard need to be requested
     * @return the next shard copy
     */
    public SearchShardTarget findNext(final SearchShardIterator shardIt, ClusterState clusterState, Exception exception) {
        SearchShardTarget next = shardIt.nextOrNull();
        while (next != null && WeightedRoutingUtils.isWeighedAway(next.getNodeId(), clusterState)) {
            SearchShardTarget nextShard = next;
            if (canFailOpen(nextShard.getShardId(), exception, clusterState)) {
                logger.info(() -> new ParameterizedMessage("{}: Fail open executed due to exception", nextShard.getShardId()), exception);
                getWeightedRoutingStats().updateFailOpenCount();
                break;
            }
            next = shardIt.nextOrNull();
        }
        return next;
    }

    /**
     * This function returns next shard copy to retry search request in case of failure from previous copy returned
     * by the iterator. It has the logic to fail open ie request shard copies present in nodes with weighted shard
     * routing weight set to zero
     *
     * @param shardsIt Shard Iterator containing order in which shard copies for a shard need to be requested
     * @return the next shard copy
     */
    public ShardRouting findNext(final ShardsIterator shardsIt, ClusterState clusterState, Exception exception) {
        ShardRouting next = shardsIt.nextOrNull();

        while (next != null && WeightedRoutingUtils.isWeighedAway(next.currentNodeId(), clusterState)) {
            ShardRouting nextShard = next;
            if (canFailOpen(nextShard.shardId(), exception, clusterState)) {
                logger.info(() -> new ParameterizedMessage("{}: Fail open executed due to exception", nextShard.shardId()), exception);
                getWeightedRoutingStats().updateFailOpenCount();
                break;
            }
            next = shardsIt.nextOrNull();
        }
        return next;
    }

    /**
     * *
     * @return true if can fail open ie request shard copies present in nodes with weighted shard
     * routing weight set to zero
     */
    private boolean canFailOpen(ShardId shardId, Exception exception, ClusterState clusterState) {
        return isInternalFailure(exception) || hasInActiveShardCopies(clusterState, shardId);
    }

    private boolean hasInActiveShardCopies(ClusterState clusterState, ShardId shardId) {
        List<ShardRouting> shards = clusterState.routingTable().shardRoutingTable(shardId).shards();
        for (ShardRouting shardRouting : shards) {
            if (!shardRouting.active()) {
                return true;
            }
        }
        return false;
    }

    public WeightedRoutingStats getWeightedRoutingStats() {
        return WeightedRoutingStats.getInstance();
    }
}
