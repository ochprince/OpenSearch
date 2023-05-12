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

package com.colasoft.opensearch.http;

import com.colasoft.opensearch.common.network.NetworkModule;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.transport.Netty4Plugin;
import com.colasoft.opensearch.transport.nio.MockNioTransportPlugin;
import com.colasoft.opensearch.transport.nio.NioTransportPlugin;
import org.junit.BeforeClass;

import java.util.Arrays;
import java.util.Collection;

public abstract class HttpSmokeTestCase extends OpenSearchIntegTestCase {

    private static String nodeTransportTypeKey;
    private static String nodeHttpTypeKey;
    private static String clientTypeKey;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void setUpTransport() {
        nodeTransportTypeKey = getTypeKey(randomFrom(getTestTransportPlugin(), Netty4Plugin.class, NioTransportPlugin.class));
        nodeHttpTypeKey = getHttpTypeKey(randomFrom(Netty4Plugin.class, NioTransportPlugin.class));
        clientTypeKey = getTypeKey(randomFrom(getTestTransportPlugin(), Netty4Plugin.class, NioTransportPlugin.class));
    }

    private static String getTypeKey(Class<? extends Plugin> clazz) {
        if (clazz.equals(MockNioTransportPlugin.class)) {
            return MockNioTransportPlugin.MOCK_NIO_TRANSPORT_NAME;
        } else if (clazz.equals(NioTransportPlugin.class)) {
            return NioTransportPlugin.NIO_TRANSPORT_NAME;
        } else {
            assert clazz.equals(Netty4Plugin.class);
            return Netty4Plugin.NETTY_TRANSPORT_NAME;
        }
    }

    private static String getHttpTypeKey(Class<? extends Plugin> clazz) {
        if (clazz.equals(NioTransportPlugin.class)) {
            return NioTransportPlugin.NIO_HTTP_TRANSPORT_NAME;
        } else {
            assert clazz.equals(Netty4Plugin.class);
            return Netty4Plugin.NETTY_HTTP_TRANSPORT_NAME;
        }
    }

    @Override
    protected boolean addMockHttpTransport() {
        return false; // enable http
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.TRANSPORT_TYPE_KEY, nodeTransportTypeKey)
                .put(NetworkModule.HTTP_TYPE_KEY, nodeHttpTypeKey).build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(getTestTransportPlugin(), Netty4Plugin.class, NioTransportPlugin.class);
    }

    @Override
    protected boolean ignoreExternalCluster() {
        return true;
    }

}
