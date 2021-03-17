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

package org.elasticsearch.indices.cluster;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.ExceptionsHelper;
import org.opensearch.Version;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.admin.cluster.reroute.ClusterRerouteRequest;
import org.opensearch.action.admin.cluster.reroute.TransportClusterRerouteAction;
import org.opensearch.action.admin.indices.close.CloseIndexRequest;
import org.opensearch.action.admin.indices.close.CloseIndexResponse;
import org.opensearch.action.admin.indices.close.TransportCloseIndexAction;
import org.opensearch.action.admin.indices.close.TransportVerifyShardBeforeCloseAction;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.TransportCreateIndexAction;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.delete.TransportDeleteIndexAction;
import org.opensearch.action.admin.indices.open.OpenIndexRequest;
import org.opensearch.action.admin.indices.open.TransportOpenIndexAction;
import org.opensearch.action.admin.indices.readonly.TransportVerifyShardIndexBlockAction;
import org.opensearch.action.admin.indices.settings.put.TransportUpdateSettingsAction;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.DestructiveOperations;
import org.opensearch.action.support.PlainActionFuture;
import org.opensearch.action.support.master.MasterNodeRequest;
import org.opensearch.action.support.master.TransportMasterNodeAction;
import org.opensearch.action.support.master.TransportMasterNodeActionUtils;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ClusterStateTaskExecutor;
import org.opensearch.cluster.ClusterStateTaskExecutor.ClusterTasksResult;
import org.opensearch.cluster.ClusterStateUpdateTask;
import org.opensearch.cluster.EmptyClusterInfoService;
import org.opensearch.cluster.action.shard.ShardStateAction;
import org.opensearch.cluster.action.shard.ShardStateAction.FailedShardEntry;
import org.opensearch.cluster.action.shard.ShardStateAction.StartedShardEntry;
import org.opensearch.cluster.block.ClusterBlock;
import org.opensearch.cluster.coordination.JoinTaskExecutor;
import org.opensearch.cluster.coordination.NodeRemovalClusterStateTaskExecutor;
import org.opensearch.cluster.metadata.AliasValidator;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.MetadataCreateIndexService;
import org.opensearch.cluster.metadata.MetadataDeleteIndexService;
import org.opensearch.cluster.metadata.MetadataIndexStateService;
import org.elasticsearch.cluster.metadata.MetadataIndexStateServiceUtils;
import org.opensearch.cluster.metadata.MetadataIndexUpgradeService;
import org.opensearch.cluster.metadata.MetadataUpdateSettingsService;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.routing.allocation.AllocationService;
import org.opensearch.cluster.routing.allocation.FailedShard;
import org.elasticsearch.cluster.routing.allocation.RandomAllocationDeciderTests;
import org.opensearch.cluster.routing.allocation.allocator.BalancedShardsAllocator;
import org.opensearch.cluster.routing.allocation.decider.AllocationDeciders;
import org.opensearch.cluster.routing.allocation.decider.ReplicaAfterPrimaryActiveAllocationDecider;
import org.opensearch.cluster.routing.allocation.decider.SameShardAllocationDecider;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Priority;
import org.opensearch.common.CheckedFunction;
import org.opensearch.common.UUIDs;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.elasticsearch.env.TestEnvironment;
import org.opensearch.index.Index;
import org.opensearch.index.IndexService;
import org.opensearch.index.mapper.MapperService;
import org.opensearch.index.shard.IndexEventListener;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.ShardLimitValidator;
import org.opensearch.indices.SystemIndices;
import org.opensearch.snapshots.EmptySnapshotsInfoService;
import org.elasticsearch.test.gateway.TestGatewayAllocator;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.carrotsearch.randomizedtesting.RandomizedTest.getRandom;
import static java.util.Collections.emptyMap;
import static org.opensearch.env.Environment.PATH_HOME_SETTING;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClusterStateChanges {
    private static final Settings SETTINGS = Settings.builder()
            .put(PATH_HOME_SETTING.getKey(), "dummy")
            .build();

    private static final Logger logger = LogManager.getLogger(ClusterStateChanges.class);
    private final AllocationService allocationService;
    private final ClusterService clusterService;
    private final ShardStateAction.ShardFailedClusterStateTaskExecutor shardFailedClusterStateTaskExecutor;
    private final ShardStateAction.ShardStartedClusterStateTaskExecutor shardStartedClusterStateTaskExecutor;

    // transport actions
    private final TransportCloseIndexAction transportCloseIndexAction;
    private final TransportOpenIndexAction transportOpenIndexAction;
    private final TransportDeleteIndexAction transportDeleteIndexAction;
    private final TransportUpdateSettingsAction transportUpdateSettingsAction;
    private final TransportClusterRerouteAction transportClusterRerouteAction;
    private final TransportCreateIndexAction transportCreateIndexAction;

    private final NodeRemovalClusterStateTaskExecutor nodeRemovalExecutor;
    private final JoinTaskExecutor joinTaskExecutor;

    public ClusterStateChanges(NamedXContentRegistry xContentRegistry, ThreadPool threadPool) {
        ClusterSettings clusterSettings = new ClusterSettings(SETTINGS, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
        allocationService = new AllocationService(new AllocationDeciders(
            new HashSet<>(Arrays.asList(new SameShardAllocationDecider(SETTINGS, clusterSettings),
                new ReplicaAfterPrimaryActiveAllocationDecider(),
                new RandomAllocationDeciderTests.RandomAllocationDecider(getRandom())))),
            new TestGatewayAllocator(), new BalancedShardsAllocator(SETTINGS),
            EmptyClusterInfoService.INSTANCE, EmptySnapshotsInfoService.INSTANCE);
        shardFailedClusterStateTaskExecutor
            = new ShardStateAction.ShardFailedClusterStateTaskExecutor(allocationService, null, () -> Priority.NORMAL, logger);
        shardStartedClusterStateTaskExecutor
            = new ShardStateAction.ShardStartedClusterStateTaskExecutor(allocationService, null, () -> Priority.NORMAL, logger);
        ActionFilters actionFilters = new ActionFilters(Collections.emptySet());
        IndexNameExpressionResolver indexNameExpressionResolver = new IndexNameExpressionResolver(new ThreadContext(Settings.EMPTY));
        DestructiveOperations destructiveOperations = new DestructiveOperations(SETTINGS, clusterSettings);
        Environment environment = TestEnvironment.newEnvironment(SETTINGS);
        Transport transport = mock(Transport.class); // it's not used

        // mocks
        clusterService = mock(ClusterService.class);
        when(clusterService.getClusterSettings()).thenReturn(clusterSettings);
        IndicesService indicesService = mock(IndicesService.class);
        // MetadataCreateIndexService uses withTempIndexService to check mappings -> fake it here
        try {
            when(indicesService.withTempIndexService(any(IndexMetadata.class), any(CheckedFunction.class)))
                .then(invocationOnMock -> {
                    IndexService indexService = mock(IndexService.class);
                    IndexMetadata indexMetadata = (IndexMetadata) invocationOnMock.getArguments()[0];
                    when(indexService.index()).thenReturn(indexMetadata.getIndex());
                    MapperService mapperService = mock(MapperService.class);
                    when(indexService.mapperService()).thenReturn(mapperService);
                    when(mapperService.documentMapper()).thenReturn(null);
                    when(indexService.getIndexEventListener()).thenReturn(new IndexEventListener() {});
                    when(indexService.getIndexSortSupplier()).thenReturn(() -> null);
                    //noinspection unchecked
                    return ((CheckedFunction) invocationOnMock.getArguments()[1]).apply(indexService);
                });
        } catch (Exception e) {
            /*
             * Catch Exception because Eclipse uses the lower bound for
             * CheckedFunction's exception type so it thinks the "when" call
             * can throw Exception. javac seems to be ok inferring something
             * else.
             */
            throw new IllegalStateException(e);
        }

        // services
        TransportService transportService = new TransportService(SETTINGS, transport, threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> DiscoveryNode.createLocal(SETTINGS, boundAddress.publishAddress(), UUIDs.randomBase64UUID()), clusterSettings,
            Collections.emptySet());
        MetadataIndexUpgradeService metadataIndexUpgradeService = new MetadataIndexUpgradeService(
            SETTINGS,
            xContentRegistry,
            null,
            null,
            null,
            null
        ) {
            // metadata upgrader should do nothing
            @Override
            public IndexMetadata upgradeIndexMetadata(IndexMetadata indexMetadata, Version minimumIndexCompatibilityVersion) {
                return indexMetadata;
            }
        };

        TransportVerifyShardBeforeCloseAction transportVerifyShardBeforeCloseAction = new TransportVerifyShardBeforeCloseAction(SETTINGS,
            transportService, clusterService, indicesService, threadPool, null, actionFilters);
        TransportVerifyShardIndexBlockAction transportVerifyShardIndexBlockAction = new TransportVerifyShardIndexBlockAction(SETTINGS,
            transportService, clusterService, indicesService, threadPool, null, actionFilters);
        ShardLimitValidator shardLimitValidator = new ShardLimitValidator(SETTINGS, clusterService);
        MetadataIndexStateService indexStateService = new MetadataIndexStateService(clusterService, allocationService,
            metadataIndexUpgradeService, indicesService, shardLimitValidator, threadPool, transportVerifyShardBeforeCloseAction,
            transportVerifyShardIndexBlockAction);
        MetadataDeleteIndexService deleteIndexService = new MetadataDeleteIndexService(SETTINGS, clusterService, allocationService);
        MetadataUpdateSettingsService metadataUpdateSettingsService = new MetadataUpdateSettingsService(clusterService,
            allocationService, IndexScopedSettings.DEFAULT_SCOPED_SETTINGS, indicesService, shardLimitValidator, threadPool);
        MetadataCreateIndexService createIndexService = new MetadataCreateIndexService(SETTINGS, clusterService, indicesService,
            allocationService, new AliasValidator(), shardLimitValidator, environment,
            IndexScopedSettings.DEFAULT_SCOPED_SETTINGS, threadPool, xContentRegistry, new SystemIndices(emptyMap()), true);

        transportCloseIndexAction = new TransportCloseIndexAction(SETTINGS, transportService, clusterService, threadPool,
            indexStateService, clusterSettings, actionFilters, indexNameExpressionResolver, destructiveOperations);
        transportOpenIndexAction = new TransportOpenIndexAction(transportService,
            clusterService, threadPool, indexStateService, actionFilters, indexNameExpressionResolver, destructiveOperations);
        transportDeleteIndexAction = new TransportDeleteIndexAction(transportService,
            clusterService, threadPool, deleteIndexService, actionFilters, indexNameExpressionResolver, destructiveOperations);
        transportUpdateSettingsAction = new TransportUpdateSettingsAction(
            transportService, clusterService, threadPool, metadataUpdateSettingsService, actionFilters, indexNameExpressionResolver);
        transportClusterRerouteAction = new TransportClusterRerouteAction(
            transportService, clusterService, threadPool, allocationService, actionFilters, indexNameExpressionResolver);
        transportCreateIndexAction = new TransportCreateIndexAction(
            transportService, clusterService, threadPool, createIndexService, actionFilters, indexNameExpressionResolver);

        nodeRemovalExecutor = new NodeRemovalClusterStateTaskExecutor(allocationService, logger);
        joinTaskExecutor = new JoinTaskExecutor(Settings.EMPTY, allocationService, logger, (s, p, r) -> {});
    }

    public ClusterState createIndex(ClusterState state, CreateIndexRequest request) {
        return execute(transportCreateIndexAction, request, state);
    }

    public ClusterState closeIndices(ClusterState state, CloseIndexRequest request) {
        final Index[] concreteIndices = Arrays.stream(request.indices())
            .map(index -> state.metadata().index(index).getIndex()).toArray(Index[]::new);

        final Map<Index, ClusterBlock> blockedIndices = new HashMap<>();
        ClusterState newState = MetadataIndexStateServiceUtils.addIndexClosedBlocks(concreteIndices, blockedIndices, state);

        newState = MetadataIndexStateServiceUtils.closeRoutingTable(newState, blockedIndices,
            blockedIndices.keySet().stream().collect(Collectors.toMap(Function.identity(), CloseIndexResponse.IndexResult::new)));
        return allocationService.reroute(newState, "indices closed");
    }

    public ClusterState openIndices(ClusterState state, OpenIndexRequest request) {
        return execute(transportOpenIndexAction, request, state);
    }

    public ClusterState deleteIndices(ClusterState state, DeleteIndexRequest request) {
        return execute(transportDeleteIndexAction, request, state);
    }

    public ClusterState updateSettings(ClusterState state, UpdateSettingsRequest request) {
        return execute(transportUpdateSettingsAction, request, state);
    }

    public ClusterState reroute(ClusterState state, ClusterRerouteRequest request) {
        return execute(transportClusterRerouteAction, request, state);
    }

    public ClusterState addNodes(ClusterState clusterState, List<DiscoveryNode> nodes) {
        return runTasks(joinTaskExecutor, clusterState, nodes.stream().map(node -> new JoinTaskExecutor.Task(node, "dummy reason"))
            .collect(Collectors.toList()));
    }

    public ClusterState joinNodesAndBecomeMaster(ClusterState clusterState, List<DiscoveryNode> nodes) {
        List<JoinTaskExecutor.Task> joinNodes = new ArrayList<>();
        joinNodes.add(JoinTaskExecutor.newBecomeMasterTask());
        joinNodes.add(JoinTaskExecutor.newFinishElectionTask());
        joinNodes.addAll(nodes.stream().map(node -> new JoinTaskExecutor.Task(node, "dummy reason"))
            .collect(Collectors.toList()));

        return runTasks(joinTaskExecutor, clusterState, joinNodes);
    }

    public ClusterState removeNodes(ClusterState clusterState, List<DiscoveryNode> nodes) {
        return runTasks(nodeRemovalExecutor, clusterState, nodes.stream()
            .map(n -> new NodeRemovalClusterStateTaskExecutor.Task(n, "dummy reason")).collect(Collectors.toList()));
    }

    public ClusterState applyFailedShards(ClusterState clusterState, List<FailedShard> failedShards) {
        List<FailedShardEntry> entries = failedShards.stream().map(failedShard ->
            new FailedShardEntry(failedShard.getRoutingEntry().shardId(), failedShard.getRoutingEntry().allocationId().getId(),
                0L, failedShard.getMessage(), failedShard.getFailure(), failedShard.markAsStale()))
            .collect(Collectors.toList());
        return runTasks(shardFailedClusterStateTaskExecutor, clusterState, entries);
    }

    public ClusterState applyStartedShards(ClusterState clusterState, List<ShardRouting> startedShards) {
        final Map<ShardRouting, Long> entries = startedShards.stream()
            .collect(Collectors.toMap(Function.identity(), startedShard -> {
                final IndexMetadata indexMetadata = clusterState.metadata().index(startedShard.shardId().getIndex());
                return indexMetadata != null ? indexMetadata.primaryTerm(startedShard.shardId().id()) : 0L;
            }));
        return applyStartedShards(clusterState, entries);
    }

    public ClusterState applyStartedShards(ClusterState clusterState, Map<ShardRouting, Long> startedShards) {
        return runTasks(shardStartedClusterStateTaskExecutor, clusterState, startedShards.entrySet().stream()
            .map(e -> new StartedShardEntry(e.getKey().shardId(), e.getKey().allocationId().getId(), e.getValue(), "shard started"))
            .collect(Collectors.toList()));
    }

    private <T> ClusterState runTasks(ClusterStateTaskExecutor<T> executor, ClusterState clusterState, List<T> entries) {
        try {
            ClusterTasksResult<T> result = executor.execute(clusterState, entries);
            for (ClusterStateTaskExecutor.TaskResult taskResult : result.executionResults.values()) {
                if (taskResult.isSuccess() == false) {
                    throw taskResult.getFailure();
                }
            }
            return result.resultingState;
        } catch (Exception e) {
            throw ExceptionsHelper.convertToRuntime(e);
        }
    }

    private <Request extends MasterNodeRequest<Request>, Response extends ActionResponse> ClusterState execute(
        TransportMasterNodeAction<Request, Response> masterNodeAction, Request request, ClusterState clusterState) {
        return executeClusterStateUpdateTask(clusterState, () -> {
            try {
                TransportMasterNodeActionUtils.runMasterOperation(masterNodeAction, request, clusterState, new PlainActionFuture<>());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private ClusterState executeClusterStateUpdateTask(ClusterState state, Runnable runnable) {
        ClusterState[] result = new ClusterState[1];
        doAnswer(invocationOnMock -> {
            ClusterStateUpdateTask task = (ClusterStateUpdateTask)invocationOnMock.getArguments()[1];
            result[0] = task.execute(state);
            return null;
        }).when(clusterService).submitStateUpdateTask(anyString(), any(ClusterStateUpdateTask.class));
        runnable.run();
        assertThat(result[0], notNullValue());
        return result[0];
    }
}
