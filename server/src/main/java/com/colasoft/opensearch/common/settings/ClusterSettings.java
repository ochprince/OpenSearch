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

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.common.settings;

import org.apache.logging.log4j.LogManager;
import com.colasoft.opensearch.action.main.TransportMainAction;
import com.colasoft.opensearch.cluster.routing.allocation.AwarenessReplicaBalance;
import com.colasoft.opensearch.action.search.CreatePitController;
import com.colasoft.opensearch.cluster.routing.allocation.decider.NodeLoadAwareAllocationDecider;
import com.colasoft.opensearch.common.util.FeatureFlags;
import com.colasoft.opensearch.index.IndexModule;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.IndexingPressure;
import com.colasoft.opensearch.index.SegmentReplicationPressureService;
import com.colasoft.opensearch.index.ShardIndexingPressureMemoryManager;
import com.colasoft.opensearch.index.ShardIndexingPressureSettings;
import com.colasoft.opensearch.index.ShardIndexingPressureStore;
import com.colasoft.opensearch.search.backpressure.settings.NodeDuressSettings;
import com.colasoft.opensearch.search.backpressure.settings.SearchBackpressureSettings;
import com.colasoft.opensearch.search.backpressure.settings.SearchShardTaskSettings;
import com.colasoft.opensearch.search.backpressure.settings.SearchTaskSettings;
import com.colasoft.opensearch.tasks.TaskManager;
import com.colasoft.opensearch.tasks.TaskResourceTrackingService;
import com.colasoft.opensearch.tasks.consumer.TopNSearchTasksLogger;
import com.colasoft.opensearch.watcher.ResourceWatcherService;
import com.colasoft.opensearch.action.admin.cluster.configuration.TransportAddVotingConfigExclusionsAction;
import com.colasoft.opensearch.action.admin.indices.close.TransportCloseIndexAction;
import com.colasoft.opensearch.action.search.TransportSearchAction;
import com.colasoft.opensearch.action.support.AutoCreateIndex;
import com.colasoft.opensearch.action.support.DestructiveOperations;
import com.colasoft.opensearch.action.support.replication.TransportReplicationAction;
import com.colasoft.opensearch.bootstrap.BootstrapSettings;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.cluster.ClusterModule;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.cluster.InternalClusterInfoService;
import com.colasoft.opensearch.cluster.NodeConnectionsService;
import com.colasoft.opensearch.cluster.action.index.MappingUpdatedAction;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.coordination.ClusterBootstrapService;
import com.colasoft.opensearch.cluster.coordination.ClusterFormationFailureHelper;
import com.colasoft.opensearch.cluster.coordination.Coordinator;
import com.colasoft.opensearch.cluster.coordination.ElectionSchedulerFactory;
import com.colasoft.opensearch.cluster.coordination.FollowersChecker;
import com.colasoft.opensearch.cluster.coordination.JoinHelper;
import com.colasoft.opensearch.cluster.coordination.LagDetector;
import com.colasoft.opensearch.cluster.coordination.LeaderChecker;
import com.colasoft.opensearch.cluster.coordination.NoClusterManagerBlockService;
import com.colasoft.opensearch.cluster.coordination.Reconfigurator;
import com.colasoft.opensearch.cluster.metadata.IndexGraveyard;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.routing.OperationRouting;
import com.colasoft.opensearch.cluster.routing.allocation.DiskThresholdSettings;
import com.colasoft.opensearch.cluster.routing.allocation.allocator.BalancedShardsAllocator;
import com.colasoft.opensearch.cluster.routing.allocation.decider.AwarenessAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.ClusterRebalanceAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.ConcurrentRebalanceAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.ConcurrentRecoveriesAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.DiskThresholdDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.EnableAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.FilterAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.SameShardAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.ShardsLimitAllocationDecider;
import com.colasoft.opensearch.cluster.routing.allocation.decider.ThrottlingAllocationDecider;
import com.colasoft.opensearch.cluster.service.ClusterApplierService;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskThrottler;
import com.colasoft.opensearch.cluster.service.ClusterManagerService;
import com.colasoft.opensearch.common.logging.Loggers;
import com.colasoft.opensearch.common.network.NetworkModule;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.Setting.Property;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.common.util.concurrent.OpenSearchExecutors;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.discovery.DiscoveryModule;
import com.colasoft.opensearch.discovery.HandshakingTransportAddressConnector;
import com.colasoft.opensearch.discovery.PeerFinder;
import com.colasoft.opensearch.discovery.SeedHostsResolver;
import com.colasoft.opensearch.discovery.SettingsBasedSeedHostsProvider;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.env.NodeEnvironment;
import com.colasoft.opensearch.gateway.DanglingIndicesState;
import com.colasoft.opensearch.gateway.GatewayService;
import com.colasoft.opensearch.gateway.PersistedClusterStateService;
import com.colasoft.opensearch.http.HttpTransportSettings;
import com.colasoft.opensearch.indices.IndexingMemoryController;
import com.colasoft.opensearch.indices.IndicesQueryCache;
import com.colasoft.opensearch.indices.IndicesRequestCache;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.ShardLimitValidator;
import com.colasoft.opensearch.indices.analysis.HunspellService;
import com.colasoft.opensearch.indices.breaker.BreakerSettings;
import com.colasoft.opensearch.indices.breaker.HierarchyCircuitBreakerService;
import com.colasoft.opensearch.indices.fielddata.cache.IndicesFieldDataCache;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.indices.store.IndicesStore;
import com.colasoft.opensearch.monitor.fs.FsHealthService;
import com.colasoft.opensearch.monitor.fs.FsService;
import com.colasoft.opensearch.monitor.jvm.JvmGcMonitorService;
import com.colasoft.opensearch.monitor.jvm.JvmService;
import com.colasoft.opensearch.monitor.os.OsService;
import com.colasoft.opensearch.monitor.process.ProcessService;
import com.colasoft.opensearch.node.Node;
import com.colasoft.opensearch.node.Node.DiscoverySettings;
import com.colasoft.opensearch.node.NodeRoleSettings;
import com.colasoft.opensearch.persistent.PersistentTasksClusterService;
import com.colasoft.opensearch.persistent.decider.EnableAssignmentDecider;
import com.colasoft.opensearch.plugins.PluginsService;
import com.colasoft.opensearch.repositories.fs.FsRepository;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.SearchModule;
import com.colasoft.opensearch.search.SearchService;
import com.colasoft.opensearch.search.aggregations.MultiBucketConsumerService;
import com.colasoft.opensearch.search.fetch.subphase.highlight.FastVectorHighlighter;
import com.colasoft.opensearch.snapshots.InternalSnapshotsInfoService;
import com.colasoft.opensearch.snapshots.SnapshotsService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.ProxyConnectionStrategy;
import com.colasoft.opensearch.transport.RemoteClusterService;
import com.colasoft.opensearch.transport.RemoteConnectionStrategy;
import com.colasoft.opensearch.transport.SniffConnectionStrategy;
import com.colasoft.opensearch.transport.TransportSettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Encapsulates all valid cluster level settings.
 *
 * @opensearch.internal
 */
public final class ClusterSettings extends AbstractScopedSettings {

    public ClusterSettings(final Settings nodeSettings, final Set<Setting<?>> settingsSet) {
        this(nodeSettings, settingsSet, Collections.emptySet());
    }

    public ClusterSettings(final Settings nodeSettings, final Set<Setting<?>> settingsSet, final Set<SettingUpgrader<?>> settingUpgraders) {
        super(nodeSettings, settingsSet, settingUpgraders, Property.NodeScope);
        addSettingsUpdater(new LoggingSettingUpdater(nodeSettings));
    }

    private static final class LoggingSettingUpdater implements SettingUpdater<Settings> {
        final Predicate<String> loggerPredicate = Loggers.LOG_LEVEL_SETTING::match;
        private final Settings settings;

        LoggingSettingUpdater(Settings settings) {
            this.settings = settings;
        }

        @Override
        public boolean hasChanged(Settings current, Settings previous) {
            return current.filter(loggerPredicate).equals(previous.filter(loggerPredicate)) == false;
        }

        @Override
        public Settings getValue(Settings current, Settings previous) {
            Settings.Builder builder = Settings.builder();
            builder.put(current.filter(loggerPredicate));
            for (String key : previous.keySet()) {
                if (loggerPredicate.test(key) && builder.keys().contains(key) == false) {
                    if (Loggers.LOG_LEVEL_SETTING.getConcreteSetting(key).exists(settings) == false) {
                        builder.putNull(key);
                    } else {
                        builder.put(key, Loggers.LOG_LEVEL_SETTING.getConcreteSetting(key).get(settings).toString());
                    }
                }
            }
            return builder.build();
        }

        @Override
        public void apply(Settings value, Settings current, Settings previous) {
            for (String key : value.keySet()) {
                assert loggerPredicate.test(key);
                String component = key.substring("logger.".length());
                if ("level".equals(component)) {
                    continue;
                }
                if ("_root".equals(component)) {
                    final String rootLevel = value.get(key);
                    if (rootLevel == null) {
                        Loggers.setLevel(LogManager.getRootLogger(), Loggers.LOG_DEFAULT_LEVEL_SETTING.get(settings));
                    } else {
                        Loggers.setLevel(LogManager.getRootLogger(), rootLevel);
                    }
                } else {
                    Loggers.setLevel(LogManager.getLogger(component), value.get(key));
                }
            }
        }
    }

    public static Set<Setting<?>> BUILT_IN_CLUSTER_SETTINGS = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                AwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_AWARENESS_ATTRIBUTE_SETTING,
                AwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_AWARENESS_FORCE_GROUP_SETTING,
                AwarenessReplicaBalance.CLUSTER_ROUTING_ALLOCATION_AWARENESS_BALANCE_SETTING,
                BalancedShardsAllocator.INDEX_BALANCE_FACTOR_SETTING,
                BalancedShardsAllocator.SHARD_BALANCE_FACTOR_SETTING,
                BalancedShardsAllocator.PREFER_PRIMARY_SHARD_BALANCE,
                BalancedShardsAllocator.SHARD_MOVE_PRIMARY_FIRST_SETTING,
                BalancedShardsAllocator.THRESHOLD_SETTING,
                BreakerSettings.CIRCUIT_BREAKER_LIMIT_SETTING,
                BreakerSettings.CIRCUIT_BREAKER_OVERHEAD_SETTING,
                BreakerSettings.CIRCUIT_BREAKER_TYPE,
                ClusterRebalanceAllocationDecider.CLUSTER_ROUTING_ALLOCATION_ALLOW_REBALANCE_SETTING,
                ConcurrentRebalanceAllocationDecider.CLUSTER_ROUTING_ALLOCATION_CLUSTER_CONCURRENT_REBALANCE_SETTING,
                ConcurrentRecoveriesAllocationDecider.CLUSTER_ROUTING_ALLOCATION_CLUSTER_CONCURRENT_RECOVERIES_SETTING,
                DanglingIndicesState.AUTO_IMPORT_DANGLING_INDICES_SETTING,
                EnableAllocationDecider.CLUSTER_ROUTING_ALLOCATION_ENABLE_SETTING,
                EnableAllocationDecider.CLUSTER_ROUTING_REBALANCE_ENABLE_SETTING,
                FilterAllocationDecider.CLUSTER_ROUTING_INCLUDE_GROUP_SETTING,
                FilterAllocationDecider.CLUSTER_ROUTING_EXCLUDE_GROUP_SETTING,
                FilterAllocationDecider.CLUSTER_ROUTING_REQUIRE_GROUP_SETTING,
                FsRepository.REPOSITORIES_CHUNK_SIZE_SETTING,
                FsRepository.REPOSITORIES_COMPRESS_SETTING,
                FsRepository.REPOSITORIES_LOCATION_SETTING,
                IndicesQueryCache.INDICES_CACHE_QUERY_SIZE_SETTING,
                IndicesQueryCache.INDICES_CACHE_QUERY_COUNT_SETTING,
                IndicesQueryCache.INDICES_QUERIES_CACHE_ALL_SEGMENTS_SETTING,
                IndicesService.INDICES_ID_FIELD_DATA_ENABLED_SETTING,
                IndicesService.WRITE_DANGLING_INDICES_INFO_SETTING,
                MappingUpdatedAction.INDICES_MAPPING_DYNAMIC_TIMEOUT_SETTING,
                MappingUpdatedAction.INDICES_MAX_IN_FLIGHT_UPDATES_SETTING,
                Metadata.SETTING_READ_ONLY_SETTING,
                Metadata.SETTING_READ_ONLY_ALLOW_DELETE_SETTING,
                Metadata.DEFAULT_REPLICA_COUNT_SETTING,
                Metadata.SETTING_CREATE_INDEX_BLOCK_SETTING,
                ShardLimitValidator.SETTING_CLUSTER_MAX_SHARDS_PER_NODE,
                ShardLimitValidator.SETTING_CLUSTER_MAX_SHARDS_PER_CLUSTER,
                ShardLimitValidator.SETTING_CLUSTER_IGNORE_DOT_INDEXES,
                RecoverySettings.INDICES_RECOVERY_MAX_BYTES_PER_SEC_SETTING,
                RecoverySettings.INDICES_RECOVERY_RETRY_DELAY_STATE_SYNC_SETTING,
                RecoverySettings.INDICES_RECOVERY_RETRY_DELAY_NETWORK_SETTING,
                RecoverySettings.INDICES_RECOVERY_ACTIVITY_TIMEOUT_SETTING,
                RecoverySettings.INDICES_RECOVERY_INTERNAL_ACTION_TIMEOUT_SETTING,
                RecoverySettings.INDICES_RECOVERY_INTERNAL_LONG_ACTION_TIMEOUT_SETTING,
                RecoverySettings.INDICES_RECOVERY_MAX_CONCURRENT_FILE_CHUNKS_SETTING,
                RecoverySettings.INDICES_RECOVERY_MAX_CONCURRENT_OPERATIONS_SETTING,
                ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_INITIAL_PRIMARIES_RECOVERIES_SETTING,
                ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_INITIAL_REPLICAS_RECOVERIES_SETTING,
                ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_INCOMING_RECOVERIES_SETTING,
                ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_OUTGOING_RECOVERIES_SETTING,
                ThrottlingAllocationDecider.CLUSTER_ROUTING_ALLOCATION_NODE_CONCURRENT_RECOVERIES_SETTING,
                DiskThresholdDecider.ENABLE_FOR_SINGLE_DATA_NODE,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_LOW_DISK_WATERMARK_SETTING,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_HIGH_DISK_WATERMARK_SETTING,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_DISK_FLOOD_STAGE_WATERMARK_SETTING,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_DISK_THRESHOLD_ENABLED_SETTING,
                DiskThresholdSettings.CLUSTER_CREATE_INDEX_BLOCK_AUTO_RELEASE,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_INCLUDE_RELOCATIONS_SETTING,
                DiskThresholdSettings.CLUSTER_ROUTING_ALLOCATION_REROUTE_INTERVAL_SETTING,
                SameShardAllocationDecider.CLUSTER_ROUTING_ALLOCATION_SAME_HOST_SETTING,
                ShardStateAction.FOLLOW_UP_REROUTE_PRIORITY_SETTING,
                InternalClusterInfoService.INTERNAL_CLUSTER_INFO_UPDATE_INTERVAL_SETTING,
                InternalClusterInfoService.INTERNAL_CLUSTER_INFO_TIMEOUT_SETTING,
                InternalSnapshotsInfoService.INTERNAL_SNAPSHOT_INFO_MAX_CONCURRENT_FETCHES_SETTING,
                DestructiveOperations.REQUIRES_NAME_SETTING,
                NoClusterManagerBlockService.NO_MASTER_BLOCK_SETTING,  // deprecated
                NoClusterManagerBlockService.NO_CLUSTER_MANAGER_BLOCK_SETTING,
                GatewayService.EXPECTED_DATA_NODES_SETTING,
                GatewayService.EXPECTED_MASTER_NODES_SETTING,
                GatewayService.EXPECTED_NODES_SETTING,
                GatewayService.RECOVER_AFTER_DATA_NODES_SETTING,
                GatewayService.RECOVER_AFTER_MASTER_NODES_SETTING,
                GatewayService.RECOVER_AFTER_NODES_SETTING,
                GatewayService.RECOVER_AFTER_TIME_SETTING,
                PersistedClusterStateService.SLOW_WRITE_LOGGING_THRESHOLD,
                NetworkModule.HTTP_DEFAULT_TYPE_SETTING,
                NetworkModule.TRANSPORT_DEFAULT_TYPE_SETTING,
                NetworkModule.HTTP_TYPE_SETTING,
                NetworkModule.TRANSPORT_TYPE_SETTING,
                HttpTransportSettings.SETTING_CORS_ALLOW_CREDENTIALS,
                HttpTransportSettings.SETTING_CORS_ENABLED,
                HttpTransportSettings.SETTING_CORS_MAX_AGE,
                HttpTransportSettings.SETTING_HTTP_DETAILED_ERRORS_ENABLED,
                HttpTransportSettings.SETTING_CORS_ALLOW_ORIGIN,
                HttpTransportSettings.SETTING_HTTP_HOST,
                HttpTransportSettings.SETTING_HTTP_PUBLISH_HOST,
                HttpTransportSettings.SETTING_HTTP_BIND_HOST,
                HttpTransportSettings.SETTING_HTTP_PORT,
                HttpTransportSettings.SETTING_HTTP_PUBLISH_PORT,
                HttpTransportSettings.SETTING_PIPELINING_MAX_EVENTS,
                HttpTransportSettings.SETTING_HTTP_COMPRESSION,
                HttpTransportSettings.SETTING_HTTP_COMPRESSION_LEVEL,
                HttpTransportSettings.SETTING_CORS_ALLOW_METHODS,
                HttpTransportSettings.SETTING_CORS_ALLOW_HEADERS,
                HttpTransportSettings.SETTING_HTTP_DETAILED_ERRORS_ENABLED,
                HttpTransportSettings.SETTING_HTTP_CONTENT_TYPE_REQUIRED,
                HttpTransportSettings.SETTING_HTTP_MAX_CONTENT_LENGTH,
                HttpTransportSettings.SETTING_HTTP_MAX_CHUNK_SIZE,
                HttpTransportSettings.SETTING_HTTP_MAX_HEADER_SIZE,
                HttpTransportSettings.SETTING_HTTP_MAX_WARNING_HEADER_COUNT,
                HttpTransportSettings.SETTING_HTTP_MAX_WARNING_HEADER_SIZE,
                HttpTransportSettings.SETTING_HTTP_MAX_INITIAL_LINE_LENGTH,
                HttpTransportSettings.SETTING_HTTP_READ_TIMEOUT,
                HttpTransportSettings.SETTING_HTTP_RESET_COOKIES,
                HttpTransportSettings.OLD_SETTING_HTTP_TCP_NO_DELAY,
                HttpTransportSettings.SETTING_HTTP_TCP_NO_DELAY,
                HttpTransportSettings.SETTING_HTTP_TCP_KEEP_ALIVE,
                HttpTransportSettings.SETTING_HTTP_TCP_KEEP_IDLE,
                HttpTransportSettings.SETTING_HTTP_TCP_KEEP_INTERVAL,
                HttpTransportSettings.SETTING_HTTP_TCP_KEEP_COUNT,
                HttpTransportSettings.SETTING_HTTP_TCP_REUSE_ADDRESS,
                HttpTransportSettings.SETTING_HTTP_TCP_SEND_BUFFER_SIZE,
                HttpTransportSettings.SETTING_HTTP_TCP_RECEIVE_BUFFER_SIZE,
                HttpTransportSettings.SETTING_HTTP_TRACE_LOG_INCLUDE,
                HttpTransportSettings.SETTING_HTTP_TRACE_LOG_EXCLUDE,
                HierarchyCircuitBreakerService.USE_REAL_MEMORY_USAGE_SETTING,
                HierarchyCircuitBreakerService.TOTAL_CIRCUIT_BREAKER_LIMIT_SETTING,
                HierarchyCircuitBreakerService.FIELDDATA_CIRCUIT_BREAKER_LIMIT_SETTING,
                HierarchyCircuitBreakerService.FIELDDATA_CIRCUIT_BREAKER_OVERHEAD_SETTING,
                HierarchyCircuitBreakerService.IN_FLIGHT_REQUESTS_CIRCUIT_BREAKER_LIMIT_SETTING,
                HierarchyCircuitBreakerService.IN_FLIGHT_REQUESTS_CIRCUIT_BREAKER_OVERHEAD_SETTING,
                HierarchyCircuitBreakerService.REQUEST_CIRCUIT_BREAKER_LIMIT_SETTING,
                HierarchyCircuitBreakerService.REQUEST_CIRCUIT_BREAKER_OVERHEAD_SETTING,
                IndexModule.NODE_STORE_ALLOW_MMAP,
                ClusterApplierService.CLUSTER_SERVICE_SLOW_TASK_LOGGING_THRESHOLD_SETTING,
                ClusterService.USER_DEFINED_METADATA,
                ClusterManagerService.MASTER_SERVICE_SLOW_TASK_LOGGING_THRESHOLD_SETTING,  // deprecated
                ClusterManagerService.CLUSTER_MANAGER_SERVICE_SLOW_TASK_LOGGING_THRESHOLD_SETTING,
                SearchService.DEFAULT_SEARCH_TIMEOUT_SETTING,
                SearchService.DEFAULT_ALLOW_PARTIAL_SEARCH_RESULTS,
                TransportSearchAction.SHARD_COUNT_LIMIT_SETTING,
                TransportSearchAction.SEARCH_CANCEL_AFTER_TIME_INTERVAL_SETTING,
                RemoteClusterService.REMOTE_CLUSTER_SKIP_UNAVAILABLE,
                SniffConnectionStrategy.REMOTE_CONNECTIONS_PER_CLUSTER,
                RemoteClusterService.REMOTE_INITIAL_CONNECTION_TIMEOUT_SETTING,
                RemoteClusterService.REMOTE_NODE_ATTRIBUTE,
                RemoteClusterService.ENABLE_REMOTE_CLUSTERS,
                RemoteClusterService.REMOTE_CLUSTER_PING_SCHEDULE,
                RemoteClusterService.REMOTE_CLUSTER_COMPRESS,
                RemoteConnectionStrategy.REMOTE_CONNECTION_MODE,
                ProxyConnectionStrategy.PROXY_ADDRESS,
                ProxyConnectionStrategy.REMOTE_SOCKET_CONNECTIONS,
                ProxyConnectionStrategy.SERVER_NAME,
                ProxyConnectionStrategy.SERVER_NAME,
                SniffConnectionStrategy.REMOTE_CLUSTERS_PROXY,
                SniffConnectionStrategy.REMOTE_CLUSTER_SEEDS,
                SniffConnectionStrategy.REMOTE_NODE_CONNECTIONS,
                TransportCloseIndexAction.CLUSTER_INDICES_CLOSE_ENABLE_SETTING,
                ShardsLimitAllocationDecider.CLUSTER_TOTAL_SHARDS_PER_NODE_SETTING,
                NodeConnectionsService.CLUSTER_NODE_RECONNECT_INTERVAL_SETTING,
                HierarchyCircuitBreakerService.FIELDDATA_CIRCUIT_BREAKER_TYPE_SETTING,
                HierarchyCircuitBreakerService.REQUEST_CIRCUIT_BREAKER_TYPE_SETTING,
                TransportReplicationAction.REPLICATION_INITIAL_RETRY_BACKOFF_BOUND,
                TransportReplicationAction.REPLICATION_RETRY_TIMEOUT,
                TransportSettings.HOST,
                TransportSettings.PUBLISH_HOST,
                TransportSettings.PUBLISH_HOST_PROFILE,
                TransportSettings.BIND_HOST,
                TransportSettings.BIND_HOST_PROFILE,
                TransportSettings.OLD_PORT,
                TransportSettings.PORT,
                TransportSettings.PORT_PROFILE,
                TransportSettings.PUBLISH_PORT,
                TransportSettings.PUBLISH_PORT_PROFILE,
                TransportSettings.OLD_TRANSPORT_COMPRESS,
                TransportSettings.TRANSPORT_COMPRESS,
                TransportSettings.PING_SCHEDULE,
                TransportSettings.TCP_CONNECT_TIMEOUT,
                TransportSettings.CONNECT_TIMEOUT,
                TransportSettings.DEFAULT_FEATURES_SETTING,
                TransportSettings.OLD_TCP_NO_DELAY,
                TransportSettings.TCP_NO_DELAY,
                TransportSettings.OLD_TCP_NO_DELAY_PROFILE,
                TransportSettings.TCP_NO_DELAY_PROFILE,
                TransportSettings.TCP_KEEP_ALIVE,
                TransportSettings.OLD_TCP_KEEP_ALIVE_PROFILE,
                TransportSettings.TCP_KEEP_ALIVE_PROFILE,
                TransportSettings.TCP_KEEP_IDLE,
                TransportSettings.TCP_KEEP_IDLE_PROFILE,
                TransportSettings.TCP_KEEP_INTERVAL,
                TransportSettings.TCP_KEEP_INTERVAL_PROFILE,
                TransportSettings.TCP_KEEP_COUNT,
                TransportSettings.TCP_KEEP_COUNT_PROFILE,
                TransportSettings.TCP_REUSE_ADDRESS,
                TransportSettings.OLD_TCP_REUSE_ADDRESS_PROFILE,
                TransportSettings.TCP_REUSE_ADDRESS_PROFILE,
                TransportSettings.TCP_SEND_BUFFER_SIZE,
                TransportSettings.OLD_TCP_SEND_BUFFER_SIZE_PROFILE,
                TransportSettings.TCP_SEND_BUFFER_SIZE_PROFILE,
                TransportSettings.TCP_RECEIVE_BUFFER_SIZE,
                TransportSettings.OLD_TCP_RECEIVE_BUFFER_SIZE_PROFILE,
                TransportSettings.TCP_RECEIVE_BUFFER_SIZE_PROFILE,
                TransportSettings.CONNECTIONS_PER_NODE_RECOVERY,
                TransportSettings.CONNECTIONS_PER_NODE_BULK,
                TransportSettings.CONNECTIONS_PER_NODE_REG,
                TransportSettings.CONNECTIONS_PER_NODE_STATE,
                TransportSettings.CONNECTIONS_PER_NODE_PING,
                TransportSettings.TRACE_LOG_EXCLUDE_SETTING,
                TransportSettings.TRACE_LOG_INCLUDE_SETTING,
                TransportSettings.SLOW_OPERATION_THRESHOLD_SETTING,
                NetworkService.NETWORK_SERVER,
                NetworkService.GLOBAL_NETWORK_HOST_SETTING,
                NetworkService.GLOBAL_NETWORK_BIND_HOST_SETTING,
                NetworkService.GLOBAL_NETWORK_PUBLISH_HOST_SETTING,
                NetworkService.TCP_NO_DELAY,
                NetworkService.TCP_KEEP_ALIVE,
                NetworkService.TCP_KEEP_IDLE,
                NetworkService.TCP_KEEP_INTERVAL,
                NetworkService.TCP_KEEP_COUNT,
                NetworkService.TCP_REUSE_ADDRESS,
                NetworkService.TCP_SEND_BUFFER_SIZE,
                NetworkService.TCP_RECEIVE_BUFFER_SIZE,
                NetworkService.TCP_CONNECT_TIMEOUT,
                IndexSettings.QUERY_STRING_ANALYZE_WILDCARD,
                IndexSettings.QUERY_STRING_ALLOW_LEADING_WILDCARD,
                ScriptService.SCRIPT_GENERAL_CACHE_SIZE_SETTING,
                ScriptService.SCRIPT_GENERAL_CACHE_EXPIRE_SETTING,
                ScriptService.SCRIPT_GENERAL_MAX_COMPILATIONS_RATE_SETTING,
                ScriptService.SCRIPT_CACHE_SIZE_SETTING,
                ScriptService.SCRIPT_CACHE_EXPIRE_SETTING,
                ScriptService.SCRIPT_DISABLE_MAX_COMPILATIONS_RATE_SETTING,
                ScriptService.SCRIPT_MAX_COMPILATIONS_RATE_SETTING,
                ScriptService.SCRIPT_MAX_SIZE_IN_BYTES,
                ScriptService.TYPES_ALLOWED_SETTING,
                ScriptService.CONTEXTS_ALLOWED_SETTING,
                IndicesService.INDICES_CACHE_CLEAN_INTERVAL_SETTING,
                IndicesFieldDataCache.INDICES_FIELDDATA_CACHE_SIZE_KEY,
                IndicesRequestCache.INDICES_CACHE_QUERY_SIZE,
                IndicesRequestCache.INDICES_CACHE_QUERY_EXPIRE,
                HunspellService.HUNSPELL_LAZY_LOAD,
                HunspellService.HUNSPELL_IGNORE_CASE,
                HunspellService.HUNSPELL_DICTIONARY_OPTIONS,
                IndicesStore.INDICES_STORE_DELETE_SHARD_TIMEOUT,
                Environment.PATH_DATA_SETTING,
                Environment.PATH_HOME_SETTING,
                Environment.PATH_LOGS_SETTING,
                Environment.PATH_REPO_SETTING,
                Environment.PATH_SHARED_DATA_SETTING,
                Environment.PIDFILE_SETTING,
                Environment.NODE_PIDFILE_SETTING,
                NodeEnvironment.NODE_ID_SEED_SETTING,
                DiscoverySettings.INITIAL_STATE_TIMEOUT_SETTING,
                DiscoveryModule.DISCOVERY_TYPE_SETTING,
                DiscoveryModule.DISCOVERY_SEED_PROVIDERS_SETTING,
                DiscoveryModule.LEGACY_DISCOVERY_HOSTS_PROVIDER_SETTING,
                DiscoveryModule.ELECTION_STRATEGY_SETTING,
                SettingsBasedSeedHostsProvider.DISCOVERY_SEED_HOSTS_SETTING,
                SettingsBasedSeedHostsProvider.LEGACY_DISCOVERY_ZEN_PING_UNICAST_HOSTS_SETTING,
                SeedHostsResolver.DISCOVERY_SEED_RESOLVER_MAX_CONCURRENT_RESOLVERS_SETTING,
                SeedHostsResolver.DISCOVERY_SEED_RESOLVER_TIMEOUT_SETTING,
                SeedHostsResolver.LEGACY_DISCOVERY_ZEN_PING_UNICAST_CONCURRENT_CONNECTS_SETTING,
                SeedHostsResolver.LEGACY_DISCOVERY_ZEN_PING_UNICAST_HOSTS_RESOLVE_TIMEOUT,
                SearchService.DEFAULT_KEEPALIVE_SETTING,
                SearchService.KEEPALIVE_INTERVAL_SETTING,
                SearchService.MAX_KEEPALIVE_SETTING,
                SearchService.ALLOW_EXPENSIVE_QUERIES,
                MultiBucketConsumerService.MAX_BUCKET_SETTING,
                SearchService.LOW_LEVEL_CANCELLATION_SETTING,
                SearchService.MAX_OPEN_SCROLL_CONTEXT,
                SearchService.MAX_OPEN_PIT_CONTEXT,
                SearchService.MAX_PIT_KEEPALIVE_SETTING,
                CreatePitController.PIT_INIT_KEEP_ALIVE,
                Node.WRITE_PORTS_FILE_SETTING,
                Node.NODE_NAME_SETTING,
                Node.NODE_ATTRIBUTES,
                Node.NODE_LOCAL_STORAGE_SETTING,
                NodeRoleSettings.NODE_ROLES_SETTING,
                AutoCreateIndex.AUTO_CREATE_INDEX_SETTING,
                BaseRestHandler.MULTI_ALLOW_EXPLICIT_INDEX,
                ClusterName.CLUSTER_NAME_SETTING,
                Client.CLIENT_TYPE_SETTING_S,
                ClusterModule.SHARDS_ALLOCATOR_TYPE_SETTING,
                OpenSearchExecutors.PROCESSORS_SETTING,
                OpenSearchExecutors.NODE_PROCESSORS_SETTING,
                ThreadContext.DEFAULT_HEADERS_SETTING,
                Loggers.LOG_DEFAULT_LEVEL_SETTING,
                Loggers.LOG_LEVEL_SETTING,
                NodeEnvironment.MAX_LOCAL_STORAGE_NODES_SETTING,
                NodeEnvironment.ENABLE_LUCENE_SEGMENT_INFOS_TRACE_SETTING,
                OsService.REFRESH_INTERVAL_SETTING,
                ProcessService.REFRESH_INTERVAL_SETTING,
                JvmService.REFRESH_INTERVAL_SETTING,
                FsService.REFRESH_INTERVAL_SETTING,
                JvmGcMonitorService.ENABLED_SETTING,
                JvmGcMonitorService.REFRESH_INTERVAL_SETTING,
                JvmGcMonitorService.GC_SETTING,
                JvmGcMonitorService.GC_OVERHEAD_WARN_SETTING,
                JvmGcMonitorService.GC_OVERHEAD_INFO_SETTING,
                JvmGcMonitorService.GC_OVERHEAD_DEBUG_SETTING,
                PageCacheRecycler.LIMIT_HEAP_SETTING,
                PageCacheRecycler.WEIGHT_BYTES_SETTING,
                PageCacheRecycler.WEIGHT_INT_SETTING,
                PageCacheRecycler.WEIGHT_LONG_SETTING,
                PageCacheRecycler.WEIGHT_OBJECTS_SETTING,
                PageCacheRecycler.TYPE_SETTING,
                PluginsService.MANDATORY_SETTING,
                BootstrapSettings.SECURITY_FILTER_BAD_DEFAULTS_SETTING,
                BootstrapSettings.MEMORY_LOCK_SETTING,
                BootstrapSettings.SYSTEM_CALL_FILTER_SETTING,
                BootstrapSettings.CTRLHANDLER_SETTING,
                KeyStoreWrapper.SEED_SETTING,
                IndexingMemoryController.INDEX_BUFFER_SIZE_SETTING,
                IndexingMemoryController.MIN_INDEX_BUFFER_SIZE_SETTING,
                IndexingMemoryController.MAX_INDEX_BUFFER_SIZE_SETTING,
                IndexingMemoryController.SHARD_INACTIVE_TIME_SETTING,
                IndexingMemoryController.SHARD_MEMORY_INTERVAL_TIME_SETTING,
                ResourceWatcherService.ENABLED,
                ResourceWatcherService.RELOAD_INTERVAL_HIGH,
                ResourceWatcherService.RELOAD_INTERVAL_MEDIUM,
                ResourceWatcherService.RELOAD_INTERVAL_LOW,
                SearchModule.INDICES_MAX_CLAUSE_COUNT_SETTING,
                ThreadPool.ESTIMATED_TIME_INTERVAL_SETTING,
                FastVectorHighlighter.SETTING_TV_HIGHLIGHT_MULTI_VALUE,
                Node.BREAKER_TYPE_KEY,
                OperationRouting.USE_ADAPTIVE_REPLICA_SELECTION_SETTING,
                OperationRouting.IGNORE_AWARENESS_ATTRIBUTES_SETTING,
                OperationRouting.WEIGHTED_ROUTING_DEFAULT_WEIGHT,
                OperationRouting.WEIGHTED_ROUTING_FAILOPEN_ENABLED,
                OperationRouting.STRICT_WEIGHTED_SHARD_ROUTING_ENABLED,
                OperationRouting.IGNORE_WEIGHTED_SHARD_ROUTING,
                IndexGraveyard.SETTING_MAX_TOMBSTONES,
                PersistentTasksClusterService.CLUSTER_TASKS_ALLOCATION_RECHECK_INTERVAL_SETTING,
                EnableAssignmentDecider.CLUSTER_TASKS_ALLOCATION_ENABLE_SETTING,
                PeerFinder.DISCOVERY_FIND_PEERS_INTERVAL_SETTING,
                PeerFinder.DISCOVERY_FIND_PEERS_INTERVAL_DURING_DECOMMISSION_SETTING,
                PeerFinder.DISCOVERY_REQUEST_PEERS_TIMEOUT_SETTING,
                ClusterFormationFailureHelper.DISCOVERY_CLUSTER_FORMATION_WARNING_TIMEOUT_SETTING,
                ElectionSchedulerFactory.ELECTION_INITIAL_TIMEOUT_SETTING,
                ElectionSchedulerFactory.ELECTION_BACK_OFF_TIME_SETTING,
                ElectionSchedulerFactory.ELECTION_MAX_TIMEOUT_SETTING,
                ElectionSchedulerFactory.ELECTION_DURATION_SETTING,
                Coordinator.PUBLISH_TIMEOUT_SETTING,
                Coordinator.PUBLISH_INFO_TIMEOUT_SETTING,
                JoinHelper.JOIN_TIMEOUT_SETTING,
                FollowersChecker.FOLLOWER_CHECK_TIMEOUT_SETTING,
                FollowersChecker.FOLLOWER_CHECK_INTERVAL_SETTING,
                FollowersChecker.FOLLOWER_CHECK_RETRY_COUNT_SETTING,
                LeaderChecker.LEADER_CHECK_TIMEOUT_SETTING,
                LeaderChecker.LEADER_CHECK_INTERVAL_SETTING,
                LeaderChecker.LEADER_CHECK_RETRY_COUNT_SETTING,
                Reconfigurator.CLUSTER_AUTO_SHRINK_VOTING_CONFIGURATION,
                TransportAddVotingConfigExclusionsAction.MAXIMUM_VOTING_CONFIG_EXCLUSIONS_SETTING,
                ClusterBootstrapService.INITIAL_MASTER_NODES_SETTING,  // deprecated
                ClusterBootstrapService.INITIAL_CLUSTER_MANAGER_NODES_SETTING,
                ClusterBootstrapService.UNCONFIGURED_BOOTSTRAP_TIMEOUT_SETTING,
                LagDetector.CLUSTER_FOLLOWER_LAG_TIMEOUT_SETTING,
                HandshakingTransportAddressConnector.PROBE_CONNECT_TIMEOUT_SETTING,
                HandshakingTransportAddressConnector.PROBE_HANDSHAKE_TIMEOUT_SETTING,
                SnapshotsService.MAX_CONCURRENT_SNAPSHOT_OPERATIONS_SETTING,
                FsHealthService.ENABLED_SETTING,
                FsHealthService.REFRESH_INTERVAL_SETTING,
                FsHealthService.SLOW_PATH_LOGGING_THRESHOLD_SETTING,
                FsHealthService.HEALTHY_TIMEOUT_SETTING,
                TransportMainAction.OVERRIDE_MAIN_RESPONSE_VERSION,
                NodeLoadAwareAllocationDecider.CLUSTER_ROUTING_ALLOCATION_LOAD_AWARENESS_PROVISIONED_CAPACITY_SETTING,
                NodeLoadAwareAllocationDecider.CLUSTER_ROUTING_ALLOCATION_LOAD_AWARENESS_SKEW_FACTOR_SETTING,
                NodeLoadAwareAllocationDecider.CLUSTER_ROUTING_ALLOCATION_LOAD_AWARENESS_ALLOW_UNASSIGNED_PRIMARIES_SETTING,
                NodeLoadAwareAllocationDecider.CLUSTER_ROUTING_ALLOCATION_LOAD_AWARENESS_FLAT_SKEW_SETTING,
                ShardIndexingPressureSettings.SHARD_INDEXING_PRESSURE_ENABLED,
                ShardIndexingPressureSettings.SHARD_INDEXING_PRESSURE_ENFORCED,
                ShardIndexingPressureSettings.REQUEST_SIZE_WINDOW,
                ShardIndexingPressureSettings.SHARD_MIN_LIMIT,
                ShardIndexingPressureStore.MAX_COLD_STORE_SIZE,
                ShardIndexingPressureMemoryManager.LOWER_OPERATING_FACTOR,
                ShardIndexingPressureMemoryManager.OPTIMAL_OPERATING_FACTOR,
                ShardIndexingPressureMemoryManager.UPPER_OPERATING_FACTOR,
                ShardIndexingPressureMemoryManager.NODE_SOFT_LIMIT,
                ShardIndexingPressureMemoryManager.THROUGHPUT_DEGRADATION_LIMITS,
                ShardIndexingPressureMemoryManager.SUCCESSFUL_REQUEST_ELAPSED_TIMEOUT,
                ShardIndexingPressureMemoryManager.MAX_OUTSTANDING_REQUESTS,
                IndexingPressure.MAX_INDEXING_BYTES,
                TaskResourceTrackingService.TASK_RESOURCE_TRACKING_ENABLED,
                TaskManager.TASK_RESOURCE_CONSUMERS_ENABLED,
                TopNSearchTasksLogger.LOG_TOP_QUERIES_SIZE_SETTING,
                TopNSearchTasksLogger.LOG_TOP_QUERIES_FREQUENCY_SETTING,
                ClusterManagerTaskThrottler.THRESHOLD_SETTINGS,
                ClusterManagerTaskThrottler.BASE_DELAY_SETTINGS,
                ClusterManagerTaskThrottler.MAX_DELAY_SETTINGS,
                // Settings related to search backpressure
                SearchBackpressureSettings.SETTING_MODE,

                NodeDuressSettings.SETTING_NUM_SUCCESSIVE_BREACHES,
                NodeDuressSettings.SETTING_CPU_THRESHOLD,
                NodeDuressSettings.SETTING_HEAP_THRESHOLD,
                SearchTaskSettings.SETTING_CANCELLATION_RATIO,
                SearchTaskSettings.SETTING_CANCELLATION_RATE,
                SearchTaskSettings.SETTING_CANCELLATION_BURST,
                SearchTaskSettings.SETTING_HEAP_PERCENT_THRESHOLD,
                SearchTaskSettings.SETTING_HEAP_VARIANCE_THRESHOLD,
                SearchTaskSettings.SETTING_HEAP_MOVING_AVERAGE_WINDOW_SIZE,
                SearchTaskSettings.SETTING_CPU_TIME_MILLIS_THRESHOLD,
                SearchTaskSettings.SETTING_ELAPSED_TIME_MILLIS_THRESHOLD,
                SearchTaskSettings.SETTING_TOTAL_HEAP_PERCENT_THRESHOLD,
                SearchShardTaskSettings.SETTING_CANCELLATION_RATIO,
                SearchShardTaskSettings.SETTING_CANCELLATION_RATE,
                SearchShardTaskSettings.SETTING_CANCELLATION_BURST,
                SearchShardTaskSettings.SETTING_HEAP_PERCENT_THRESHOLD,
                SearchShardTaskSettings.SETTING_HEAP_VARIANCE_THRESHOLD,
                SearchShardTaskSettings.SETTING_HEAP_MOVING_AVERAGE_WINDOW_SIZE,
                SearchShardTaskSettings.SETTING_CPU_TIME_MILLIS_THRESHOLD,
                SearchShardTaskSettings.SETTING_ELAPSED_TIME_MILLIS_THRESHOLD,
                SearchShardTaskSettings.SETTING_TOTAL_HEAP_PERCENT_THRESHOLD,
                SearchBackpressureSettings.SETTING_CANCELLATION_RATIO,  // deprecated
                SearchBackpressureSettings.SETTING_CANCELLATION_RATE,   // deprecated
                SearchBackpressureSettings.SETTING_CANCELLATION_BURST,   // deprecated
                SegmentReplicationPressureService.SEGMENT_REPLICATION_INDEXING_PRESSURE_ENABLED,
                SegmentReplicationPressureService.MAX_INDEXING_CHECKPOINTS,
                SegmentReplicationPressureService.MAX_REPLICATION_TIME_SETTING,
                SegmentReplicationPressureService.MAX_ALLOWED_STALE_SHARDS,

                // Settings related to Searchable Snapshots
                Node.NODE_SEARCH_CACHE_SIZE_SETTING
            )
        )
    );

    public static List<SettingUpgrader<?>> BUILT_IN_SETTING_UPGRADERS = Collections.emptyList();

    /**
     * Map of feature flag name to feature-flagged cluster settings. Once each feature
     * is ready for production release, the feature flag can be removed, and the
     * setting should be moved to {@link #BUILT_IN_CLUSTER_SETTINGS}.
     */
    public static final Map<List<String>, List<Setting>> FEATURE_FLAGGED_CLUSTER_SETTINGS = Map.of(
        List.of(FeatureFlags.SEGMENT_REPLICATION_EXPERIMENTAL),
        List.of(IndicesService.CLUSTER_REPLICATION_TYPE_SETTING),
        List.of(FeatureFlags.REMOTE_STORE),
        List.of(
            IndicesService.CLUSTER_REMOTE_STORE_ENABLED_SETTING,
            IndicesService.CLUSTER_REMOTE_STORE_REPOSITORY_SETTING,
            IndicesService.CLUSTER_REMOTE_TRANSLOG_STORE_ENABLED_SETTING,
            IndicesService.CLUSTER_REMOTE_TRANSLOG_REPOSITORY_SETTING
        )
    );
}
