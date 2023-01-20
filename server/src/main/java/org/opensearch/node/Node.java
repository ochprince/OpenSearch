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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.util.Constants;
import org.opensearch.common.SetOnce;
import org.opensearch.cluster.routing.allocation.AwarenessReplicaBalance;
import org.opensearch.index.IndexModule;
import org.opensearch.index.IndexingPressureService;
import org.opensearch.tasks.TaskResourceTrackingService;
import org.opensearch.threadpool.RunnableTaskExecutionListener;
import org.opensearch.common.util.FeatureFlags;
import org.opensearch.indices.replication.SegmentReplicationSourceFactory;
import org.opensearch.indices.replication.SegmentReplicationTargetService;
import org.opensearch.indices.replication.SegmentReplicationSourceService;
import org.opensearch.extensions.ExtensionsManager;
import org.opensearch.extensions.NoopExtensionsManager;
import org.opensearch.search.backpressure.SearchBackpressureService;
import org.opensearch.search.backpressure.settings.SearchBackpressureSettings;
import org.opensearch.tasks.TaskResourceTrackingService;
import org.opensearch.threadpool.RunnableTaskExecutionListener;
import org.opensearch.index.store.RemoteSegmentStoreDirectoryFactory;
import org.opensearch.watcher.ResourceWatcherService;
import org.opensearch.Assertions;
import org.opensearch.Build;
import org.opensearch.OpenSearchException;
import org.opensearch.OpenSearchTimeoutException;
import org.opensearch.Version;
import org.opensearch.action.ActionModule;
import org.opensearch.action.ActionType;
import org.opensearch.action.admin.cluster.snapshots.status.TransportNodesSnapshotsStatus;
import org.opensearch.action.search.SearchExecutionStatsCollector;
import org.opensearch.action.search.SearchPhaseController;
import org.opensearch.action.search.SearchTransportService;
import org.opensearch.action.support.TransportAction;
import org.opensearch.action.update.UpdateHelper;
import org.opensearch.bootstrap.BootstrapCheck;
import org.opensearch.bootstrap.BootstrapContext;
import org.opensearch.client.Client;
import org.opensearch.client.node.NodeClient;
import org.opensearch.cluster.ClusterInfoService;
import org.opensearch.cluster.ClusterModule;
import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ClusterStateObserver;
import org.opensearch.cluster.InternalClusterInfoService;
import org.opensearch.cluster.NodeConnectionsService;
import org.opensearch.cluster.action.index.MappingUpdatedAction;
import org.opensearch.cluster.metadata.AliasValidator;
import org.opensearch.cluster.metadata.IndexTemplateMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.metadata.MetadataCreateDataStreamService;
import org.opensearch.cluster.metadata.MetadataCreateIndexService;
import org.opensearch.cluster.metadata.MetadataIndexUpgradeService;
import org.opensearch.cluster.metadata.SystemIndexMetadataUpgradeService;
import org.opensearch.cluster.metadata.TemplateUpgradeService;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.node.DiscoveryNodeRole;
import org.opensearch.cluster.routing.BatchedRerouteService;
import org.opensearch.cluster.routing.RerouteService;
import org.opensearch.cluster.routing.allocation.DiskThresholdMonitor;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.StopWatch;
import org.opensearch.common.breaker.CircuitBreaker;
import org.opensearch.common.component.Lifecycle;
import org.opensearch.common.component.LifecycleComponent;
import org.opensearch.common.inject.Injector;
import org.opensearch.common.inject.Key;
import org.opensearch.common.inject.Module;
import org.opensearch.common.inject.ModulesBuilder;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.lease.Releasables;
import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.common.logging.HeaderWarning;
import org.opensearch.common.logging.NodeAndClusterIdStateListener;
import org.opensearch.common.network.NetworkAddress;
import org.opensearch.common.network.NetworkModule;
import org.opensearch.common.network.NetworkService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.ConsistentSettingsService;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.settings.SettingUpgrader;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsModule;
import org.opensearch.common.transport.BoundTransportAddress;
import org.opensearch.common.transport.TransportAddress;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.util.BigArrays;
import org.opensearch.common.util.PageCacheRecycler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.core.internal.io.IOUtils;
import org.opensearch.discovery.Discovery;
import org.opensearch.discovery.DiscoveryModule;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.env.NodeMetadata;
import org.opensearch.gateway.GatewayAllocator;
import org.opensearch.gateway.GatewayMetaState;
import org.opensearch.gateway.GatewayModule;
import org.opensearch.gateway.GatewayService;
import org.opensearch.gateway.MetaStateService;
import org.opensearch.gateway.PersistedClusterStateService;
import org.opensearch.http.HttpServerTransport;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AnalysisRegistry;
import org.opensearch.index.engine.EngineFactory;
import org.opensearch.indices.IndicesModule;
import org.opensearch.indices.IndicesService;
import org.opensearch.indices.ShardLimitValidator;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.indices.SystemIndices;
import org.opensearch.indices.analysis.AnalysisModule;
import org.opensearch.indices.breaker.BreakerSettings;
import org.opensearch.indices.breaker.CircuitBreakerService;
import org.opensearch.indices.breaker.HierarchyCircuitBreakerService;
import org.opensearch.indices.breaker.NoneCircuitBreakerService;
import org.opensearch.indices.cluster.IndicesClusterStateService;
import org.opensearch.indices.recovery.PeerRecoverySourceService;
import org.opensearch.indices.recovery.PeerRecoveryTargetService;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.indices.store.IndicesStore;
import org.opensearch.ingest.IngestService;
import org.opensearch.monitor.MonitorService;
import org.opensearch.monitor.fs.FsHealthService;
import org.opensearch.monitor.jvm.JvmInfo;
import org.opensearch.persistent.PersistentTasksClusterService;
import org.opensearch.persistent.PersistentTasksExecutor;
import org.opensearch.persistent.PersistentTasksExecutorRegistry;
import org.opensearch.persistent.PersistentTasksService;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.CircuitBreakerPlugin;
import org.opensearch.plugins.ClusterPlugin;
import org.opensearch.plugins.DiscoveryPlugin;
import org.opensearch.plugins.EnginePlugin;
import org.opensearch.plugins.IndexStorePlugin;
import org.opensearch.plugins.IngestPlugin;
import org.opensearch.plugins.MapperPlugin;
import org.opensearch.plugins.MetadataUpgrader;
import org.opensearch.plugins.NetworkPlugin;
import org.opensearch.plugins.PersistentTaskPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.PluginsService;
import org.opensearch.plugins.RepositoryPlugin;
import org.opensearch.plugins.ScriptPlugin;
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.plugins.SystemIndexPlugin;
import org.opensearch.repositories.RepositoriesModule;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.ScriptModule;
import org.opensearch.script.ScriptService;
import org.opensearch.search.SearchModule;
import org.opensearch.search.SearchService;
import org.opensearch.search.aggregations.support.AggregationUsageService;
import org.opensearch.search.fetch.FetchPhase;
import org.opensearch.search.query.QueryPhase;
import org.opensearch.snapshots.InternalSnapshotsInfoService;
import org.opensearch.snapshots.RestoreService;
import org.opensearch.snapshots.SnapshotShardsService;
import org.opensearch.snapshots.SnapshotsInfoService;
import org.opensearch.snapshots.SnapshotsService;
import org.opensearch.tasks.Task;
import org.opensearch.tasks.TaskCancellationService;
import org.opensearch.tasks.TaskResultsService;
import org.opensearch.threadpool.ExecutorBuilder;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.RemoteClusterService;
import org.opensearch.transport.Transport;
import org.opensearch.transport.TransportInterceptor;
import org.opensearch.transport.TransportService;
import org.opensearch.usage.UsageService;

import javax.net.ssl.SNIHostName;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.opensearch.common.util.FeatureFlags.REPLICATION_TYPE;
import static org.opensearch.index.ShardIndexingPressureSettings.SHARD_INDEXING_PRESSURE_ENABLED_ATTRIBUTE_KEY;

/**
 * A node represent a node within a cluster ({@code cluster.name}). The {@link #client()} can be used
 * in order to use a {@link Client} to perform actions/operations against the cluster.
 *
 * @opensearch.internal
 */
public class Node implements Closeable {
    public static final Setting<Boolean> WRITE_PORTS_FILE_SETTING = Setting.boolSetting("node.portsfile", false, Property.NodeScope);
    private static final Setting<Boolean> NODE_DATA_SETTING = Setting.boolSetting(
        "node.data",
        true,
        Property.Deprecated,
        Property.NodeScope
    );
    private static final Setting<Boolean> NODE_MASTER_SETTING = Setting.boolSetting(
        "node.master",
        true,
        Property.Deprecated,
        Property.NodeScope
    );
    private static final Setting<Boolean> NODE_INGEST_SETTING = Setting.boolSetting(
        "node.ingest",
        true,
        Property.Deprecated,
        Property.NodeScope
    );
    private static final Setting<Boolean> NODE_REMOTE_CLUSTER_CLIENT = Setting.boolSetting(
        "node.remote_cluster_client",
        RemoteClusterService.ENABLE_REMOTE_CLUSTERS,
        Property.Deprecated,
        Property.NodeScope
    );

    /**
    * controls whether the node is allowed to persist things like metadata to disk
    * Note that this does not control whether the node stores actual indices (see
    * {@link #NODE_DATA_SETTING}). However, if this is false, {@link #NODE_DATA_SETTING}
    * and {@link #NODE_MASTER_SETTING} must also be false.
    *
    */
    public static final Setting<Boolean> NODE_LOCAL_STORAGE_SETTING = Setting.boolSetting(
        "node.local_storage",
        true,
        Property.Deprecated,
        Property.NodeScope
    );
    public static final Setting<String> NODE_NAME_SETTING = Setting.simpleString("node.name", Property.NodeScope);
    public static final Setting.AffixSetting<String> NODE_ATTRIBUTES = Setting.prefixKeySetting(
        "node.attr.",
        (key) -> new Setting<>(key, "", (value) -> {
            if (value.length() > 0
                && (Character.isWhitespace(value.charAt(0)) || Character.isWhitespace(value.charAt(value.length() - 1)))) {
                throw new IllegalArgumentException(key + " cannot have leading or trailing whitespace " + "[" + value + "]");
            }
            if (value.length() > 0 && "node.attr.server_name".equals(key)) {
                try {
                    new SNIHostName(value);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("invalid node.attr.server_name [" + value + "]", e);
                }
            }
            return value;
        }, Property.NodeScope)
    );
    public static final Setting<String> BREAKER_TYPE_KEY = new Setting<>("indices.breaker.type", "hierarchy", (s) -> {
        switch (s) {
            case "hierarchy":
            case "none":
                return s;
            default:
                throw new IllegalArgumentException("indices.breaker.type must be one of [hierarchy, none] but was: " + s);
        }
    }, Setting.Property.NodeScope);

    private static final String CLIENT_TYPE = "node";

    /**
     * The discovery settings for the node.
     *
     * @opensearch.internal
     */
    public static class DiscoverySettings {
        public static final Setting<TimeValue> INITIAL_STATE_TIMEOUT_SETTING = Setting.positiveTimeSetting(
            "discovery.initial_state_timeout",
            TimeValue.timeValueSeconds(30),
            Property.NodeScope
        );
    }

    private final Lifecycle lifecycle = new Lifecycle();

    /**
     * This logger instance is an instance field as opposed to a static field. This ensures that the field is not
     * initialized until an instance of Node is constructed, which is sure to happen after the logging infrastructure
     * has been initialized to include the hostname. If this field were static, then it would be initialized when the
     * class initializer runs. Alas, this happens too early, before logging is initialized as this class is referred to
     * in InternalSettingsPreparer#finalizeSettings, which runs when creating the Environment, before logging is
     * initialized.
     */
    private final Logger logger = LogManager.getLogger(Node.class);
    private final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(Node.class);
    private final Injector injector;
    private final Environment environment;
    private final NodeEnvironment nodeEnvironment;
    private final PluginsService pluginsService;
    private final ExtensionsManager extensionsManager;
    private final NodeClient client;
    private final Collection<LifecycleComponent> pluginLifecycleComponents;
    private final LocalNodeFactory localNodeFactory;
    private final NodeService nodeService;
    final NamedWriteableRegistry namedWriteableRegistry;
    private final AtomicReference<RunnableTaskExecutionListener> runnableTaskListener;

    public Node(Environment environment) {
        this(environment, Collections.emptyList(), true);
    }

    /**
     * Constructs a node
     *
     * @param initialEnvironment         the initial environment for this node, which will be added to by plugins
     * @param classpathPlugins           the plugins to be loaded from the classpath
     * @param forbidPrivateIndexSettings whether or not private index settings are forbidden when creating an index; this is used in the
     *                                   test framework for tests that rely on being able to set private settings
     */
    protected Node(
        final Environment initialEnvironment,
        Collection<Class<? extends Plugin>> classpathPlugins,
        boolean forbidPrivateIndexSettings
    ) {
        final List<Closeable> resourcesToClose = new ArrayList<>(); // register everything we need to release in the case of an error
        boolean success = false;
        try {
            Settings tmpSettings = Settings.builder()
                .put(initialEnvironment.settings())
                .put(Client.CLIENT_TYPE_SETTING_S.getKey(), CLIENT_TYPE)
                // Enabling shard indexing backpressure node-attribute
                .put(NODE_ATTRIBUTES.getKey() + SHARD_INDEXING_PRESSURE_ENABLED_ATTRIBUTE_KEY, "true")
                .build();

            final JvmInfo jvmInfo = JvmInfo.jvmInfo();
            logger.info(
                "version[{}], pid[{}], build[{}/{}/{}], OS[{}/{}/{}], JVM[{}/{}/{}/{}]",
                Build.CURRENT.getQualifiedVersion(),
                jvmInfo.pid(),
                Build.CURRENT.type().displayName(),
                Build.CURRENT.hash(),
                Build.CURRENT.date(),
                Constants.OS_NAME,
                Constants.OS_VERSION,
                Constants.OS_ARCH,
                Constants.JVM_VENDOR,
                Constants.JVM_NAME,
                Constants.JAVA_VERSION,
                Constants.JVM_VERSION
            );
            if (jvmInfo.getBundledJdk()) {
                logger.info("JVM home [{}], using bundled JDK [{}]", System.getProperty("java.home"), jvmInfo.getUsingBundledJdk());
            } else {
                logger.info("JVM home [{}]", System.getProperty("java.home"));
                deprecationLogger.deprecate(
                    "no-jdk",
                    "no-jdk distributions that do not bundle a JDK are deprecated and will be removed in a future release"
                );
            }
            logger.info("JVM arguments {}", Arrays.toString(jvmInfo.getInputArguments()));
            if (Build.CURRENT.isProductionRelease() == false) {
                logger.warn(
                    "version [{}] is a pre-release version of OpenSearch and is not suitable for production",
                    Build.CURRENT.getQualifiedVersion()
                );
            }

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "using config [{}], data [{}], logs [{}], plugins [{}]",
                    initialEnvironment.configFile(),
                    Arrays.toString(initialEnvironment.dataFiles()),
                    initialEnvironment.logsFile(),
                    initialEnvironment.pluginsFile()
                );
            }

            this.pluginsService = new PluginsService(
                tmpSettings,
                initialEnvironment.configFile(),
                initialEnvironment.modulesFile(),
                initialEnvironment.pluginsFile(),
                classpathPlugins
            );

            if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
                this.extensionsManager = new ExtensionsManager(tmpSettings, initialEnvironment.extensionDir());
            } else {
                this.extensionsManager = new NoopExtensionsManager();
            }

            final Settings settings = pluginsService.updatedSettings();

            // Ensure to initialize Feature Flags via the settings from opensearch.yml
            FeatureFlags.initializeFeatureFlags(settings);

            final Set<DiscoveryNodeRole> additionalRoles = pluginsService.filterPlugins(Plugin.class)
                .stream()
                .map(Plugin::getRoles)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
            DiscoveryNode.setAdditionalRoles(additionalRoles);

            DiscoveryNode.setDeprecatedMasterRole();

            /*
             * Create the environment based on the finalized view of the settings. This is to ensure that components get the same setting
             * values, no matter they ask for them from.
             */
            this.environment = new Environment(settings, initialEnvironment.configFile(), Node.NODE_LOCAL_STORAGE_SETTING.get(settings));
            Environment.assertEquivalent(initialEnvironment, this.environment);
            nodeEnvironment = new NodeEnvironment(tmpSettings, environment);
            logger.info(
                "node name [{}], node ID [{}], cluster name [{}], roles {}",
                NODE_NAME_SETTING.get(tmpSettings),
                nodeEnvironment.nodeId(),
                ClusterName.CLUSTER_NAME_SETTING.get(tmpSettings).value(),
                DiscoveryNode.getRolesFromSettings(settings)
                    .stream()
                    .map(DiscoveryNodeRole::roleName)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            );
            resourcesToClose.add(nodeEnvironment);
            localNodeFactory = new LocalNodeFactory(settings, nodeEnvironment.nodeId());

            final List<ExecutorBuilder<?>> executorBuilders = pluginsService.getExecutorBuilders(settings);

            runnableTaskListener = new AtomicReference<>();
            final ThreadPool threadPool = new ThreadPool(settings, runnableTaskListener, executorBuilders.toArray(new ExecutorBuilder[0]));
            resourcesToClose.add(() -> ThreadPool.terminate(threadPool, 10, TimeUnit.SECONDS));
            final ResourceWatcherService resourceWatcherService = new ResourceWatcherService(settings, threadPool);
            resourcesToClose.add(resourceWatcherService);
            // adds the context to the DeprecationLogger so that it does not need to be injected everywhere
            HeaderWarning.setThreadContext(threadPool.getThreadContext());
            resourcesToClose.add(() -> HeaderWarning.removeThreadContext(threadPool.getThreadContext()));

            final List<Setting<?>> additionalSettings = new ArrayList<>();
            // register the node.data, node.ingest, node.master, node.remote_cluster_client settings here so we can mark them private
            additionalSettings.add(NODE_DATA_SETTING);
            additionalSettings.add(NODE_INGEST_SETTING);
            additionalSettings.add(NODE_MASTER_SETTING);
            additionalSettings.add(NODE_REMOTE_CLUSTER_CLIENT);
            additionalSettings.addAll(pluginsService.getPluginSettings());
            final List<String> additionalSettingsFilter = new ArrayList<>(pluginsService.getPluginSettingsFilter());
            for (final ExecutorBuilder<?> builder : threadPool.builders()) {
                additionalSettings.addAll(builder.getRegisteredSettings());
            }
            client = new NodeClient(settings, threadPool);

            final ScriptModule scriptModule = new ScriptModule(settings, pluginsService.filterPlugins(ScriptPlugin.class));
            final ScriptService scriptService = newScriptService(settings, scriptModule.engines, scriptModule.contexts);
            AnalysisModule analysisModule = new AnalysisModule(this.environment, pluginsService.filterPlugins(AnalysisPlugin.class));
            // this is as early as we can validate settings at this point. we already pass them to ScriptModule as well as ThreadPool
            // so we might be late here already

            final Set<SettingUpgrader<?>> settingsUpgraders = pluginsService.filterPlugins(Plugin.class)
                .stream()
                .map(Plugin::getSettingUpgraders)
                .flatMap(List::stream)
                .collect(Collectors.toSet());

            final SettingsModule settingsModule = new SettingsModule(
                settings,
                additionalSettings,
                additionalSettingsFilter,
                settingsUpgraders
            );
            scriptModule.registerClusterSettingsListeners(scriptService, settingsModule.getClusterSettings());
            final NetworkService networkService = new NetworkService(
                getCustomNameResolvers(pluginsService.filterPlugins(DiscoveryPlugin.class))
            );

            List<ClusterPlugin> clusterPlugins = pluginsService.filterPlugins(ClusterPlugin.class);
            final ClusterService clusterService = new ClusterService(settings, settingsModule.getClusterSettings(), threadPool);
            clusterService.addStateApplier(scriptService);
            resourcesToClose.add(clusterService);
            final Set<Setting<?>> consistentSettings = settingsModule.getConsistentSettings();
            if (consistentSettings.isEmpty() == false) {
                clusterService.addLocalNodeMasterListener(
                    new ConsistentSettingsService(settings, clusterService, consistentSettings).newHashPublisher()
                );
            }
            final IngestService ingestService = new IngestService(
                clusterService,
                threadPool,
                this.environment,
                scriptService,
                analysisModule.getAnalysisRegistry(),
                pluginsService.filterPlugins(IngestPlugin.class),
                client
            );
            final SetOnce<RepositoriesService> repositoriesServiceReference = new SetOnce<>();
            final ClusterInfoService clusterInfoService = newClusterInfoService(settings, clusterService, threadPool, client);
            final UsageService usageService = new UsageService();

            ModulesBuilder modules = new ModulesBuilder();
            // plugin modules must be added here, before others or we can get crazy injection errors...
            for (Module pluginModule : pluginsService.createGuiceModules()) {
                modules.add(pluginModule);
            }
            final MonitorService monitorService = new MonitorService(settings, nodeEnvironment, threadPool);
            final FsHealthService fsHealthService = new FsHealthService(
                settings,
                clusterService.getClusterSettings(),
                threadPool,
                nodeEnvironment
            );
            final SetOnce<RerouteService> rerouteServiceReference = new SetOnce<>();
            final InternalSnapshotsInfoService snapshotsInfoService = new InternalSnapshotsInfoService(
                settings,
                clusterService,
                repositoriesServiceReference::get,
                rerouteServiceReference::get
            );
            final ClusterModule clusterModule = new ClusterModule(
                settings,
                clusterService,
                clusterPlugins,
                clusterInfoService,
                snapshotsInfoService,
                threadPool.getThreadContext()
            );
            modules.add(clusterModule);
            IndicesModule indicesModule = new IndicesModule(pluginsService.filterPlugins(MapperPlugin.class));
            modules.add(indicesModule);

            SearchModule searchModule = new SearchModule(settings, pluginsService.filterPlugins(SearchPlugin.class));
            List<BreakerSettings> pluginCircuitBreakers = pluginsService.filterPlugins(CircuitBreakerPlugin.class)
                .stream()
                .map(plugin -> plugin.getCircuitBreaker(settings))
                .collect(Collectors.toList());
            final CircuitBreakerService circuitBreakerService = createCircuitBreakerService(
                settingsModule.getSettings(),
                pluginCircuitBreakers,
                settingsModule.getClusterSettings()
            );
            pluginsService.filterPlugins(CircuitBreakerPlugin.class).forEach(plugin -> {
                CircuitBreaker breaker = circuitBreakerService.getBreaker(plugin.getCircuitBreaker(settings).getName());
                plugin.setCircuitBreaker(breaker);
            });
            resourcesToClose.add(circuitBreakerService);
            modules.add(new GatewayModule());

            PageCacheRecycler pageCacheRecycler = createPageCacheRecycler(settings);
            BigArrays bigArrays = createBigArrays(pageCacheRecycler, circuitBreakerService);
            modules.add(settingsModule);
            List<NamedWriteableRegistry.Entry> namedWriteables = Stream.of(
                NetworkModule.getNamedWriteables().stream(),
                indicesModule.getNamedWriteables().stream(),
                searchModule.getNamedWriteables().stream(),
                pluginsService.filterPlugins(Plugin.class).stream().flatMap(p -> p.getNamedWriteables().stream()),
                ClusterModule.getNamedWriteables().stream()
            ).flatMap(Function.identity()).collect(Collectors.toList());
            final NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(namedWriteables);
            NamedXContentRegistry xContentRegistry = new NamedXContentRegistry(
                Stream.of(
                    NetworkModule.getNamedXContents().stream(),
                    IndicesModule.getNamedXContents().stream(),
                    searchModule.getNamedXContents().stream(),
                    pluginsService.filterPlugins(Plugin.class).stream().flatMap(p -> p.getNamedXContent().stream()),
                    ClusterModule.getNamedXWriteables().stream()
                ).flatMap(Function.identity()).collect(toList())
            );
            final MetaStateService metaStateService = new MetaStateService(nodeEnvironment, xContentRegistry);
            final PersistedClusterStateService lucenePersistedStateFactory = new PersistedClusterStateService(
                nodeEnvironment,
                xContentRegistry,
                bigArrays,
                clusterService.getClusterSettings(),
                threadPool::relativeTimeInMillis
            );

            // collect engine factory providers from plugins
            final Collection<EnginePlugin> enginePlugins = pluginsService.filterPlugins(EnginePlugin.class);
            final Collection<Function<IndexSettings, Optional<EngineFactory>>> engineFactoryProviders = enginePlugins.stream()
                .map(plugin -> (Function<IndexSettings, Optional<EngineFactory>>) plugin::getEngineFactory)
                .collect(Collectors.toList());

            final Map<String, IndexStorePlugin.DirectoryFactory> builtInDirectoryFactories = IndexModule.createBuiltInDirectoryFactories(
                repositoriesServiceReference::get,
                threadPool
            );
            final Map<String, IndexStorePlugin.DirectoryFactory> directoryFactories = new HashMap<>();
            pluginsService.filterPlugins(IndexStorePlugin.class)
                .stream()
                .map(IndexStorePlugin::getDirectoryFactories)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                .forEach((k, v) -> {
                    // do not allow any plugin-provided index store type to conflict with a built-in type
                    if (builtInDirectoryFactories.containsKey(k)) {
                        throw new IllegalStateException("registered index store type [" + k + "] conflicts with a built-in type");
                    }
                    directoryFactories.put(k, v);
                });
            directoryFactories.putAll(builtInDirectoryFactories);

            final Map<String, IndexStorePlugin.RecoveryStateFactory> recoveryStateFactories = pluginsService.filterPlugins(
                IndexStorePlugin.class
            )
                .stream()
                .map(IndexStorePlugin::getRecoveryStateFactories)
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            final Map<String, Collection<SystemIndexDescriptor>> systemIndexDescriptorMap = Collections.unmodifiableMap(
                pluginsService.filterPlugins(SystemIndexPlugin.class)
                    .stream()
                    .collect(
                        Collectors.toMap(plugin -> plugin.getClass().getSimpleName(), plugin -> plugin.getSystemIndexDescriptors(settings))
                    )
            );
            final SystemIndices systemIndices = new SystemIndices(systemIndexDescriptorMap);

            final RerouteService rerouteService = new BatchedRerouteService(clusterService, clusterModule.getAllocationService()::reroute);
            rerouteServiceReference.set(rerouteService);
            clusterService.setRerouteService(rerouteService);

            final IndexStorePlugin.RemoteDirectoryFactory remoteDirectoryFactory = new RemoteSegmentStoreDirectoryFactory(
                repositoriesServiceReference::get
            );

            final IndicesService indicesService;
            if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
                indicesService = new IndicesService(
                    settings,
                    pluginsService,
                    extensionsManager,
                    nodeEnvironment,
                    xContentRegistry,
                    analysisModule.getAnalysisRegistry(),
                    clusterModule.getIndexNameExpressionResolver(),
                    indicesModule.getMapperRegistry(),
                    namedWriteableRegistry,
                    threadPool,
                    settingsModule.getIndexScopedSettings(),
                    circuitBreakerService,
                    bigArrays,
                    scriptService,
                    clusterService,
                    client,
                    metaStateService,
                    engineFactoryProviders,
                    Map.copyOf(directoryFactories),
                    searchModule.getValuesSourceRegistry(),
                    recoveryStateFactories,
                    remoteDirectoryFactory,
                    repositoriesServiceReference::get
                );
            } else {
                indicesService = new IndicesService(
                    settings,
                    pluginsService,
                    nodeEnvironment,
                    xContentRegistry,
                    analysisModule.getAnalysisRegistry(),
                    clusterModule.getIndexNameExpressionResolver(),
                    indicesModule.getMapperRegistry(),
                    namedWriteableRegistry,
                    threadPool,
                    settingsModule.getIndexScopedSettings(),
                    circuitBreakerService,
                    bigArrays,
                    scriptService,
                    clusterService,
                    client,
                    metaStateService,
                    engineFactoryProviders,
                    Map.copyOf(directoryFactories),
                    searchModule.getValuesSourceRegistry(),
                    recoveryStateFactories,
                    remoteDirectoryFactory,
                    repositoriesServiceReference::get
                );
            }

            final AliasValidator aliasValidator = new AliasValidator();

            final ShardLimitValidator shardLimitValidator = new ShardLimitValidator(settings, clusterService, systemIndices);
            final AwarenessReplicaBalance awarenessReplicaBalance = new AwarenessReplicaBalance(
                settings,
                clusterService.getClusterSettings()
            );
            final MetadataCreateIndexService metadataCreateIndexService = new MetadataCreateIndexService(
                settings,
                clusterService,
                indicesService,
                clusterModule.getAllocationService(),
                aliasValidator,
                shardLimitValidator,
                environment,
                settingsModule.getIndexScopedSettings(),
                threadPool,
                xContentRegistry,
                systemIndices,
                forbidPrivateIndexSettings,
                awarenessReplicaBalance
            );
            pluginsService.filterPlugins(Plugin.class)
                .forEach(
                    p -> p.getAdditionalIndexSettingProviders().forEach(metadataCreateIndexService::addAdditionalIndexSettingProvider)
                );

            final MetadataCreateDataStreamService metadataCreateDataStreamService = new MetadataCreateDataStreamService(
                threadPool,
                clusterService,
                metadataCreateIndexService
            );

            Collection<Object> pluginComponents = pluginsService.filterPlugins(Plugin.class)
                .stream()
                .flatMap(
                    p -> p.createComponents(
                        client,
                        clusterService,
                        threadPool,
                        resourceWatcherService,
                        scriptService,
                        xContentRegistry,
                        environment,
                        nodeEnvironment,
                        namedWriteableRegistry,
                        clusterModule.getIndexNameExpressionResolver(),
                        repositoriesServiceReference::get
                    ).stream()
                )
                .collect(Collectors.toList());

            ActionModule actionModule = new ActionModule(
                settings,
                clusterModule.getIndexNameExpressionResolver(),
                settingsModule.getIndexScopedSettings(),
                settingsModule.getClusterSettings(),
                settingsModule.getSettingsFilter(),
                threadPool,
                pluginsService.filterPlugins(ActionPlugin.class),
                client,
                circuitBreakerService,
                usageService,
                systemIndices
            );
            modules.add(actionModule);

            final RestController restController = actionModule.getRestController();

            final NetworkModule networkModule = new NetworkModule(
                settings,
                pluginsService.filterPlugins(NetworkPlugin.class),
                threadPool,
                bigArrays,
                pageCacheRecycler,
                circuitBreakerService,
                namedWriteableRegistry,
                xContentRegistry,
                networkService,
                restController,
                clusterService.getClusterSettings()
            );
            Collection<UnaryOperator<Map<String, IndexTemplateMetadata>>> indexTemplateMetadataUpgraders = pluginsService.filterPlugins(
                Plugin.class
            ).stream().map(Plugin::getIndexTemplateMetadataUpgrader).collect(Collectors.toList());
            final MetadataUpgrader metadataUpgrader = new MetadataUpgrader(indexTemplateMetadataUpgraders);
            final MetadataIndexUpgradeService metadataIndexUpgradeService = new MetadataIndexUpgradeService(
                settings,
                xContentRegistry,
                indicesModule.getMapperRegistry(),
                settingsModule.getIndexScopedSettings(),
                systemIndices,
                scriptService
            );
            if (DiscoveryNode.isClusterManagerNode(settings)) {
                clusterService.addListener(new SystemIndexMetadataUpgradeService(systemIndices, clusterService));
            }
            new TemplateUpgradeService(client, clusterService, threadPool, indexTemplateMetadataUpgraders);
            final Transport transport = networkModule.getTransportSupplier().get();
            Set<String> taskHeaders = Stream.concat(
                pluginsService.filterPlugins(ActionPlugin.class).stream().flatMap(p -> p.getTaskHeaders().stream()),
                Stream.of(Task.X_OPAQUE_ID)
            ).collect(Collectors.toSet());
            final TransportService transportService = newTransportService(
                settings,
                transport,
                threadPool,
                networkModule.getTransportInterceptor(),
                localNodeFactory,
                settingsModule.getClusterSettings(),
                taskHeaders
            );
            if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
                this.extensionsManager.initializeServicesAndRestHandler(
                    restController,
                    settingsModule,
                    transportService,
                    clusterService,
                    environment.settings(),
                    client
                );
            }
            final GatewayMetaState gatewayMetaState = new GatewayMetaState();
            final ResponseCollectorService responseCollectorService = new ResponseCollectorService(clusterService);
            final SearchTransportService searchTransportService = new SearchTransportService(
                transportService,
                SearchExecutionStatsCollector.makeWrapper(responseCollectorService)
            );
            final HttpServerTransport httpServerTransport = newHttpTransport(networkModule);
            final IndexingPressureService indexingPressureService = new IndexingPressureService(settings, clusterService);
            // Going forward, IndexingPressureService will have required constructs for exposing listeners/interfaces for plugin
            // development. Then we can deprecate Getter and Setter for IndexingPressureService in ClusterService (#478).
            clusterService.setIndexingPressureService(indexingPressureService);

            final TaskResourceTrackingService taskResourceTrackingService = new TaskResourceTrackingService(
                settings,
                clusterService.getClusterSettings(),
                threadPool
            );

            final SearchBackpressureSettings searchBackpressureSettings = new SearchBackpressureSettings(
                settings,
                clusterService.getClusterSettings()
            );

            final SearchBackpressureService searchBackpressureService = new SearchBackpressureService(
                searchBackpressureSettings,
                taskResourceTrackingService,
                threadPool
            );

            final RecoverySettings recoverySettings = new RecoverySettings(settings, settingsModule.getClusterSettings());
            RepositoriesModule repositoriesModule = new RepositoriesModule(
                this.environment,
                pluginsService.filterPlugins(RepositoryPlugin.class),
                transportService,
                clusterService,
                threadPool,
                xContentRegistry,
                recoverySettings
            );
            RepositoriesService repositoryService = repositoriesModule.getRepositoryService();
            repositoriesServiceReference.set(repositoryService);
            SnapshotsService snapshotsService = new SnapshotsService(
                settings,
                clusterService,
                clusterModule.getIndexNameExpressionResolver(),
                repositoryService,
                transportService,
                actionModule.getActionFilters()
            );
            SnapshotShardsService snapshotShardsService = new SnapshotShardsService(
                settings,
                clusterService,
                repositoryService,
                transportService,
                indicesService
            );
            TransportNodesSnapshotsStatus nodesSnapshotsStatus = new TransportNodesSnapshotsStatus(
                threadPool,
                clusterService,
                transportService,
                snapshotShardsService,
                actionModule.getActionFilters()
            );
            RestoreService restoreService = new RestoreService(
                clusterService,
                repositoryService,
                clusterModule.getAllocationService(),
                metadataCreateIndexService,
                metadataIndexUpgradeService,
                clusterService.getClusterSettings(),
                shardLimitValidator
            );

            final DiskThresholdMonitor diskThresholdMonitor = new DiskThresholdMonitor(
                settings,
                clusterService::state,
                clusterService.getClusterSettings(),
                client,
                threadPool::relativeTimeInMillis,
                rerouteService
            );
            clusterInfoService.addListener(diskThresholdMonitor::onNewInfo);

            final DiscoveryModule discoveryModule = new DiscoveryModule(
                settings,
                threadPool,
                transportService,
                namedWriteableRegistry,
                networkService,
                clusterService.getClusterManagerService(),
                clusterService.getClusterApplierService(),
                clusterService.getClusterSettings(),
                pluginsService.filterPlugins(DiscoveryPlugin.class),
                clusterModule.getAllocationService(),
                environment.configFile(),
                gatewayMetaState,
                rerouteService,
                fsHealthService
            );
            this.nodeService = new NodeService(
                settings,
                threadPool,
                monitorService,
                discoveryModule.getDiscovery(),
                transportService,
                indicesService,
                pluginsService,
                circuitBreakerService,
                scriptService,
                httpServerTransport,
                ingestService,
                clusterService,
                settingsModule.getSettingsFilter(),
                responseCollectorService,
                searchTransportService,
                indexingPressureService,
                searchModule.getValuesSourceRegistry().getUsageService(),
                searchBackpressureService
            );

            final SearchService searchService = newSearchService(
                clusterService,
                indicesService,
                threadPool,
                scriptService,
                bigArrays,
                searchModule.getQueryPhase(),
                searchModule.getFetchPhase(),
                responseCollectorService,
                circuitBreakerService,
                searchModule.getIndexSearcherExecutor(threadPool)
            );

            final List<PersistentTasksExecutor<?>> tasksExecutors = pluginsService.filterPlugins(PersistentTaskPlugin.class)
                .stream()
                .map(
                    p -> p.getPersistentTasksExecutor(
                        clusterService,
                        threadPool,
                        client,
                        settingsModule,
                        clusterModule.getIndexNameExpressionResolver()
                    )
                )
                .flatMap(List::stream)
                .collect(toList());

            final PersistentTasksExecutorRegistry registry = new PersistentTasksExecutorRegistry(tasksExecutors);
            final PersistentTasksClusterService persistentTasksClusterService = new PersistentTasksClusterService(
                settings,
                registry,
                clusterService,
                threadPool
            );
            resourcesToClose.add(persistentTasksClusterService);
            final PersistentTasksService persistentTasksService = new PersistentTasksService(clusterService, threadPool, client);

            modules.add(b -> {
                b.bind(Node.class).toInstance(this);
                b.bind(NodeService.class).toInstance(nodeService);
                b.bind(NamedXContentRegistry.class).toInstance(xContentRegistry);
                b.bind(PluginsService.class).toInstance(pluginsService);
                b.bind(Client.class).toInstance(client);
                b.bind(NodeClient.class).toInstance(client);
                b.bind(Environment.class).toInstance(this.environment);
                if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
                    b.bind(ExtensionsManager.class).toInstance(this.extensionsManager);
                }
                b.bind(ThreadPool.class).toInstance(threadPool);
                b.bind(NodeEnvironment.class).toInstance(nodeEnvironment);
                b.bind(ResourceWatcherService.class).toInstance(resourceWatcherService);
                b.bind(CircuitBreakerService.class).toInstance(circuitBreakerService);
                b.bind(BigArrays.class).toInstance(bigArrays);
                b.bind(PageCacheRecycler.class).toInstance(pageCacheRecycler);
                b.bind(ScriptService.class).toInstance(scriptService);
                b.bind(AnalysisRegistry.class).toInstance(analysisModule.getAnalysisRegistry());
                b.bind(IngestService.class).toInstance(ingestService);
                b.bind(IndexingPressureService.class).toInstance(indexingPressureService);
                b.bind(TaskResourceTrackingService.class).toInstance(taskResourceTrackingService);
                b.bind(SearchBackpressureService.class).toInstance(searchBackpressureService);
                b.bind(UsageService.class).toInstance(usageService);
                b.bind(AggregationUsageService.class).toInstance(searchModule.getValuesSourceRegistry().getUsageService());
                b.bind(NamedWriteableRegistry.class).toInstance(namedWriteableRegistry);
                b.bind(MetadataUpgrader.class).toInstance(metadataUpgrader);
                b.bind(MetaStateService.class).toInstance(metaStateService);
                b.bind(PersistedClusterStateService.class).toInstance(lucenePersistedStateFactory);
                b.bind(IndicesService.class).toInstance(indicesService);
                b.bind(AliasValidator.class).toInstance(aliasValidator);
                b.bind(MetadataCreateIndexService.class).toInstance(metadataCreateIndexService);
                b.bind(AwarenessReplicaBalance.class).toInstance(awarenessReplicaBalance);
                b.bind(MetadataCreateDataStreamService.class).toInstance(metadataCreateDataStreamService);
                b.bind(SearchService.class).toInstance(searchService);
                b.bind(SearchTransportService.class).toInstance(searchTransportService);
                b.bind(SearchPhaseController.class)
                    .toInstance(new SearchPhaseController(namedWriteableRegistry, searchService::aggReduceContextBuilder));
                b.bind(Transport.class).toInstance(transport);
                b.bind(TransportService.class).toInstance(transportService);
                b.bind(NetworkService.class).toInstance(networkService);
                b.bind(UpdateHelper.class).toInstance(new UpdateHelper(scriptService));
                b.bind(MetadataIndexUpgradeService.class).toInstance(metadataIndexUpgradeService);
                b.bind(ClusterInfoService.class).toInstance(clusterInfoService);
                b.bind(SnapshotsInfoService.class).toInstance(snapshotsInfoService);
                b.bind(GatewayMetaState.class).toInstance(gatewayMetaState);
                b.bind(Discovery.class).toInstance(discoveryModule.getDiscovery());
                {
                    processRecoverySettings(settingsModule.getClusterSettings(), recoverySettings);
                    b.bind(PeerRecoverySourceService.class)
                        .toInstance(new PeerRecoverySourceService(transportService, indicesService, recoverySettings));
                    b.bind(PeerRecoveryTargetService.class)
                        .toInstance(new PeerRecoveryTargetService(threadPool, transportService, recoverySettings, clusterService));
                    if (FeatureFlags.isEnabled(REPLICATION_TYPE)) {
                        b.bind(SegmentReplicationTargetService.class)
                            .toInstance(
                                new SegmentReplicationTargetService(
                                    threadPool,
                                    recoverySettings,
                                    transportService,
                                    new SegmentReplicationSourceFactory(transportService, recoverySettings, clusterService),
                                    indicesService
                                )
                            );
                        b.bind(SegmentReplicationSourceService.class)
                            .toInstance(new SegmentReplicationSourceService(indicesService, transportService, recoverySettings));
                    } else {
                        b.bind(SegmentReplicationTargetService.class).toInstance(SegmentReplicationTargetService.NO_OP);
                        b.bind(SegmentReplicationSourceService.class).toInstance(SegmentReplicationSourceService.NO_OP);
                    }
                }
                b.bind(HttpServerTransport.class).toInstance(httpServerTransport);
                pluginComponents.stream().forEach(p -> b.bind((Class) p.getClass()).toInstance(p));
                b.bind(PersistentTasksService.class).toInstance(persistentTasksService);
                b.bind(PersistentTasksClusterService.class).toInstance(persistentTasksClusterService);
                b.bind(PersistentTasksExecutorRegistry.class).toInstance(registry);
                b.bind(RepositoriesService.class).toInstance(repositoryService);
                b.bind(SnapshotsService.class).toInstance(snapshotsService);
                b.bind(SnapshotShardsService.class).toInstance(snapshotShardsService);
                b.bind(TransportNodesSnapshotsStatus.class).toInstance(nodesSnapshotsStatus);
                b.bind(RestoreService.class).toInstance(restoreService);
                b.bind(RerouteService.class).toInstance(rerouteService);
                b.bind(ShardLimitValidator.class).toInstance(shardLimitValidator);
                b.bind(FsHealthService.class).toInstance(fsHealthService);
                b.bind(SystemIndices.class).toInstance(systemIndices);
            });
            injector = modules.createInjector();

            // We allocate copies of existing shards by looking for a viable copy of the shard in the cluster and assigning the shard there.
            // The search for viable copies is triggered by an allocation attempt (i.e. a reroute) and is performed asynchronously. When it
            // completes we trigger another reroute to try the allocation again. This means there is a circular dependency: the allocation
            // service needs access to the existing shards allocators (e.g. the GatewayAllocator) which need to be able to trigger a
            // reroute, which needs to call into the allocation service. We close the loop here:
            clusterModule.setExistingShardsAllocators(injector.getInstance(GatewayAllocator.class));

            List<LifecycleComponent> pluginLifecycleComponents = pluginComponents.stream()
                .filter(p -> p instanceof LifecycleComponent)
                .map(p -> (LifecycleComponent) p)
                .collect(Collectors.toList());
            pluginLifecycleComponents.addAll(
                pluginsService.getGuiceServiceClasses().stream().map(injector::getInstance).collect(Collectors.toList())
            );
            resourcesToClose.addAll(pluginLifecycleComponents);
            resourcesToClose.add(injector.getInstance(PeerRecoverySourceService.class));
            this.pluginLifecycleComponents = Collections.unmodifiableList(pluginLifecycleComponents);
            client.initialize(injector.getInstance(new Key<Map<ActionType, TransportAction>>() {
            }), () -> clusterService.localNode().getId(), transportService.getRemoteClusterService(), namedWriteableRegistry);
            this.namedWriteableRegistry = namedWriteableRegistry;

            logger.debug("initializing HTTP handlers ...");
            actionModule.initRestHandlers(() -> clusterService.state().nodes());
            logger.info("initialized");

            success = true;
        } catch (IOException ex) {
            throw new OpenSearchException("failed to bind service", ex);
        } finally {
            if (!success) {
                IOUtils.closeWhileHandlingException(resourcesToClose);
            }
        }
    }

    protected TransportService newTransportService(
        Settings settings,
        Transport transport,
        ThreadPool threadPool,
        TransportInterceptor interceptor,
        Function<BoundTransportAddress, DiscoveryNode> localNodeFactory,
        ClusterSettings clusterSettings,
        Set<String> taskHeaders
    ) {
        return new TransportService(settings, transport, threadPool, interceptor, localNodeFactory, clusterSettings, taskHeaders);
    }

    protected void processRecoverySettings(ClusterSettings clusterSettings, RecoverySettings recoverySettings) {
        // Noop in production, overridden by tests
    }

    /**
     * The settings that are used by this node. Contains original settings as well as additional settings provided by plugins.
     */
    public Settings settings() {
        return this.environment.settings();
    }

    /**
     * A client that can be used to execute actions (operations) against the cluster.
     */
    public Client client() {
        return client;
    }

    /**
     * Returns the environment of the node
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Returns the {@link NodeEnvironment} instance of this node
     */
    public NodeEnvironment getNodeEnvironment() {
        return nodeEnvironment;
    }

    /**
     * Start the node. If the node is already started, this method is no-op.
     */
    public Node start() throws NodeValidationException {
        if (!lifecycle.moveToStarted()) {
            return this;
        }

        logger.info("starting ...");
        pluginLifecycleComponents.forEach(LifecycleComponent::start);

        injector.getInstance(MappingUpdatedAction.class).setClient(client);
        injector.getInstance(IndicesService.class).start();
        injector.getInstance(IndicesClusterStateService.class).start();
        injector.getInstance(SnapshotsService.class).start();
        injector.getInstance(SnapshotShardsService.class).start();
        injector.getInstance(RepositoriesService.class).start();
        injector.getInstance(SearchService.class).start();
        injector.getInstance(FsHealthService.class).start();
        nodeService.getMonitorService().start();
        nodeService.getSearchBackpressureService().start();

        final ClusterService clusterService = injector.getInstance(ClusterService.class);

        final NodeConnectionsService nodeConnectionsService = injector.getInstance(NodeConnectionsService.class);
        nodeConnectionsService.start();
        clusterService.setNodeConnectionsService(nodeConnectionsService);

        injector.getInstance(GatewayService.class).start();
        Discovery discovery = injector.getInstance(Discovery.class);
        clusterService.getClusterManagerService().setClusterStatePublisher(discovery::publish);

        // Start the transport service now so the publish address will be added to the local disco node in ClusterService
        TransportService transportService = injector.getInstance(TransportService.class);
        transportService.getTaskManager().setTaskResultsService(injector.getInstance(TaskResultsService.class));
        transportService.getTaskManager().setTaskCancellationService(new TaskCancellationService(transportService));

        TaskResourceTrackingService taskResourceTrackingService = injector.getInstance(TaskResourceTrackingService.class);
        transportService.getTaskManager().setTaskResourceTrackingService(taskResourceTrackingService);
        runnableTaskListener.set(taskResourceTrackingService);

        transportService.start();
        assert localNodeFactory.getNode() != null;
        assert transportService.getLocalNode().equals(localNodeFactory.getNode())
            : "transportService has a different local node than the factory provided";
        injector.getInstance(PeerRecoverySourceService.class).start();
        if (FeatureFlags.isEnabled(REPLICATION_TYPE)) {
            injector.getInstance(SegmentReplicationSourceService.class).start();
        }

        // Load (and maybe upgrade) the metadata stored on disk
        final GatewayMetaState gatewayMetaState = injector.getInstance(GatewayMetaState.class);
        gatewayMetaState.start(
            settings(),
            transportService,
            clusterService,
            injector.getInstance(MetaStateService.class),
            injector.getInstance(MetadataIndexUpgradeService.class),
            injector.getInstance(MetadataUpgrader.class),
            injector.getInstance(PersistedClusterStateService.class)
        );
        if (Assertions.ENABLED) {
            try {
                assert injector.getInstance(MetaStateService.class).loadFullState().v1().isEmpty();
                final NodeMetadata nodeMetadata = NodeMetadata.FORMAT.loadLatestState(
                    logger,
                    NamedXContentRegistry.EMPTY,
                    nodeEnvironment.nodeDataPaths()
                );
                assert nodeMetadata != null;
                assert nodeMetadata.nodeVersion().equals(Version.CURRENT);
                assert nodeMetadata.nodeId().equals(localNodeFactory.getNode().getId());
            } catch (IOException e) {
                assert false : e;
            }
        }
        // we load the global state here (the persistent part of the cluster state stored on disk) to
        // pass it to the bootstrap checks to allow plugins to enforce certain preconditions based on the recovered state.
        final Metadata onDiskMetadata = gatewayMetaState.getPersistedState().getLastAcceptedState().metadata();
        assert onDiskMetadata != null : "metadata is null but shouldn't"; // this is never null
        validateNodeBeforeAcceptingRequests(
            new BootstrapContext(environment, onDiskMetadata),
            transportService.boundAddress(),
            pluginsService.filterPlugins(Plugin.class).stream().flatMap(p -> p.getBootstrapChecks().stream()).collect(Collectors.toList())
        );

        clusterService.addStateApplier(transportService.getTaskManager());
        // start after transport service so the local disco is known
        discovery.start(); // start before cluster service so that it can set initial state on ClusterApplierService
        clusterService.start();
        assert clusterService.localNode().equals(localNodeFactory.getNode())
            : "clusterService has a different local node than the factory provided";
        transportService.acceptIncomingRequests();
        if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
            extensionsManager.initialize();
        }
        discovery.startInitialJoin();
        final TimeValue initialStateTimeout = DiscoverySettings.INITIAL_STATE_TIMEOUT_SETTING.get(settings());
        configureNodeAndClusterIdStateListener(clusterService);

        if (initialStateTimeout.millis() > 0) {
            final ThreadPool thread = injector.getInstance(ThreadPool.class);
            ClusterState clusterState = clusterService.state();
            ClusterStateObserver observer = new ClusterStateObserver(clusterState, clusterService, null, logger, thread.getThreadContext());

            if (clusterState.nodes().getClusterManagerNodeId() == null) {
                logger.debug("waiting to join the cluster. timeout [{}]", initialStateTimeout);
                final CountDownLatch latch = new CountDownLatch(1);
                observer.waitForNextChange(new ClusterStateObserver.Listener() {
                    @Override
                    public void onNewClusterState(ClusterState state) {
                        latch.countDown();
                    }

                    @Override
                    public void onClusterServiceClose() {
                        latch.countDown();
                    }

                    @Override
                    public void onTimeout(TimeValue timeout) {
                        logger.warn("timed out while waiting for initial discovery state - timeout: {}", initialStateTimeout);
                        latch.countDown();
                    }
                }, state -> state.nodes().getClusterManagerNodeId() != null, initialStateTimeout);

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new OpenSearchTimeoutException("Interrupted while waiting for initial discovery state");
                }
            }
        }

        injector.getInstance(HttpServerTransport.class).start();

        if (WRITE_PORTS_FILE_SETTING.get(settings())) {
            TransportService transport = injector.getInstance(TransportService.class);
            writePortsFile("transport", transport.boundAddress());
            HttpServerTransport http = injector.getInstance(HttpServerTransport.class);
            writePortsFile("http", http.boundAddress());
        }

        logger.info("started");

        pluginsService.filterPlugins(ClusterPlugin.class).forEach(ClusterPlugin::onNodeStarted);

        return this;
    }

    protected void configureNodeAndClusterIdStateListener(ClusterService clusterService) {
        NodeAndClusterIdStateListener.getAndSetNodeIdAndClusterId(
            clusterService,
            injector.getInstance(ThreadPool.class).getThreadContext()
        );
    }

    private Node stop() {
        if (!lifecycle.moveToStopped()) {
            return this;
        }
        logger.info("stopping ...");

        injector.getInstance(ResourceWatcherService.class).close();
        injector.getInstance(HttpServerTransport.class).stop();

        injector.getInstance(SnapshotsService.class).stop();
        injector.getInstance(SnapshotShardsService.class).stop();
        injector.getInstance(RepositoriesService.class).stop();
        // stop any changes happening as a result of cluster state changes
        injector.getInstance(IndicesClusterStateService.class).stop();
        // close discovery early to not react to pings anymore.
        // This can confuse other nodes and delay things - mostly if we're the cluster manager and we're running tests.
        injector.getInstance(Discovery.class).stop();
        // we close indices first, so operations won't be allowed on it
        injector.getInstance(ClusterService.class).stop();
        injector.getInstance(NodeConnectionsService.class).stop();
        injector.getInstance(FsHealthService.class).stop();
        nodeService.getMonitorService().stop();
        nodeService.getSearchBackpressureService().stop();
        injector.getInstance(GatewayService.class).stop();
        injector.getInstance(SearchService.class).stop();
        injector.getInstance(TransportService.class).stop();

        pluginLifecycleComponents.forEach(LifecycleComponent::stop);
        // we should stop this last since it waits for resources to get released
        // if we had scroll searchers etc or recovery going on we wait for to finish.
        injector.getInstance(IndicesService.class).stop();
        logger.info("stopped");

        return this;
    }

    // During concurrent close() calls we want to make sure that all of them return after the node has completed it's shutdown cycle.
    // If not, the hook that is added in Bootstrap#setup() will be useless:
    // close() might not be executed, in case another (for example api) call to close() has already set some lifecycles to stopped.
    // In this case the process will be terminated even if the first call to close() has not finished yet.
    @Override
    public synchronized void close() throws IOException {
        synchronized (lifecycle) {
            if (lifecycle.started()) {
                stop();
            }
            if (!lifecycle.moveToClosed()) {
                return;
            }
        }

        logger.info("closing ...");
        List<Closeable> toClose = new ArrayList<>();
        StopWatch stopWatch = new StopWatch("node_close");
        toClose.add(() -> stopWatch.start("node_service"));
        toClose.add(nodeService);
        toClose.add(() -> stopWatch.stop().start("http"));
        toClose.add(injector.getInstance(HttpServerTransport.class));
        toClose.add(() -> stopWatch.stop().start("snapshot_service"));
        toClose.add(injector.getInstance(SnapshotsService.class));
        toClose.add(injector.getInstance(SnapshotShardsService.class));
        toClose.add(injector.getInstance(RepositoriesService.class));
        toClose.add(() -> stopWatch.stop().start("client"));
        Releasables.close(injector.getInstance(Client.class));
        toClose.add(() -> stopWatch.stop().start("indices_cluster"));
        toClose.add(injector.getInstance(IndicesClusterStateService.class));
        toClose.add(() -> stopWatch.stop().start("indices"));
        toClose.add(injector.getInstance(IndicesService.class));
        // close filter/fielddata caches after indices
        toClose.add(injector.getInstance(IndicesStore.class));
        toClose.add(injector.getInstance(PeerRecoverySourceService.class));
        if (FeatureFlags.isEnabled(REPLICATION_TYPE)) {
            toClose.add(injector.getInstance(SegmentReplicationSourceService.class));
        }
        toClose.add(() -> stopWatch.stop().start("cluster"));
        toClose.add(injector.getInstance(ClusterService.class));
        toClose.add(() -> stopWatch.stop().start("node_connections_service"));
        toClose.add(injector.getInstance(NodeConnectionsService.class));
        toClose.add(() -> stopWatch.stop().start("discovery"));
        toClose.add(injector.getInstance(Discovery.class));
        toClose.add(() -> stopWatch.stop().start("monitor"));
        toClose.add(nodeService.getMonitorService());
        toClose.add(nodeService.getSearchBackpressureService());
        toClose.add(() -> stopWatch.stop().start("fsHealth"));
        toClose.add(injector.getInstance(FsHealthService.class));
        toClose.add(() -> stopWatch.stop().start("gateway"));
        toClose.add(injector.getInstance(GatewayService.class));
        toClose.add(() -> stopWatch.stop().start("search"));
        toClose.add(injector.getInstance(SearchService.class));
        toClose.add(() -> stopWatch.stop().start("transport"));
        toClose.add(injector.getInstance(TransportService.class));

        for (LifecycleComponent plugin : pluginLifecycleComponents) {
            toClose.add(() -> stopWatch.stop().start("plugin(" + plugin.getClass().getName() + ")"));
            toClose.add(plugin);
        }
        toClose.addAll(pluginsService.filterPlugins(Plugin.class));

        toClose.add(() -> stopWatch.stop().start("script"));
        toClose.add(injector.getInstance(ScriptService.class));

        toClose.add(() -> stopWatch.stop().start("thread_pool"));
        toClose.add(() -> injector.getInstance(ThreadPool.class).shutdown());
        // Don't call shutdownNow here, it might break ongoing operations on Lucene indices.
        // See https://issues.apache.org/jira/browse/LUCENE-7248. We call shutdownNow in
        // awaitClose if the node doesn't finish closing within the specified time.

        toClose.add(() -> stopWatch.stop().start("gateway_meta_state"));
        toClose.add(injector.getInstance(GatewayMetaState.class));

        toClose.add(() -> stopWatch.stop().start("node_environment"));
        toClose.add(injector.getInstance(NodeEnvironment.class));
        toClose.add(stopWatch::stop);

        if (logger.isTraceEnabled()) {
            toClose.add(() -> logger.trace("Close times for each service:\n{}", stopWatch.prettyPrint()));
        }
        IOUtils.close(toClose);
        logger.info("closed");
    }

    /**
     * Wait for this node to be effectively closed.
     */
    // synchronized to prevent running concurrently with close()
    public synchronized boolean awaitClose(long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (lifecycle.closed() == false) {
            // We don't want to shutdown the threadpool or interrupt threads on a node that is not
            // closed yet.
            throw new IllegalStateException("Call close() first");
        }

        ThreadPool threadPool = injector.getInstance(ThreadPool.class);
        final boolean terminated = ThreadPool.terminate(threadPool, timeout, timeUnit);
        if (terminated) {
            // All threads terminated successfully. Because search, recovery and all other operations
            // that run on shards run in the threadpool, indices should be effectively closed by now.
            if (nodeService.awaitClose(0, TimeUnit.MILLISECONDS) == false) {
                throw new IllegalStateException(
                    "Some shards are still open after the threadpool terminated. "
                        + "Something is leaking index readers or store references."
                );
            }
        }
        return terminated;
    }

    /**
     * Returns {@code true} if the node is closed.
     */
    public boolean isClosed() {
        return lifecycle.closed();
    }

    public Injector injector() {
        return this.injector;
    }

    /**
     * Hook for validating the node after network
     * services are started but before the cluster service is started
     * and before the network service starts accepting incoming network
     * requests.
     *
     * @param context               the bootstrap context for this node
     * @param boundTransportAddress the network addresses the node is
     *                              bound and publishing to
     */
    @SuppressWarnings("unused")
    protected void validateNodeBeforeAcceptingRequests(
        final BootstrapContext context,
        final BoundTransportAddress boundTransportAddress,
        List<BootstrapCheck> bootstrapChecks
    ) throws NodeValidationException {}

    /** Writes a file to the logs dir containing the ports for the given transport type */
    private void writePortsFile(String type, BoundTransportAddress boundAddress) {
        Path tmpPortsFile = environment.logsFile().resolve(type + ".ports.tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tmpPortsFile, Charset.forName("UTF-8"))) {
            for (TransportAddress address : boundAddress.boundAddresses()) {
                InetAddress inetAddress = InetAddress.getByName(address.getAddress());
                writer.write(NetworkAddress.format(new InetSocketAddress(inetAddress, address.getPort())) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write ports file", e);
        }
        Path portsFile = environment.logsFile().resolve(type + ".ports");
        try {
            Files.move(tmpPortsFile, portsFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename ports file", e);
        }
    }

    /**
     * The {@link PluginsService} used to build this node's components.
     */
    protected PluginsService getPluginsService() {
        return pluginsService;
    }

    /**
     * Creates a new {@link CircuitBreakerService} based on the settings provided.
     * @see #BREAKER_TYPE_KEY
     */
    public static CircuitBreakerService createCircuitBreakerService(
        Settings settings,
        List<BreakerSettings> breakerSettings,
        ClusterSettings clusterSettings
    ) {
        String type = BREAKER_TYPE_KEY.get(settings);
        if (type.equals("hierarchy")) {
            return new HierarchyCircuitBreakerService(settings, breakerSettings, clusterSettings);
        } else if (type.equals("none")) {
            return new NoneCircuitBreakerService();
        } else {
            throw new IllegalArgumentException("Unknown circuit breaker type [" + type + "]");
        }
    }

    /**
     * Creates a new {@link BigArrays} instance used for this node.
     * This method can be overwritten by subclasses to change their {@link BigArrays} implementation for instance for testing
     */
    BigArrays createBigArrays(PageCacheRecycler pageCacheRecycler, CircuitBreakerService circuitBreakerService) {
        return new BigArrays(pageCacheRecycler, circuitBreakerService, CircuitBreaker.REQUEST);
    }

    /**
     * Creates a new {@link BigArrays} instance used for this node.
     * This method can be overwritten by subclasses to change their {@link BigArrays} implementation for instance for testing
     */
    PageCacheRecycler createPageCacheRecycler(Settings settings) {
        return new PageCacheRecycler(settings);
    }

    /**
     * Creates a new the SearchService. This method can be overwritten by tests to inject mock implementations.
     */
    protected SearchService newSearchService(
        ClusterService clusterService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ScriptService scriptService,
        BigArrays bigArrays,
        QueryPhase queryPhase,
        FetchPhase fetchPhase,
        ResponseCollectorService responseCollectorService,
        CircuitBreakerService circuitBreakerService,
        Executor indexSearcherExecutor
    ) {
        return new SearchService(
            clusterService,
            indicesService,
            threadPool,
            scriptService,
            bigArrays,
            queryPhase,
            fetchPhase,
            responseCollectorService,
            circuitBreakerService,
            indexSearcherExecutor
        );
    }

    /**
     * Creates a new the ScriptService. This method can be overwritten by tests to inject mock implementations.
     */
    protected ScriptService newScriptService(Settings settings, Map<String, ScriptEngine> engines, Map<String, ScriptContext<?>> contexts) {
        return new ScriptService(settings, engines, contexts);
    }

    /**
     * Get Custom Name Resolvers list based on a Discovery Plugins list
     * @param discoveryPlugins Discovery plugins list
     */
    private List<NetworkService.CustomNameResolver> getCustomNameResolvers(List<DiscoveryPlugin> discoveryPlugins) {
        List<NetworkService.CustomNameResolver> customNameResolvers = new ArrayList<>();
        for (DiscoveryPlugin discoveryPlugin : discoveryPlugins) {
            NetworkService.CustomNameResolver customNameResolver = discoveryPlugin.getCustomNameResolver(settings());
            if (customNameResolver != null) {
                customNameResolvers.add(customNameResolver);
            }
        }
        return customNameResolvers;
    }

    /** Constructs a ClusterInfoService which may be mocked for tests. */
    protected ClusterInfoService newClusterInfoService(
        Settings settings,
        ClusterService clusterService,
        ThreadPool threadPool,
        NodeClient client
    ) {
        final InternalClusterInfoService service = new InternalClusterInfoService(settings, clusterService, threadPool, client);
        if (DiscoveryNode.isClusterManagerNode(settings)) {
            // listen for state changes (this node starts/stops being the elected cluster manager, or new nodes are added)
            clusterService.addListener(service);
        }
        return service;
    }

    /** Constructs a {@link org.opensearch.http.HttpServerTransport} which may be mocked for tests. */
    protected HttpServerTransport newHttpTransport(NetworkModule networkModule) {
        return networkModule.getHttpServerTransportSupplier().get();
    }

    private static class LocalNodeFactory implements Function<BoundTransportAddress, DiscoveryNode> {
        private final SetOnce<DiscoveryNode> localNode = new SetOnce<>();
        private final String persistentNodeId;
        private final Settings settings;

        private LocalNodeFactory(Settings settings, String persistentNodeId) {
            this.persistentNodeId = persistentNodeId;
            this.settings = settings;
        }

        @Override
        public DiscoveryNode apply(BoundTransportAddress boundTransportAddress) {
            localNode.set(DiscoveryNode.createLocal(settings, boundTransportAddress.publishAddress(), persistentNodeId));
            return localNode.get();
        }

        DiscoveryNode getNode() {
            assert localNode.get() != null;
            return localNode.get();
        }
    }
}
