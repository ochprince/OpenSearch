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

package com.colasoft.opensearch.transport.nio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.common.SetOnce;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.common.util.concurrent.OpenSearchExecutors;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.http.HttpServerTransport;
import com.colasoft.opensearch.http.nio.NioHttpServerTransport;
import com.colasoft.opensearch.indices.breaker.CircuitBreakerService;
import com.colasoft.opensearch.plugins.NetworkPlugin;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.Transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.colasoft.opensearch.common.settings.Setting.intSetting;

public class NioTransportPlugin extends Plugin implements NetworkPlugin {

    public static final String NIO_TRANSPORT_NAME = "nio-transport";
    public static final String NIO_HTTP_TRANSPORT_NAME = "nio-http-transport";

    private static final Logger logger = LogManager.getLogger(NioTransportPlugin.class);

    public static final Setting<Integer> NIO_WORKER_COUNT = new Setting<>(
        "transport.nio.worker_count",
        (s) -> Integer.toString(OpenSearchExecutors.allocatedProcessors(s)),
        (s) -> Setting.parseInt(s, 1, "transport.nio.worker_count"),
        Setting.Property.NodeScope
    );
    public static final Setting<Integer> NIO_HTTP_WORKER_COUNT = intSetting("http.nio.worker_count", 0, 0, Setting.Property.NodeScope);

    private final SetOnce<NioGroupFactory> groupFactory = new SetOnce<>();

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(NIO_HTTP_WORKER_COUNT, NIO_WORKER_COUNT);
    }

    @Override
    public Map<String, Supplier<Transport>> getTransports(
        Settings settings,
        ThreadPool threadPool,
        PageCacheRecycler pageCacheRecycler,
        CircuitBreakerService circuitBreakerService,
        NamedWriteableRegistry namedWriteableRegistry,
        NetworkService networkService
    ) {
        return Collections.singletonMap(
            NIO_TRANSPORT_NAME,
            () -> new NioTransport(
                settings,
                Version.CURRENT,
                threadPool,
                networkService,
                pageCacheRecycler,
                namedWriteableRegistry,
                circuitBreakerService,
                getNioGroupFactory(settings)
            )
        );
    }

    @Override
    public Map<String, Supplier<HttpServerTransport>> getHttpTransports(
        Settings settings,
        ThreadPool threadPool,
        BigArrays bigArrays,
        PageCacheRecycler pageCacheRecycler,
        CircuitBreakerService circuitBreakerService,
        NamedXContentRegistry xContentRegistry,
        NetworkService networkService,
        HttpServerTransport.Dispatcher dispatcher,
        ClusterSettings clusterSettings
    ) {
        return Collections.singletonMap(
            NIO_HTTP_TRANSPORT_NAME,
            () -> new NioHttpServerTransport(
                settings,
                networkService,
                bigArrays,
                pageCacheRecycler,
                threadPool,
                xContentRegistry,
                dispatcher,
                getNioGroupFactory(settings),
                clusterSettings
            )
        );
    }

    private synchronized NioGroupFactory getNioGroupFactory(Settings settings) {
        NioGroupFactory nioGroupFactory = groupFactory.get();
        if (nioGroupFactory != null) {
            assert nioGroupFactory.getSettings().equals(settings) : "Different settings than originally provided";
            return nioGroupFactory;
        } else {
            groupFactory.set(new NioGroupFactory(settings, logger));
            return groupFactory.get();
        }
    }
}
