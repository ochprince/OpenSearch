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

package com.colasoft.opensearch.test.transport;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.component.Lifecycle;
import com.colasoft.opensearch.common.component.LifecycleListener;
import com.colasoft.opensearch.common.transport.BoundTransportAddress;
import com.colasoft.opensearch.common.transport.TransportAddress;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.transport.ConnectionProfile;
import com.colasoft.opensearch.transport.RequestHandlerRegistry;
import com.colasoft.opensearch.transport.Transport;
import com.colasoft.opensearch.transport.TransportChannel;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportMessageListener;
import com.colasoft.opensearch.transport.TransportRequest;
import com.colasoft.opensearch.transport.TransportRequestHandler;
import com.colasoft.opensearch.transport.TransportRequestOptions;
import com.colasoft.opensearch.transport.TransportStats;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StubbableTransport implements Transport {

    private final ConcurrentHashMap<TransportAddress, SendRequestBehavior> sendBehaviors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TransportAddress, OpenConnectionBehavior> connectBehaviors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RequestHandlerRegistry<?>> replacedRequestRegistries = new ConcurrentHashMap<>();
    private volatile SendRequestBehavior defaultSendRequest = null;
    private volatile OpenConnectionBehavior defaultConnectBehavior = null;
    private final Transport delegate;

    public StubbableTransport(Transport transport) {
        this.delegate = transport;
    }

    boolean setDefaultSendBehavior(SendRequestBehavior sendBehavior) {
        SendRequestBehavior prior = defaultSendRequest;
        defaultSendRequest = sendBehavior;
        return prior == null;
    }

    public boolean setDefaultConnectBehavior(OpenConnectionBehavior openConnectionBehavior) {
        OpenConnectionBehavior prior = this.defaultConnectBehavior;
        this.defaultConnectBehavior = openConnectionBehavior;
        return prior == null;
    }

    boolean addSendBehavior(TransportAddress transportAddress, SendRequestBehavior sendBehavior) {
        return sendBehaviors.put(transportAddress, sendBehavior) == null;
    }

    boolean addConnectBehavior(TransportAddress transportAddress, OpenConnectionBehavior connectBehavior) {
        return connectBehaviors.put(transportAddress, connectBehavior) == null;
    }

    <Request extends TransportRequest> void addRequestHandlingBehavior(String actionName, RequestHandlingBehavior<Request> behavior) {
        final RequestHandlers requestHandlers = delegate.getRequestHandlers();
        final RequestHandlerRegistry<Request> realRegistry = requestHandlers.getHandler(actionName);
        if (realRegistry == null) {
            throw new IllegalStateException("Cannot find registered action for: " + actionName);
        }
        replacedRequestRegistries.put(actionName, realRegistry);
        final TransportRequestHandler<Request> realHandler = realRegistry.getHandler();
        final RequestHandlerRegistry<Request> newRegistry = RequestHandlerRegistry.replaceHandler(
            realRegistry,
            (request, channel, task) -> behavior.messageReceived(realHandler, request, channel, task)
        );
        requestHandlers.forceRegister(newRegistry);
    }

    void clearBehaviors() {
        clearOutboundBehaviors();
        clearInboundBehaviors();
    }

    void clearInboundBehaviors() {
        for (Map.Entry<String, RequestHandlerRegistry<?>> entry : replacedRequestRegistries.entrySet()) {
            getRequestHandlers().forceRegister(entry.getValue());
        }
        replacedRequestRegistries.clear();
    }

    void clearOutboundBehaviors() {
        this.defaultSendRequest = null;
        sendBehaviors.clear();
        this.defaultConnectBehavior = null;
        connectBehaviors.clear();
    }

    void clearOutboundBehaviors(TransportAddress transportAddress) {
        SendRequestBehavior behavior = sendBehaviors.remove(transportAddress);
        if (behavior != null) {
            behavior.clearCallback();
        }
        OpenConnectionBehavior openConnectionBehavior = connectBehaviors.remove(transportAddress);
        if (openConnectionBehavior != null) {
            openConnectionBehavior.clearCallback();
        }
    }

    Transport getDelegate() {
        return delegate;
    }

    @Override
    public void setMessageListener(TransportMessageListener listener) {
        delegate.setMessageListener(listener);
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return delegate.boundAddress();
    }

    @Override
    public TransportAddress[] addressesFromString(String address) throws UnknownHostException {
        return delegate.addressesFromString(address);
    }

    @Override
    public List<String> getDefaultSeedAddresses() {
        return delegate.getDefaultSeedAddresses();
    }

    @Override
    public void openConnection(DiscoveryNode node, ConnectionProfile profile, ActionListener<Connection> listener) {
        TransportAddress address = node.getAddress();
        OpenConnectionBehavior behavior = connectBehaviors.getOrDefault(address, defaultConnectBehavior);

        ActionListener<Connection> wrappedListener = ActionListener.delegateFailure(
            listener,
            (delegatedListener, connection) -> delegatedListener.onResponse(new WrappedConnection(connection))
        );

        if (behavior == null) {
            delegate.openConnection(node, profile, wrappedListener);
        } else {
            behavior.openConnection(delegate, node, profile, wrappedListener);
        }
    }

    @Override
    public TransportStats getStats() {
        return delegate.getStats();
    }

    @Override
    public Transport.ResponseHandlers getResponseHandlers() {
        return delegate.getResponseHandlers();
    }

    @Override
    public RequestHandlers getRequestHandlers() {
        return delegate.getRequestHandlers();
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return delegate.lifecycleState();
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        delegate.addLifecycleListener(listener);
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        delegate.removeLifecycleListener(listener);
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Map<String, BoundTransportAddress> profileBoundAddresses() {
        return delegate.profileBoundAddresses();
    }

    public class WrappedConnection implements Transport.Connection {

        private final Transport.Connection connection;

        private WrappedConnection(Transport.Connection connection) {
            this.connection = connection;
        }

        @Override
        public DiscoveryNode getNode() {
            return connection.getNode();
        }

        @Override
        public void sendRequest(long requestId, String action, TransportRequest request, TransportRequestOptions options)
            throws IOException, TransportException {
            TransportAddress address = connection.getNode().getAddress();
            SendRequestBehavior behavior = sendBehaviors.getOrDefault(address, defaultSendRequest);
            if (behavior == null) {
                connection.sendRequest(requestId, action, request, options);
            } else {
                behavior.sendRequest(connection, requestId, action, request, options);
            }
        }

        @Override
        public void addCloseListener(ActionListener<Void> listener) {
            connection.addCloseListener(listener);
        }

        @Override
        public boolean isClosed() {
            return connection.isClosed();
        }

        @Override
        public Version getVersion() {
            return connection.getVersion();
        }

        @Override
        public Object getCacheKey() {
            return connection.getCacheKey();
        }

        @Override
        public void close() {
            connection.close();
        }

        public Transport.Connection getConnection() {
            return connection;
        }
    }

    @FunctionalInterface
    public interface OpenConnectionBehavior {

        void openConnection(
            Transport transport,
            DiscoveryNode discoveryNode,
            ConnectionProfile profile,
            ActionListener<Connection> listener
        );

        default void clearCallback() {}
    }

    @FunctionalInterface
    public interface SendRequestBehavior {
        void sendRequest(Connection connection, long requestId, String action, TransportRequest request, TransportRequestOptions options)
            throws IOException;

        default void clearCallback() {}
    }

    @FunctionalInterface
    public interface RequestHandlingBehavior<Request extends TransportRequest> {

        void messageReceived(TransportRequestHandler<Request> handler, Request request, TransportChannel channel, Task task)
            throws Exception;

        default void clearCallback() {}
    }
}
