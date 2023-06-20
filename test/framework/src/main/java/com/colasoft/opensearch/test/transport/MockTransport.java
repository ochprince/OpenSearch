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

import com.colasoft.opensearch.cluster.ClusterModule;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.Randomness;
import com.colasoft.opensearch.common.collect.Tuple;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.transport.BoundTransportAddress;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.CloseableConnection;
import com.colasoft.opensearch.transport.ClusterConnectionManager;
import com.colasoft.opensearch.transport.RemoteTransportException;
import com.colasoft.opensearch.transport.SendRequestTransportException;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportInterceptor;
import com.colasoft.opensearch.transport.TransportMessageListener;
import com.colasoft.opensearch.transport.TransportRequest;
import com.colasoft.opensearch.transport.TransportRequestOptions;
import com.colasoft.opensearch.transport.TransportResponse;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static org.apache.lucene.tests.util.LuceneTestCase.rarely;

/**
 * A basic transport implementation that allows to intercept requests that have been sent
 */
public class MockTransport extends StubbableTransport {

    private TransportMessageListener listener;
    private ConcurrentMap<Long, Tuple<DiscoveryNode, String>> requests = new ConcurrentHashMap<>();

    public TransportService createTransportService(
        Settings settings,
        ThreadPool threadPool,
        TransportInterceptor interceptor,
        Function<BoundTransportAddress, DiscoveryNode> localNodeFactory,
        @Nullable ClusterSettings clusterSettings,
        Set<String> taskHeaders
    ) {
        StubbableConnectionManager connectionManager = new StubbableConnectionManager(new ClusterConnectionManager(settings, this));
        connectionManager.setDefaultNodeConnectedBehavior((cm, node) -> false);
        connectionManager.setDefaultGetConnectionBehavior((cm, discoveryNode) -> createConnection(discoveryNode));
        return new TransportService(
            settings,
            this,
            threadPool,
            interceptor,
            localNodeFactory,
            clusterSettings,
            taskHeaders,
            connectionManager
        );
    }

    public MockTransport() {
        super(new FakeTransport());
        setDefaultConnectBehavior((transport, discoveryNode, profile, listener) -> listener.onResponse(createConnection(discoveryNode)));
    }

    /**
     * simulate a response for the given requestId
     */
    @SuppressWarnings("unchecked")
    public <Response extends TransportResponse> void handleResponse(final long requestId, final Response response) {
        final TransportResponseHandler<Response> transportResponseHandler = (TransportResponseHandler<Response>) getResponseHandlers()
            .onResponseReceived(requestId, listener);
        if (transportResponseHandler != null) {
            final Response deliveredResponse;
            try (BytesStreamOutput output = new BytesStreamOutput()) {
                response.writeTo(output);
                deliveredResponse = transportResponseHandler.read(
                    new NamedWriteableAwareStreamInput(output.bytes().streamInput(), writeableRegistry())
                );
            } catch (IOException | UnsupportedOperationException e) {
                throw new AssertionError("failed to serialize/deserialize response " + response, e);
            }
            transportResponseHandler.handleResponse(deliveredResponse);
        }
    }

    /**
     * simulate a local error for the given requestId, will be wrapped
     * by a {@link SendRequestTransportException}
     *
     * @param requestId the id corresponding to the captured send
     *                  request
     * @param t         the failure to wrap
     */
    public void handleLocalError(final long requestId, final Throwable t) {
        Tuple<DiscoveryNode, String> request = requests.get(requestId);
        assert request != null;
        this.handleError(requestId, new SendRequestTransportException(request.v1(), request.v2(), t));
    }

    /**
     * simulate a remote error for the given requestId, will be wrapped
     * by a {@link RemoteTransportException}
     *
     * @param requestId the id corresponding to the captured send
     *                  request
     * @param t         the failure to wrap
     */
    public void handleRemoteError(final long requestId, final Throwable t) {
        final RemoteTransportException remoteException;
        if (rarely(Randomness.get())) {
            remoteException = new RemoteTransportException("remote failure, coming from local node", t);
        } else {
            try (BytesStreamOutput output = new BytesStreamOutput()) {
                output.writeException(t);
                remoteException = new RemoteTransportException("remote failure", output.bytes().streamInput().readException());
            } catch (IOException ioException) {
                throw new AssertionError("failed to serialize/deserialize supplied exception " + t, ioException);
            }
        }
        this.handleError(requestId, remoteException);
    }

    /**
     * simulate an error for the given requestId, unlike
     * {@link #handleLocalError(long, Throwable)} and
     * {@link #handleRemoteError(long, Throwable)}, the provided
     * exception will not be wrapped but will be delivered to the
     * transport layer as is
     *
     * @param requestId the id corresponding to the captured send
     *                  request
     * @param e         the failure
     */
    public void handleError(final long requestId, final TransportException e) {
        final TransportResponseHandler transportResponseHandler = getResponseHandlers().onResponseReceived(requestId, listener);
        if (transportResponseHandler != null) {
            transportResponseHandler.handleException(e);
        }
    }

    public Connection createConnection(DiscoveryNode node) {
        return new CloseableConnection() {
            @Override
            public DiscoveryNode getNode() {
                return node;
            }

            @Override
            public void sendRequest(long requestId, String action, TransportRequest request, TransportRequestOptions options)
                throws TransportException {
                requests.put(requestId, Tuple.tuple(node, action));
                onSendRequest(requestId, action, request, node, options);
            }
        };
    }

    protected void onSendRequest(long requestId, String action, TransportRequest request, DiscoveryNode node) {}

    protected void onSendRequest(
        long requestId,
        String action,
        TransportRequest request,
        DiscoveryNode node,
        TransportRequestOptions options
    ) {
        onSendRequest(requestId, action, request, node);
    }

    @Override
    public void setMessageListener(TransportMessageListener listener) {
        if (this.listener != null) {
            throw new IllegalStateException("listener already set");
        }
        this.listener = listener;
        super.setMessageListener(listener);
    }

    protected NamedWriteableRegistry writeableRegistry() {
        return new NamedWriteableRegistry(ClusterModule.getNamedWriteables());
    }
}
