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

package org.elasticsearch.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SniffingConnectionPool extends AbstractStaticConnectionPool {

    private static final Log logger = LogFactory.getLog(SniffingConnectionPool.class);

    private final boolean sniffOnFailure;
    private final Sniffer sniffer;
    private volatile List<StatefulConnection> connections;
    private final SnifferTask snifferTask;

    public SniffingConnectionPool(int sniffInterval, boolean sniffOnFailure, int sniffAfterFailureDelay,
                                  CloseableHttpClient client, RequestConfig sniffRequestConfig, int sniffRequestTimeout, Scheme scheme,
                                  Predicate<Connection> connectionSelector, Node... nodes) {
        super(connectionSelector);
        if (sniffInterval <= 0) {
            throw new IllegalArgumentException("sniffInterval must be greater than 0");
        }
        if (sniffAfterFailureDelay <= 0) {
            throw new IllegalArgumentException("sniffAfterFailureDelay must be greater than 0");
        }
        Objects.requireNonNull(scheme, "scheme cannot be null");
        if (nodes == null || nodes.length == 0) {
            throw new IllegalArgumentException("no nodes provided");
        }
        this.sniffOnFailure = sniffOnFailure;
        this.sniffer = new Sniffer(client, sniffRequestConfig, sniffRequestTimeout, scheme.toString());
        this.connections = createConnections(nodes);
        this.snifferTask = new SnifferTask(sniffInterval, sniffAfterFailureDelay);
    }

    @Override
    protected List<StatefulConnection> getConnections() {
        return this.connections;
    }

    @Override
    public void beforeAttempt(StatefulConnection connection) throws IOException {

    }

    @Override
    public void onFailure(StatefulConnection connection) throws IOException {
        super.onFailure(connection);
        if (sniffOnFailure) {
            //re-sniff immediately but take out the node that failed
            snifferTask.sniffOnFailure(connection.getNode());
        }
    }

    @Override
    public void close() throws IOException {
        snifferTask.shutdown();
    }

    public enum Scheme {
        HTTP, HTTPS;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private class SnifferTask implements Runnable {
        private final int sniffInterval;
        private final int sniffAfterFailureDelay;
        private final ScheduledExecutorService scheduledExecutorService;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private volatile boolean failure = false;
        private volatile ScheduledFuture<?> scheduledFuture;

        private SnifferTask(int sniffInterval, int sniffAfterFailureDelay) {
            this.sniffInterval = sniffInterval;
            this.sniffAfterFailureDelay = sniffAfterFailureDelay;
            this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
            this.scheduledFuture = this.scheduledExecutorService.schedule(this, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            sniff(node -> true);
        }

        void sniffOnFailure(Node failedNode) {
            //sync sniff straightaway on failure
            failure = true;
            sniff(node -> node.getHttpHost().equals(failedNode.getHttpHost()) == false);
        }

        void sniff(Predicate<Node> nodeFilter) {
            if (running.compareAndSet(false, true)) {
                try {
                    Iterator<StatefulConnection> connectionIterator = nextUnfilteredConnection().iterator();
                    if (connectionIterator.hasNext()) {
                        sniff(connectionIterator, nodeFilter);
                    } else {
                        StatefulConnection connection = lastResortConnection();
                        logger.info("no healthy nodes available, trying " + connection.getNode());
                        sniff(Stream.of(connection).iterator(), nodeFilter);
                    }
                } catch (Throwable t) {
                    logger.error("error while sniffing nodes", t);
                } finally {
                    try {
                        //regardless of whether and when the next sniff is scheduled, cancel it and schedule a new one with updated delay
                        this.scheduledFuture.cancel(false);
                        if (this.failure) {
                            this.scheduledFuture = this.scheduledExecutorService.schedule(this,
                                    sniffAfterFailureDelay, TimeUnit.MILLISECONDS);
                            this.failure = false;
                        } else {
                            this.scheduledFuture = this.scheduledExecutorService.schedule(this, sniffInterval, TimeUnit.MILLISECONDS);
                        }
                    } catch (Throwable t) {
                        logger.error("error while scheduling next sniffer task", t);
                    } finally {
                        running.set(false);
                    }
                }
            }
        }

        void sniff(Iterator<StatefulConnection> connectionIterator, Predicate<Node> nodeFilter) throws IOException {
            IOException lastSeenException = null;
            while (connectionIterator.hasNext()) {
                StatefulConnection connection = connectionIterator.next();
                try {
                    List<Node> sniffedNodes = sniffer.sniffNodes(connection.getNode());
                    Node[] filteredNodes = sniffedNodes.stream().filter(nodeFilter).toArray(Node[]::new);
                    logger.debug("adding " + filteredNodes.length + " nodes out of " + sniffedNodes.size() + " sniffed nodes");
                    connections = createConnections(filteredNodes);
                    onSuccess(connection);
                    return;
                } catch (IOException e) {
                    //here we have control over the request, if it fails something is really wrong, always call onFailure
                    onFailure(connection);
                    if (lastSeenException != null) {
                        e.addSuppressed(lastSeenException);
                    }
                    lastSeenException = e;
                }
            }
            logger.warn("failed to sniff nodes", lastSeenException);
        }

        void shutdown() {
            scheduledExecutorService.shutdown();
            try {
                if (scheduledExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduledExecutorService.shutdownNow();
        }
    }
}
