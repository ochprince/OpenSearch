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

package com.colasoft.opensearch.node;

import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.ClusterInfoService;
import com.colasoft.opensearch.cluster.MockInternalClusterInfoService;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.network.NetworkModule;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.transport.BoundTransportAddress;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.common.util.MockBigArrays;
import com.colasoft.opensearch.common.util.MockPageCacheRecycler;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.http.HttpServerTransport;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.breaker.CircuitBreakerService;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.script.MockScriptService;
import com.colasoft.opensearch.script.ScriptContext;
import com.colasoft.opensearch.script.ScriptEngine;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.MockSearchService;
import com.colasoft.opensearch.search.SearchService;
import com.colasoft.opensearch.search.fetch.FetchPhase;
import com.colasoft.opensearch.search.query.QueryPhase;
import com.colasoft.opensearch.test.MockHttpTransport;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.Transport;
import com.colasoft.opensearch.transport.TransportInterceptor;
import com.colasoft.opensearch.transport.TransportService;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * A node for testing which allows:
 * <ul>
 *   <li>Overriding Version.CURRENT</li>
 *   <li>Adding test plugins that exist on the classpath</li>
 * </ul>
 */
public class MockNode extends Node {

    private final Collection<Class<? extends Plugin>> classpathPlugins;

    public MockNode(final Settings settings, final Collection<Class<? extends Plugin>> classpathPlugins) {
        this(settings, classpathPlugins, true);
    }

    public MockNode(
        final Settings settings,
        final Collection<Class<? extends Plugin>> classpathPlugins,
        final boolean forbidPrivateIndexSettings
    ) {
        this(settings, classpathPlugins, null, forbidPrivateIndexSettings);
    }

    public MockNode(
        final Settings settings,
        final Collection<Class<? extends Plugin>> classpathPlugins,
        final Path configPath,
        final boolean forbidPrivateIndexSettings
    ) {
        this(
            InternalSettingsPreparer.prepareEnvironment(settings, Collections.emptyMap(), configPath, () -> "mock_ node"),
            classpathPlugins,
            forbidPrivateIndexSettings
        );
    }

    private MockNode(
        final Environment environment,
        final Collection<Class<? extends Plugin>> classpathPlugins,
        final boolean forbidPrivateIndexSettings
    ) {
        super(environment, classpathPlugins, forbidPrivateIndexSettings);
        this.classpathPlugins = classpathPlugins;
    }

    /**
     * The classpath plugins this node was constructed with.
     */
    public Collection<Class<? extends Plugin>> getClasspathPlugins() {
        return classpathPlugins;
    }

    @Override
    protected BigArrays createBigArrays(PageCacheRecycler pageCacheRecycler, CircuitBreakerService circuitBreakerService) {
        if (getPluginsService().filterPlugins(NodeMocksPlugin.class).isEmpty()) {
            return super.createBigArrays(pageCacheRecycler, circuitBreakerService);
        }
        return new MockBigArrays(pageCacheRecycler, circuitBreakerService);
    }

    @Override
    PageCacheRecycler createPageCacheRecycler(Settings settings) {
        if (getPluginsService().filterPlugins(NodeMocksPlugin.class).isEmpty()) {
            return super.createPageCacheRecycler(settings);
        }
        return new MockPageCacheRecycler(settings);
    }

    @Override
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
        if (getPluginsService().filterPlugins(MockSearchService.TestPlugin.class).isEmpty()) {
            return super.newSearchService(
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
        return new MockSearchService(
            clusterService,
            indicesService,
            threadPool,
            scriptService,
            bigArrays,
            queryPhase,
            fetchPhase,
            circuitBreakerService
        );
    }

    @Override
    protected ScriptService newScriptService(Settings settings, Map<String, ScriptEngine> engines, Map<String, ScriptContext<?>> contexts) {
        if (getPluginsService().filterPlugins(MockScriptService.TestPlugin.class).isEmpty()) {
            return super.newScriptService(settings, engines, contexts);
        }
        return new MockScriptService(settings, engines, contexts);
    }

    @Override
    protected TransportService newTransportService(
        Settings settings,
        Transport transport,
        ThreadPool threadPool,
        TransportInterceptor interceptor,
        Function<BoundTransportAddress, DiscoveryNode> localNodeFactory,
        ClusterSettings clusterSettings,
        Set<String> taskHeaders
    ) {
        // we use the MockTransportService.TestPlugin class as a marker to create a network
        // module with this MockNetworkService. NetworkService is such an integral part of the systme
        // we don't allow to plug it in from plugins or anything. this is a test-only override and
        // can't be done in a production env.
        if (getPluginsService().filterPlugins(MockTransportService.TestPlugin.class).isEmpty()) {
            return super.newTransportService(settings, transport, threadPool, interceptor, localNodeFactory, clusterSettings, taskHeaders);
        } else {
            return new MockTransportService(settings, transport, threadPool, interceptor, localNodeFactory, clusterSettings, taskHeaders);
        }
    }

    @Override
    protected void processRecoverySettings(ClusterSettings clusterSettings, RecoverySettings recoverySettings) {
        if (false == getPluginsService().filterPlugins(RecoverySettingsChunkSizePlugin.class).isEmpty()) {
            clusterSettings.addSettingsUpdateConsumer(RecoverySettingsChunkSizePlugin.CHUNK_SIZE_SETTING, recoverySettings::setChunkSize);
        }
    }

    @Override
    protected ClusterInfoService newClusterInfoService(
        Settings settings,
        ClusterService clusterService,
        ThreadPool threadPool,
        NodeClient client
    ) {
        if (getPluginsService().filterPlugins(MockInternalClusterInfoService.TestPlugin.class).isEmpty()) {
            return super.newClusterInfoService(settings, clusterService, threadPool, client);
        } else {
            final MockInternalClusterInfoService service = new MockInternalClusterInfoService(settings, clusterService, threadPool, client);
            clusterService.addListener(service);
            return service;
        }
    }

    @Override
    protected HttpServerTransport newHttpTransport(NetworkModule networkModule) {
        if (getPluginsService().filterPlugins(MockHttpTransport.TestPlugin.class).isEmpty()) {
            return super.newHttpTransport(networkModule);
        } else {
            return new MockHttpTransport();
        }
    }

    @Override
    protected void configureNodeAndClusterIdStateListener(ClusterService clusterService) {
        // do not configure this in tests as this is causing SetOnce to throw exceptions when jvm is used for multiple tests
    }

    public NamedWriteableRegistry getNamedWriteableRegistry() {
        return namedWriteableRegistry;
    }
}
