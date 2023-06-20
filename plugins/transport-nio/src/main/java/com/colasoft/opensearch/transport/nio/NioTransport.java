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

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.PlainActionFuture;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.indices.breaker.CircuitBreakerService;
import com.colasoft.opensearch.nio.BytesChannelContext;
import com.colasoft.opensearch.nio.ChannelFactory;
import com.colasoft.opensearch.nio.Config;
import com.colasoft.opensearch.nio.InboundChannelBuffer;
import com.colasoft.opensearch.nio.NioGroup;
import com.colasoft.opensearch.nio.NioSelector;
import com.colasoft.opensearch.nio.NioSocketChannel;
import com.colasoft.opensearch.nio.ServerChannelContext;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TcpTransport;
import com.colasoft.opensearch.transport.TransportSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.colasoft.opensearch.common.util.concurrent.ConcurrentCollections.newConcurrentMap;

public class NioTransport extends TcpTransport {

    private static final Logger logger = LogManager.getLogger(NioTransport.class);

    protected final PageAllocator pageAllocator;
    private final ConcurrentMap<String, TcpChannelFactory> profileToChannelFactory = newConcurrentMap();
    private final NioGroupFactory groupFactory;
    private volatile NioGroup nioGroup;
    private volatile Function<DiscoveryNode, TcpChannelFactory> clientChannelFactory;

    protected NioTransport(
        Settings settings,
        Version version,
        ThreadPool threadPool,
        NetworkService networkService,
        PageCacheRecycler pageCacheRecycler,
        NamedWriteableRegistry namedWriteableRegistry,
        CircuitBreakerService circuitBreakerService,
        NioGroupFactory groupFactory
    ) {
        super(settings, version, threadPool, pageCacheRecycler, circuitBreakerService, namedWriteableRegistry, networkService);
        this.pageAllocator = new PageAllocator(pageCacheRecycler);
        this.groupFactory = groupFactory;
    }

    @Override
    protected NioTcpServerChannel bind(String name, InetSocketAddress address) throws IOException {
        TcpChannelFactory channelFactory = this.profileToChannelFactory.get(name);
        NioTcpServerChannel serverChannel = nioGroup.bindServerChannel(address, channelFactory);
        PlainActionFuture<Void> future = PlainActionFuture.newFuture();
        serverChannel.addBindListener(ActionListener.toBiConsumer(future));
        future.actionGet();
        return serverChannel;
    }

    @Override
    protected NioTcpChannel initiateChannel(DiscoveryNode node) throws IOException {
        InetSocketAddress address = node.getAddress().address();
        return nioGroup.openChannel(address, clientChannelFactory.apply(node));
    }

    @Override
    protected void doStart() {
        boolean success = false;
        try {
            nioGroup = groupFactory.getTransportGroup();

            ProfileSettings clientProfileSettings = new ProfileSettings(settings, TransportSettings.DEFAULT_PROFILE);
            clientChannelFactory = clientChannelFactoryFunction(clientProfileSettings);

            if (NetworkService.NETWORK_SERVER.get(settings)) {
                // loop through all profiles and start them up, special handling for default one
                for (ProfileSettings profileSettings : profileSettings) {
                    String profileName = profileSettings.profileName;
                    TcpChannelFactory factory = serverChannelFactory(profileSettings);
                    profileToChannelFactory.putIfAbsent(profileName, factory);
                    bindServer(profileSettings);
                }
            }

            super.doStart();
            success = true;
        } catch (IOException e) {
            throw new OpenSearchException(e);
        } finally {
            if (success == false) {
                doStop();
            }
        }
    }

    @Override
    protected void stopInternal() {
        try {
            nioGroup.close();
        } catch (Exception e) {
            logger.warn("unexpected exception while stopping nio group", e);
        }
        profileToChannelFactory.clear();
    }

    protected void acceptChannel(NioSocketChannel channel) {
        serverAcceptedChannel((NioTcpChannel) channel);
    }

    protected TcpChannelFactory serverChannelFactory(ProfileSettings profileSettings) {
        return new TcpChannelFactoryImpl(profileSettings, false);
    }

    protected Function<DiscoveryNode, TcpChannelFactory> clientChannelFactoryFunction(ProfileSettings profileSettings) {
        return (n) -> new TcpChannelFactoryImpl(profileSettings, true);
    }

    protected abstract static class TcpChannelFactory extends ChannelFactory<NioTcpServerChannel, NioTcpChannel> {

        protected TcpChannelFactory(ProfileSettings profileSettings) {
            super(
                profileSettings.tcpNoDelay,
                profileSettings.tcpKeepAlive,
                profileSettings.tcpKeepIdle,
                profileSettings.tcpKeepInterval,
                profileSettings.tcpKeepCount,
                profileSettings.reuseAddress,
                Math.toIntExact(profileSettings.sendBufferSize.getBytes()),
                Math.toIntExact(profileSettings.receiveBufferSize.getBytes())
            );
        }
    }

    private class TcpChannelFactoryImpl extends TcpChannelFactory {

        private final boolean isClient;
        private final String profileName;

        private TcpChannelFactoryImpl(ProfileSettings profileSettings, boolean isClient) {
            super(profileSettings);
            this.isClient = isClient;
            this.profileName = profileSettings.profileName;
        }

        @Override
        public NioTcpChannel createChannel(NioSelector selector, SocketChannel channel, Config.Socket socketConfig) {
            NioTcpChannel nioChannel = new NioTcpChannel(isClient == false, profileName, channel);
            Consumer<Exception> exceptionHandler = (e) -> onException(nioChannel, e);
            TcpReadWriteHandler handler = new TcpReadWriteHandler(nioChannel, pageCacheRecycler, NioTransport.this);
            BytesChannelContext context = new BytesChannelContext(
                nioChannel,
                selector,
                socketConfig,
                exceptionHandler,
                handler,
                new InboundChannelBuffer(pageAllocator)
            );
            nioChannel.setContext(context);
            return nioChannel;
        }

        @Override
        public NioTcpServerChannel createServerChannel(
            NioSelector selector,
            ServerSocketChannel channel,
            Config.ServerSocket socketConfig
        ) {
            NioTcpServerChannel nioChannel = new NioTcpServerChannel(channel);
            Consumer<Exception> exceptionHandler = (e) -> onServerException(nioChannel, e);
            Consumer<NioSocketChannel> acceptor = NioTransport.this::acceptChannel;
            ServerChannelContext context = new ServerChannelContext(nioChannel, this, selector, socketConfig, acceptor, exceptionHandler);
            nioChannel.setContext(context);
            return nioChannel;
        }
    }
}
