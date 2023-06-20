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

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.routing.IndexRoutingTable;
import com.colasoft.opensearch.cluster.routing.RoutingNode;
import com.colasoft.opensearch.cluster.routing.RoutingNodes;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.allocation.allocator.BalancedShardsAllocator;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.index.IndexModule;
import com.colasoft.opensearch.indices.replication.common.ReplicationType;
import com.colasoft.opensearch.test.InternalTestCluster;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.colasoft.opensearch.cluster.routing.ShardRoutingState.STARTED;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;

import com.colasoft.opensearch.cluster.OpenSearchAllocationTestCase.ShardAllocations;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 0)
public class SegmentReplicationAllocationIT extends SegmentReplicationBaseIT {

    private void createIndex(String idxName, int shardCount, int replicaCount, boolean isSegRep) {
        Settings.Builder builder = Settings.builder()
            .put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, shardCount)
            .put(IndexModule.INDEX_QUERY_CACHE_ENABLED_SETTING.getKey(), false)
            .put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, replicaCount);
        if (isSegRep) {
            builder = builder.put(IndexMetadata.SETTING_REPLICATION_TYPE, ReplicationType.SEGMENT);
        } else {
            builder = builder.put(IndexMetadata.SETTING_REPLICATION_TYPE, ReplicationType.DOCUMENT);
        }
        prepareCreate(idxName, builder).get();
    }

    public void enablePreferPrimaryBalance() {
        assertAcked(
            client().admin()
                .cluster()
                .prepareUpdateSettings()
                .setPersistentSettings(Settings.builder().put(BalancedShardsAllocator.PREFER_PRIMARY_SHARD_BALANCE.getKey(), "true"))
        );
    }

    /**
     * This test verifies that the overall primary balance is attained during allocation. This test verifies primary
     * balance per index and across all indices is maintained.
     * @throws Exception
     */
    public void testGlobalPrimaryAllocation() throws Exception {
        internalCluster().startClusterManagerOnlyNode();
        final int maxReplicaCount = 1;
        final int maxShardCount = 1;
        final int nodeCount = randomIntBetween(maxReplicaCount + 1, 10);
        final int numberOfIndices = randomIntBetween(5, 10);

        final List<String> nodeNames = new ArrayList<>();
        logger.info("--> Creating {} nodes", nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            nodeNames.add(internalCluster().startNode());
        }
        enablePreferPrimaryBalance();
        int shardCount, replicaCount;
        ClusterState state;
        for (int i = 0; i < numberOfIndices; i++) {
            shardCount = randomIntBetween(1, maxShardCount);
            replicaCount = randomIntBetween(0, maxReplicaCount);
            createIndex("test" + i, shardCount, replicaCount, i % 2 == 0);
            logger.info("--> Creating index {} with shard count {} and replica count {}", "test" + i, shardCount, replicaCount);
            ensureGreen(TimeValue.timeValueSeconds(60));
        }
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();
        verifyPrimaryBalance();
    }

    /**
     * This test verifies the happy path where primary shard allocation is balanced when multiple indices are created.
     *
     * This test in general passes without primary shard balance as well due to nature of allocation algorithm which
     * assigns all primary shards first followed by replica copies.
     */
    public void testPerIndexPrimaryAllocation() throws Exception {
        internalCluster().startClusterManagerOnlyNode();
        final int maxReplicaCount = 2;
        final int maxShardCount = 5;
        final int nodeCount = randomIntBetween(maxReplicaCount + 1, 10);
        final int numberOfIndices = randomIntBetween(5, 10);

        final List<String> nodeNames = new ArrayList<>();
        logger.info("--> Creating {} nodes", nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            nodeNames.add(internalCluster().startNode());
        }
        enablePreferPrimaryBalance();
        int shardCount, replicaCount;
        ClusterState state;
        for (int i = 0; i < numberOfIndices; i++) {
            shardCount = randomIntBetween(1, maxShardCount);
            replicaCount = randomIntBetween(0, maxReplicaCount);
            createIndex("test" + i, shardCount, replicaCount, i % 2 == 0);
            logger.info("--> Creating index {} with shard count {} and replica count {}", "test" + i, shardCount, replicaCount);
            ensureGreen(TimeValue.timeValueSeconds(60));
            state = client().admin().cluster().prepareState().execute().actionGet().getState();
            logger.info(ShardAllocations.printShardDistribution(state));
        }
        verifyPerIndexPrimaryBalance();
    }

    /**
     * This test verifies balanced primary shard allocation for a single index with large shard count in event of node
     * going down and a new node joining the cluster. The results in shard distribution skewness and re-balancing logic
     * ensures the primary shard distribution is balanced.
     *
     */
    public void testSingleIndexShardAllocation() throws Exception {
        internalCluster().startClusterManagerOnlyNode();
        final int maxReplicaCount = 1;
        final int maxShardCount = 50;
        final int nodeCount = 5;

        final List<String> nodeNames = new ArrayList<>();
        logger.info("--> Creating {} nodes", nodeCount);
        for (int i = 0; i < nodeCount; i++) {
            nodeNames.add(internalCluster().startNode());
        }
        enablePreferPrimaryBalance();

        ClusterState state;
        createIndex("test", maxShardCount, maxReplicaCount, true);
        ensureGreen(TimeValue.timeValueSeconds(60));
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();

        // Remove a node
        internalCluster().stopRandomNode(InternalTestCluster.nameFilter(nodeNames.get(0)));
        ensureGreen(TimeValue.timeValueSeconds(60));
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();

        // Add a new node
        internalCluster().startDataOnlyNode();
        ensureGreen(TimeValue.timeValueSeconds(60));
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();
    }

    /**
     * Similar to testSingleIndexShardAllocation test but creates multiple indices, multiple nodes adding in and getting
     * removed. The test asserts post each such event that primary shard distribution is balanced for each index.
     */
    public void testAllocationWithDisruption() throws Exception {
        internalCluster().startClusterManagerOnlyNode();
        final int maxReplicaCount = 2;
        final int maxShardCount = 2;
        // Create higher number of nodes than number of shards to reduce chances of SameShardAllocationDecider kicking-in
        // and preventing primary relocations
        final int nodeCount = randomIntBetween(5, 10);
        final int numberOfIndices = randomIntBetween(1, 10);

        logger.info("--> Creating {} nodes", nodeCount);
        final List<String> nodeNames = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodeNames.add(internalCluster().startNode());
        }
        enablePreferPrimaryBalance();

        int shardCount, replicaCount;
        ClusterState state;
        for (int i = 0; i < numberOfIndices; i++) {
            shardCount = randomIntBetween(1, maxShardCount);
            replicaCount = randomIntBetween(1, maxReplicaCount);
            logger.info("--> Creating index test{} with primary {} and replica {}", i, shardCount, replicaCount);
            createIndex("test" + i, shardCount, replicaCount, i % 2 == 0);
            ensureGreen(TimeValue.timeValueSeconds(60));
            if (logger.isTraceEnabled()) {
                state = client().admin().cluster().prepareState().execute().actionGet().getState();
                logger.info(ShardAllocations.printShardDistribution(state));
            }
        }
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();

        final int additionalNodeCount = randomIntBetween(1, 5);
        logger.info("--> Adding {} nodes", additionalNodeCount);

        internalCluster().startNodes(additionalNodeCount);
        ensureGreen(TimeValue.timeValueSeconds(60));
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();

        int nodeCountToStop = additionalNodeCount;
        while (nodeCountToStop > 0) {
            internalCluster().stopRandomDataNode();
            // give replica a chance to promote as primary before terminating node containing the replica
            ensureGreen(TimeValue.timeValueSeconds(60));
            nodeCountToStop--;
        }
        state = client().admin().cluster().prepareState().execute().actionGet().getState();
        logger.info("--> Cluster state post nodes stop {}", state);
        logger.info(ShardAllocations.printShardDistribution(state));
        verifyPerIndexPrimaryBalance();
    }

    /**
     * Utility method which ensures cluster has balanced primary shard distribution across a single index.
     * @throws Exception exception
     */
    private void verifyPerIndexPrimaryBalance() throws Exception {
        assertBusy(() -> {
            final ClusterState currentState = client().admin().cluster().prepareState().execute().actionGet().getState();
            RoutingNodes nodes = currentState.getRoutingNodes();
            for (ObjectObjectCursor<String, IndexRoutingTable> index : currentState.getRoutingTable().indicesRouting()) {
                final int totalPrimaryShards = index.value.primaryShardsActive();
                final int avgPrimaryShardsPerNode = (int) Math.ceil(totalPrimaryShards * 1f / currentState.getRoutingNodes().size());
                for (RoutingNode node : nodes) {
                    final int primaryCount = node.shardsWithState(index.key, STARTED)
                        .stream()
                        .filter(ShardRouting::primary)
                        .collect(Collectors.toList())
                        .size();
                    if (primaryCount > avgPrimaryShardsPerNode) {
                        logger.info(
                            "--> Primary shard balance assertion failure for index {} on node {} {} <= {}",
                            index.key,
                            node.node().getName(),
                            primaryCount,
                            avgPrimaryShardsPerNode
                        );
                    }
                    assertTrue(primaryCount <= avgPrimaryShardsPerNode);
                }
            }
        }, 60, TimeUnit.SECONDS);
    }

    private void verifyPrimaryBalance() throws Exception {
        assertBusy(() -> {
            final ClusterState currentState = client().admin().cluster().prepareState().execute().actionGet().getState();
            RoutingNodes nodes = currentState.getRoutingNodes();
            int totalPrimaryShards = 0;
            for (ObjectObjectCursor<String, IndexRoutingTable> index : currentState.getRoutingTable().indicesRouting()) {
                totalPrimaryShards += index.value.primaryShardsActive();
            }
            final int avgPrimaryShardsPerNode = (int) Math.ceil(totalPrimaryShards * 1f / currentState.getRoutingNodes().size());
            for (RoutingNode node : nodes) {
                final int primaryCount = node.shardsWithState(STARTED)
                    .stream()
                    .filter(ShardRouting::primary)
                    .collect(Collectors.toList())
                    .size();
                assertTrue(primaryCount <= avgPrimaryShardsPerNode);
            }
        }, 60, TimeUnit.SECONDS);
    }
}
