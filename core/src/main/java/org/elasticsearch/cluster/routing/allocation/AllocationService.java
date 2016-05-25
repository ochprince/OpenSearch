/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.cluster.routing.allocation;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.elasticsearch.cluster.ClusterInfoService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.health.ClusterStateHealth;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.AllocationId;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.cluster.routing.allocation.allocator.ShardsAllocator;
import org.elasticsearch.cluster.routing.allocation.command.AllocationCommands;
import org.elasticsearch.cluster.routing.allocation.decider.AllocationDeciders;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.gateway.GatewayAllocator;
import org.elasticsearch.index.shard.ShardId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This service manages the node allocation of a cluster. For this reason the
 * {@link AllocationService} keeps {@link AllocationDeciders} to choose nodes
 * for shard allocation. This class also manages new nodes joining the cluster
 * and rerouting of shards.
 */
public class AllocationService extends AbstractComponent {

    private final AllocationDeciders allocationDeciders;
    private final GatewayAllocator gatewayAllocator;
    private final ShardsAllocator shardsAllocator;
    private final ClusterInfoService clusterInfoService;

    @Inject
    public AllocationService(Settings settings, AllocationDeciders allocationDeciders, GatewayAllocator gatewayAllocator,
                             ShardsAllocator shardsAllocator, ClusterInfoService clusterInfoService) {
        super(settings);
        this.allocationDeciders = allocationDeciders;
        this.gatewayAllocator = gatewayAllocator;
        this.shardsAllocator = shardsAllocator;
        this.clusterInfoService = clusterInfoService;
    }

    /**
     * Applies the started shards. Note, shards can be called several times within this method.
     * <p>
     * If the same instance of the routing table is returned, then no change has been made.</p>
     */
    public RoutingAllocation.Result applyStartedShards(ClusterState clusterState, List<? extends ShardRouting> startedShards) {
        return applyStartedShards(clusterState, startedShards, true);
    }

    public RoutingAllocation.Result applyStartedShards(ClusterState clusterState, List<? extends ShardRouting> startedShards, boolean withReroute) {
        RoutingNodes routingNodes = getMutableRoutingNodes(clusterState);
        // shuffle the unassigned nodes, just so we won't have things like poison failed shards
        routingNodes.unassigned().shuffle();
        StartedRerouteAllocation allocation = new StartedRerouteAllocation(allocationDeciders, routingNodes, clusterState, startedShards, clusterInfoService.getClusterInfo());
        boolean changed = applyStartedShards(routingNodes, startedShards);
        if (!changed) {
            return new RoutingAllocation.Result(false, clusterState.routingTable(), clusterState.metaData());
        }
        gatewayAllocator.applyStartedShards(allocation);
        if (withReroute) {
            reroute(allocation);
        }
        final RoutingAllocation.Result result = buildChangedResult(clusterState.metaData(), clusterState.routingTable(), routingNodes);

        String startedShardsAsString = firstListElementsToCommaDelimitedString(startedShards, s -> s.shardId().toString());
        logClusterHealthStateChange(
                new ClusterStateHealth(clusterState),
                new ClusterStateHealth(clusterState.metaData(), result.routingTable()),
                "shards started [" + startedShardsAsString + "] ..."
        );
        return result;

    }

    protected RoutingAllocation.Result buildChangedResult(MetaData oldMetaData, RoutingTable oldRoutingTable, RoutingNodes newRoutingNodes) {
        return buildChangedResult(oldMetaData, oldRoutingTable, newRoutingNodes, new RoutingExplanations());

    }

    protected RoutingAllocation.Result buildChangedResult(MetaData oldMetaData, RoutingTable oldRoutingTable, RoutingNodes newRoutingNodes,
                                                          RoutingExplanations explanations) {
        final RoutingTable newRoutingTable = new RoutingTable.Builder().updateNodes(newRoutingNodes).build();
        MetaData newMetaData = updateMetaDataWithRoutingTable(oldMetaData, oldRoutingTable, newRoutingTable);
        assert newRoutingTable.validate(newMetaData); // validates the routing table is coherent with the cluster state metadata
        return new RoutingAllocation.Result(true, newRoutingTable, newMetaData, explanations);
    }

    /**
     * Updates the current {@link MetaData} based on the newly created {@link RoutingTable}. Specifically
     * we update {@link IndexMetaData#getActiveAllocationIds()} and {@link IndexMetaData#primaryTerm(int)} based on
     * the changes made during this allocation.
     *
     * @param oldMetaData     {@link MetaData} object from before the routing table was changed.
     * @param oldRoutingTable {@link RoutingTable} from before the  change.
     * @param newRoutingTable new {@link RoutingTable} created by the allocation change
     * @return adapted {@link MetaData}, potentially the original one if no change was needed.
     */
    static MetaData updateMetaDataWithRoutingTable(MetaData oldMetaData, RoutingTable oldRoutingTable, RoutingTable newRoutingTable) {
        MetaData.Builder metaDataBuilder = null;
        for (IndexRoutingTable newIndexTable : newRoutingTable) {
            final IndexMetaData oldIndexMetaData = oldMetaData.index(newIndexTable.getIndex());
            if (oldIndexMetaData == null) {
                throw new IllegalStateException("no metadata found for index " + newIndexTable.getIndex().getName());
            }
            IndexMetaData.Builder indexMetaDataBuilder = null;
            for (IndexShardRoutingTable newShardTable : newIndexTable) {
                final ShardId shardId = newShardTable.shardId();

                // update activeAllocationIds
                Set<String> activeAllocationIds = newShardTable.activeShards().stream()
                        .map(ShardRouting::allocationId)
                        .filter(Objects::nonNull)
                        .map(AllocationId::getId)
                        .collect(Collectors.toSet());
                // only update active allocation ids if there is an active shard
                if (activeAllocationIds.isEmpty() == false) {
                    // get currently stored allocation ids
                    Set<String> storedAllocationIds = oldIndexMetaData.activeAllocationIds(shardId.id());
                    if (activeAllocationIds.equals(storedAllocationIds) == false) {
                        if (indexMetaDataBuilder == null) {
                            indexMetaDataBuilder = IndexMetaData.builder(oldIndexMetaData);
                        }
                        indexMetaDataBuilder.putActiveAllocationIds(shardId.id(), activeAllocationIds);
                    }
                }

                // update primary terms
                final ShardRouting newPrimary = newShardTable.primaryShard();
                if (newPrimary == null) {
                    throw new IllegalStateException("missing primary shard for " + newShardTable.shardId());
                }
                final ShardRouting oldPrimary = oldRoutingTable.shardRoutingTable(shardId).primaryShard();
                if (oldPrimary == null) {
                    throw new IllegalStateException("missing primary shard for " + newShardTable.shardId());
                }
                // we update the primary term on initial assignment or when a replica is promoted. Most notably we do *not*
                // update them when a primary relocates
                if (newPrimary.unassigned() ||
                        newPrimary.isSameAllocation(oldPrimary) ||
                        // we do not use newPrimary.isTargetRelocationOf(oldPrimary) because that one enforces newPrimary to
                        // be initializing. However, when the target shard is activated, we still want the primary term to staty
                        // the same
                        (oldPrimary.relocating() && newPrimary.isSameAllocation(oldPrimary.buildTargetRelocatingShard()))) {
                    // do nothing
                } else {
                    // incrementing the primary term
                    if (indexMetaDataBuilder == null) {
                        indexMetaDataBuilder = IndexMetaData.builder(oldIndexMetaData);
                    }
                    indexMetaDataBuilder.primaryTerm(shardId.id(), oldIndexMetaData.primaryTerm(shardId.id()) + 1);
                }
            }
            if (indexMetaDataBuilder != null) {
                if (metaDataBuilder == null) {
                    metaDataBuilder = MetaData.builder(oldMetaData);
                }
                metaDataBuilder.put(indexMetaDataBuilder);
            }
        }
        if (metaDataBuilder != null) {
            return metaDataBuilder.build();
        } else {
            return oldMetaData;
        }
    }

    public RoutingAllocation.Result applyFailedShard(ClusterState clusterState, ShardRouting failedShard) {
        return applyFailedShards(clusterState, Collections.singletonList(new FailedRerouteAllocation.FailedShard(failedShard, null, null)));
    }

    /**
     * Applies the failed shards. Note, shards can be called several times within this method.
     * <p>
     * If the same instance of the routing table is returned, then no change has been made.</p>
     */
    public RoutingAllocation.Result applyFailedShards(ClusterState clusterState, List<FailedRerouteAllocation.FailedShard> failedShards) {
        RoutingNodes routingNodes = getMutableRoutingNodes(clusterState);
        // shuffle the unassigned nodes, just so we won't have things like poison failed shards
        routingNodes.unassigned().shuffle();
        FailedRerouteAllocation allocation = new FailedRerouteAllocation(allocationDeciders, routingNodes, clusterState, failedShards, clusterInfoService.getClusterInfo());
        boolean changed = false;
        // as failing primaries also fail associated replicas, we fail replicas first here so that their nodes are added to ignore list
        List<FailedRerouteAllocation.FailedShard> orderedFailedShards = new ArrayList<>(failedShards);
        orderedFailedShards.sort(Comparator.comparing(failedShard -> failedShard.shard.primary()));
        for (FailedRerouteAllocation.FailedShard failedShard : orderedFailedShards) {
            UnassignedInfo unassignedInfo = failedShard.shard.unassignedInfo();
            final int failedAllocations = unassignedInfo != null ? unassignedInfo.getNumFailedAllocations() : 0;
            changed |= applyFailedShard(allocation, failedShard.shard, true, new UnassignedInfo(UnassignedInfo.Reason.ALLOCATION_FAILED, failedShard.message, failedShard.failure,
                    failedAllocations + 1, System.nanoTime(), System.currentTimeMillis()));
        }
        if (!changed) {
            return new RoutingAllocation.Result(false, clusterState.routingTable(), clusterState.metaData());
        }
        gatewayAllocator.applyFailedShards(allocation);
        reroute(allocation);
        final RoutingAllocation.Result result = buildChangedResult(clusterState.metaData(), clusterState.routingTable(), routingNodes);
        String failedShardsAsString = firstListElementsToCommaDelimitedString(failedShards, s -> s.shard.shardId().toString());
        logClusterHealthStateChange(
                new ClusterStateHealth(clusterState),
                new ClusterStateHealth(clusterState.getMetaData(), result.routingTable()),
                "shards failed [" + failedShardsAsString + "] ..."
        );
        return result;
    }

    /**
     * Internal helper to cap the number of elements in a potentially long list for logging.
     *
     * @param elements  The elements to log. May be any non-null list. Must not be null.
     * @param formatter A function that can convert list elements to a String. Must not be null.
     * @param <T>       The list element type.
     * @return A comma-separated string of the first few elements.
     */
    private <T> String firstListElementsToCommaDelimitedString(List<T> elements, Function<T, String> formatter) {
        final int maxNumberOfElements = 10;
        return elements
                .stream()
                .limit(maxNumberOfElements)
                .map(formatter)
                .collect(Collectors.joining(", "));
    }

    public RoutingAllocation.Result reroute(ClusterState clusterState, AllocationCommands commands, boolean explain, boolean retryFailed) {
        RoutingNodes routingNodes = getMutableRoutingNodes(clusterState);
        // we don't shuffle the unassigned shards here, to try and get as close as possible to
        // a consistent result of the effect the commands have on the routing
        // this allows systems to dry run the commands, see the resulting cluster state, and act on it
        RoutingAllocation allocation = new RoutingAllocation(allocationDeciders, routingNodes, clusterState,
            clusterInfoService.getClusterInfo(), currentNanoTime(), retryFailed);
        // don't short circuit deciders, we want a full explanation
        allocation.debugDecision(true);
        // we ignore disable allocation, because commands are explicit
        allocation.ignoreDisable(true);
        RoutingExplanations explanations = commands.execute(allocation, explain);
        // we revert the ignore disable flag, since when rerouting, we want the original setting to take place
        allocation.ignoreDisable(false);
        // the assumption is that commands will move / act on shards (or fail through exceptions)
        // so, there will always be shard "movements", so no need to check on reroute
        reroute(allocation);
        RoutingAllocation.Result result = buildChangedResult(clusterState.metaData(), clusterState.routingTable(), routingNodes, explanations);
        logClusterHealthStateChange(
                new ClusterStateHealth(clusterState),
                new ClusterStateHealth(clusterState.getMetaData(), result.routingTable()),
                "reroute commands"
        );
        return result;
    }


    /**
     * Reroutes the routing table based on the live nodes.
     * <p>
     * If the same instance of the routing table is returned, then no change has been made.
     */
    public RoutingAllocation.Result reroute(ClusterState clusterState, String reason) {
        return reroute(clusterState, reason, false);
    }

    /**
     * Reroutes the routing table based on the live nodes.
     * <p>
     * If the same instance of the routing table is returned, then no change has been made.
     */
    protected RoutingAllocation.Result reroute(ClusterState clusterState, String reason, boolean debug) {
        RoutingNodes routingNodes = getMutableRoutingNodes(clusterState);
        // shuffle the unassigned nodes, just so we won't have things like poison failed shards
        routingNodes.unassigned().shuffle();
        RoutingAllocation allocation = new RoutingAllocation(allocationDeciders, routingNodes, clusterState,
            clusterInfoService.getClusterInfo(), currentNanoTime(), false);
        allocation.debugDecision(debug);
        if (!reroute(allocation)) {
            return new RoutingAllocation.Result(false, clusterState.routingTable(), clusterState.metaData());
        }
        RoutingAllocation.Result result = buildChangedResult(clusterState.metaData(), clusterState.routingTable(), routingNodes);
        logClusterHealthStateChange(
                new ClusterStateHealth(clusterState),
                new ClusterStateHealth(clusterState.getMetaData(), result.routingTable()),
                reason
        );
        return result;
    }

    private void logClusterHealthStateChange(ClusterStateHealth previousStateHealth, ClusterStateHealth newStateHealth, String reason) {
        ClusterHealthStatus previousHealth = previousStateHealth.getStatus();
        ClusterHealthStatus currentHealth = newStateHealth.getStatus();
        if (!previousHealth.equals(currentHealth)) {
            logger.info("Cluster health status changed from [{}] to [{}] (reason: [{}]).", previousHealth, currentHealth, reason);
        }
    }

    private boolean reroute(RoutingAllocation allocation) {
        boolean changed = false;
        // first, clear from the shards any node id they used to belong to that is now dead
        changed |= deassociateDeadNodes(allocation);

        // create a sorted list of from nodes with least number of shards to the maximum ones
        applyNewNodes(allocation);

        // elect primaries *before* allocating unassigned, so backups of primaries that failed
        // will be moved to primary state and not wait for primaries to be allocated and recovered (*from gateway*)
        changed |= electPrimariesAndUnassignedDanglingReplicas(allocation);

        // now allocate all the unassigned to available nodes
        if (allocation.routingNodes().unassigned().size() > 0) {
            updateLeftDelayOfUnassignedShards(allocation, settings);

            changed |= gatewayAllocator.allocateUnassigned(allocation);
        }

        changed |= shardsAllocator.allocate(allocation);
        assert RoutingNodes.assertShardStats(allocation.routingNodes());
        return changed;
    }

    // public for testing
    public static void updateLeftDelayOfUnassignedShards(RoutingAllocation allocation, Settings settings) {
        final RoutingNodes.UnassignedShards.UnassignedIterator unassignedIterator = allocation.routingNodes().unassigned().iterator();
        final MetaData metaData = allocation.metaData();
        while (unassignedIterator.hasNext()) {
            ShardRouting shardRouting = unassignedIterator.next();
            final IndexMetaData indexMetaData = metaData.getIndexSafe(shardRouting.index());
            UnassignedInfo previousUnassignedInfo = shardRouting.unassignedInfo();
            UnassignedInfo updatedUnassignedInfo = previousUnassignedInfo.updateDelay(allocation.getCurrentNanoTime(), settings,
                indexMetaData.getSettings());
            if (updatedUnassignedInfo != previousUnassignedInfo) { // reference equality!
                unassignedIterator.updateUnassignedInfo(updatedUnassignedInfo);
            }
        }
    }

    private boolean electPrimariesAndUnassignedDanglingReplicas(RoutingAllocation allocation) {
        boolean changed = false;
        final RoutingNodes routingNodes = allocation.routingNodes();
        if (routingNodes.unassigned().getNumPrimaries() == 0) {
            // move out if we don't have unassigned primaries
            return changed;
        }
        // now, go over and elect a new primary if possible, not, from this code block on, if one is elected,
        // routingNodes.hasUnassignedPrimaries() will potentially be false
        final RoutingNodes.UnassignedShards.UnassignedIterator unassignedIterator = routingNodes.unassigned().iterator();
        while (unassignedIterator.hasNext()) {
            ShardRouting shardEntry = unassignedIterator.next();
            if (shardEntry.primary()) {
                // remove dangling replicas that are initializing for primary shards
                changed |= failReplicasForUnassignedPrimary(allocation, shardEntry);
                ShardRouting candidate = allocation.routingNodes().activeReplica(shardEntry);
                if (candidate != null) {
                    shardEntry = unassignedIterator.demotePrimaryToReplicaShard();
                    ShardRouting primarySwappedCandidate = routingNodes.promoteAssignedReplicaShardToPrimary(candidate);
                    if (primarySwappedCandidate.relocatingNodeId() != null) {
                        changed = true;
                        // its also relocating, make sure to move the other routing to primary
                        RoutingNode node = routingNodes.node(primarySwappedCandidate.relocatingNodeId());
                        if (node != null) {
                            for (ShardRouting shardRouting : node) {
                                if (shardRouting.shardId().equals(primarySwappedCandidate.shardId()) && !shardRouting.primary()) {
                                    routingNodes.promoteAssignedReplicaShardToPrimary(shardRouting);
                                    break;
                                }
                            }
                        }
                    }
                    IndexMetaData index = allocation.metaData().getIndexSafe(primarySwappedCandidate.index());
                    if (IndexMetaData.isIndexUsingShadowReplicas(index.getSettings())) {
                        routingNodes.reinitShadowPrimary(primarySwappedCandidate);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    /**
     * Applies the new nodes to the routing nodes and returns them (just the
     * new nodes);
     */
    private void applyNewNodes(RoutingAllocation allocation) {
        final RoutingNodes routingNodes = allocation.routingNodes();
        for (ObjectCursor<DiscoveryNode> cursor : allocation.nodes().getDataNodes().values()) {
            DiscoveryNode node = cursor.value;
            if (!routingNodes.isKnown(node)) {
                routingNodes.addNode(node);
            }
        }
    }

    private boolean deassociateDeadNodes(RoutingAllocation allocation) {
        boolean changed = false;
        for (RoutingNodes.RoutingNodesIterator it = allocation.routingNodes().nodes(); it.hasNext(); ) {
            RoutingNode node = it.next();
            if (allocation.nodes().getDataNodes().containsKey(node.nodeId())) {
                // its a live node, continue
                continue;
            }
            changed = true;
            // now, go over all the shards routing on the node, and fail them
            for (ShardRouting shardRouting : node.copyShards()) {
                UnassignedInfo unassignedInfo = new UnassignedInfo(UnassignedInfo.Reason.NODE_LEFT, "node_left[" + node.nodeId() + "]", null,
                        0, allocation.getCurrentNanoTime(), System.currentTimeMillis());
                applyFailedShard(allocation, shardRouting, false, unassignedInfo);
            }
            // its a dead node, remove it, note, its important to remove it *after* we apply failed shard
            // since it relies on the fact that the RoutingNode exists in the list of nodes
            it.remove();
        }
        return changed;
    }

    private boolean failReplicasForUnassignedPrimary(RoutingAllocation allocation, ShardRouting primary) {
        List<ShardRouting> replicas = new ArrayList<>();
        for (ShardRouting routing : allocation.routingNodes().assignedShards(primary)) {
            if (!routing.primary() && routing.initializing()) {
                replicas.add(routing);
            }
        }
        boolean changed = false;
        for (ShardRouting routing : replicas) {
            changed |= applyFailedShard(allocation, routing, false,
                    new UnassignedInfo(UnassignedInfo.Reason.PRIMARY_FAILED, "primary failed while replica initializing",
                            null, 0, allocation.getCurrentNanoTime(), System.currentTimeMillis()));
        }
        return changed;
    }

    private boolean applyStartedShards(RoutingNodes routingNodes, Iterable<? extends ShardRouting> startedShardEntries) {
        boolean dirty = false;
        // apply shards might be called several times with the same shard, ignore it
        for (ShardRouting startedShard : startedShardEntries) {
            assert startedShard.initializing();

            // validate index still exists. strictly speaking this is not needed but it gives clearer logs
            if (routingNodes.routingTable().index(startedShard.index()) == null) {
                logger.debug("{} ignoring shard started, unknown index (routing: {})", startedShard.shardId(), startedShard);
                continue;
            }


            RoutingNode currentRoutingNode = routingNodes.node(startedShard.currentNodeId());
            if (currentRoutingNode == null) {
                logger.debug("{} failed to find shard in order to start it [failed to find node], ignoring (routing: {})", startedShard.shardId(), startedShard);
                continue;
            }

            ShardRouting matchingShard = currentRoutingNode.getByShardId(startedShard.shardId());
            if (matchingShard == null) {
                logger.debug("{} failed to find shard in order to start it [failed to find shard], ignoring (routing: {})", startedShard.shardId(), startedShard);
            } else if (matchingShard.isSameAllocation(startedShard) == false) {
                logger.debug("{} failed to find shard with matching allocation id in order to start it [failed to find matching shard], ignoring (routing: {}, matched shard routing: {})", startedShard.shardId(), startedShard, matchingShard);
            } else {
                if (matchingShard.active()) {
                    logger.trace("{} shard is already started, ignoring (routing: {})", startedShard.shardId(), startedShard);
                } else {
                    dirty = true;
                    // override started shard with the latest copy.
                    startedShard = matchingShard;
                    routingNodes.started(matchingShard);
                    logger.trace("{} marked shard as started (routing: {})", startedShard.shardId(), startedShard);
                }
            }

            // startedShard is the current state of the shard (post relocation for example)
            // this means that after relocation, the state will be started and the currentNodeId will be
            // the node we relocated to
            if (startedShard.relocatingNodeId() == null) {
                continue;
            }

            RoutingNodes.RoutingNodeIterator sourceRoutingNode = routingNodes.routingNodeIter(startedShard.relocatingNodeId());
            if (sourceRoutingNode != null) {
                while (sourceRoutingNode.hasNext()) {
                    ShardRouting shard = sourceRoutingNode.next();
                    if (shard.isRelocationSourceOf(startedShard)) {
                        dirty = true;
                        sourceRoutingNode.remove();
                        break;
                    }
                }
            }
        }
        return dirty;
    }

    /**
     * Applies the relevant logic to handle a failed shard. Returns <tt>true</tt> if changes happened that
     * require relocation.
     */
    private boolean applyFailedShard(RoutingAllocation allocation, ShardRouting failedShard, boolean addToIgnoreList, UnassignedInfo unassignedInfo) {
        IndexRoutingTable indexRoutingTable = allocation.routingTable().index(failedShard.index());
        if (indexRoutingTable == null) {
            logger.debug("{} ignoring shard failure, unknown index in {} ({})", failedShard.shardId(), failedShard, unassignedInfo.shortSummary());
            return false;
        }
        RoutingNodes routingNodes = allocation.routingNodes();

        RoutingNodes.RoutingNodeIterator matchedNode = routingNodes.routingNodeIter(failedShard.currentNodeId());
        if (matchedNode == null) {
            logger.debug("{} ignoring shard failure, unknown node in {} ({})", failedShard.shardId(), failedShard, unassignedInfo.shortSummary());
            return false;
        }

        boolean matchedShard = false;
        while (matchedNode.hasNext()) {
            ShardRouting routing = matchedNode.next();
            if (routing.isSameAllocation(failedShard)) {
                matchedShard = true;
                logger.debug("{} failed shard {} found in routingNodes, failing it ({})", failedShard.shardId(), failedShard, unassignedInfo.shortSummary());
                break;
            }
        }

        if (matchedShard == false) {
            logger.debug("{} ignoring shard failure, unknown allocation id in {} ({})", failedShard.shardId(), failedShard, unassignedInfo.shortSummary());
            return false;
        }
        if (failedShard.primary()) {
            // fail replicas first otherwise we move RoutingNodes into an inconsistent state
            failReplicasForUnassignedPrimary(allocation, failedShard);
        }
        // replace incoming instance to make sure we work on the latest one
        failedShard = matchedNode.current();

        // remove the current copy of the shard
        matchedNode.remove();

        if (addToIgnoreList) {
            // make sure we ignore this shard on the relevant node
            allocation.addIgnoreShardForNode(failedShard.shardId(), failedShard.currentNodeId());
        }

        if (failedShard.relocatingNodeId() != null && failedShard.initializing()) {
            // The shard is a target of a relocating shard. In that case we only
            // need to remove the target shard and cancel the source relocation.
            // No shard is left unassigned
            logger.trace("{} is a relocation target, resolving source to cancel relocation ({})", failedShard, unassignedInfo.shortSummary());
            RoutingNode relocatingFromNode = routingNodes.node(failedShard.relocatingNodeId());
            if (relocatingFromNode != null) {
                for (ShardRouting shardRouting : relocatingFromNode) {
                    if (shardRouting.isRelocationSourceOf(failedShard)) {
                        logger.trace("{}, resolved source to [{}]. canceling relocation ... ({})", failedShard.shardId(), shardRouting, unassignedInfo.shortSummary());
                        routingNodes.cancelRelocation(shardRouting);
                        break;
                    }
                }
            }
        } else {
            // The fail shard is the main copy of the current shard routing. Any
            // relocation will be cancelled (and the target shard removed as well)
            // and the shard copy needs to be marked as unassigned

            if (failedShard.relocatingNodeId() != null) {
                // handle relocation source shards.  we need to find the target initializing shard that is recovering, and remove it...
                assert failedShard.initializing() == false; // should have been dealt with and returned
                assert failedShard.relocating();

                RoutingNodes.RoutingNodeIterator initializingNode = routingNodes.routingNodeIter(failedShard.relocatingNodeId());
                if (initializingNode != null) {
                    while (initializingNode.hasNext()) {
                        ShardRouting shardRouting = initializingNode.next();
                        if (shardRouting.isRelocationTargetOf(failedShard)) {
                            logger.trace("{} is removed due to the failure of the source shard", shardRouting);
                            initializingNode.remove();
                        }
                    }
                }
            }

            matchedNode.moveToUnassigned(unassignedInfo);
        }
        assert matchedNode.isRemoved() : "failedShard " + failedShard + " was matched but wasn't removed";
        return true;
    }

    private RoutingNodes getMutableRoutingNodes(ClusterState clusterState) {
        RoutingNodes routingNodes = new RoutingNodes(clusterState, false); // this is a costly operation - only call this once!
        return routingNodes;
    }

    /** override this to control time based decisions during allocation */
    protected long currentNanoTime() {
        return System.nanoTime();
    }
}
