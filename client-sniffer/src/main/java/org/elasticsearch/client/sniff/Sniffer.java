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

package org.elasticsearch.client.sniff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class responsible for sniffing nodes from an elasticsearch cluster and setting them to a provided instance of {@link RestClient}.
 * Must be created via {@link Builder}, which allows to set all of the different options or rely on defaults.
 * A background task fetches the nodes from elasticsearch and updates them periodically.
 * Supports sniffing on failure, meaning that the client will notify the sniffer at each host failure, so that nodes can be updated
 * straightaway.
 */
public final class Sniffer extends RestClient.FailureListener implements Closeable {

    private static final Log logger = LogFactory.getLog(Sniffer.class);

    private final boolean sniffOnFailure;
    private final Task task;

    private Sniffer(RestClient restClient, HostsSniffer hostsSniffer, long sniffInterval,
                    boolean sniffOnFailure, long sniffAfterFailureDelay) {
        this.task = new Task(hostsSniffer, restClient, sniffInterval, sniffAfterFailureDelay);
        this.sniffOnFailure = sniffOnFailure;
        restClient.setFailureListener(this);
    }

    @Override
    public void onFailure(HttpHost host) throws IOException {
        if (sniffOnFailure) {
            //re-sniff immediately but take out the node that failed
            task.sniffOnFailure(host);
        }
    }

    @Override
    public void close() throws IOException {
        task.shutdown();
    }

    private static class Task implements Runnable {
        private final HostsSniffer hostsSniffer;
        private final RestClient restClient;

        private final long sniffIntervalMillis;
        private final long sniffAfterFailureDelayMillis;
        private final ScheduledExecutorService scheduledExecutorService;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private ScheduledFuture<?> scheduledFuture;

        private Task(HostsSniffer hostsSniffer, RestClient restClient, long sniffIntervalMillis, long sniffAfterFailureDelayMillis) {
            this.hostsSniffer = hostsSniffer;
            this.restClient = restClient;
            this.sniffIntervalMillis = sniffIntervalMillis;
            this.sniffAfterFailureDelayMillis = sniffAfterFailureDelayMillis;
            this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduleNextRun(0);
        }

        synchronized void scheduleNextRun(long delayMillis) {
            if (scheduledExecutorService.isShutdown() == false) {
                try {
                    if (scheduledFuture != null) {
                        //regardless of when the next sniff is scheduled, cancel it and schedule a new one with updated delay
                        this.scheduledFuture.cancel(false);
                    }
                    logger.debug("scheduling next sniff in " + delayMillis + " ms");
                    this.scheduledFuture = this.scheduledExecutorService.schedule(this, delayMillis, TimeUnit.MILLISECONDS);
                } catch(Exception e) {
                    logger.error("error while scheduling next sniffer task", e);
                }
            }
        }

        @Override
        public void run() {
            sniff(null, sniffIntervalMillis);
        }

        void sniffOnFailure(HttpHost failedHost) {
            sniff(failedHost, sniffAfterFailureDelayMillis);
        }

        void sniff(HttpHost excludeHost, long nextSniffDelayMillis) {
            if (running.compareAndSet(false, true)) {
                try {
                    List<HttpHost> sniffedNodes = hostsSniffer.sniffHosts();
                    if (excludeHost != null) {
                        sniffedNodes.remove(excludeHost);
                    }
                    logger.debug("sniffed nodes: " + sniffedNodes);
                    this.restClient.setHosts(sniffedNodes.toArray(new HttpHost[sniffedNodes.size()]));
                } catch (Exception e) {
                    logger.error("error while sniffing nodes", e);
                } finally {
                    scheduleNextRun(nextSniffDelayMillis);
                    running.set(false);
                }
            }
        }

        synchronized void shutdown() {
            scheduledExecutorService.shutdown();
            try {
                if (scheduledExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    return;
                }
                scheduledExecutorService.shutdownNow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns a new {@link Builder} to help with {@link Sniffer} creation.
     */
    public static Builder builder(RestClient restClient, HostsSniffer hostsSniffer) {
        return new Builder(restClient, hostsSniffer);
    }

    /**
     * Sniffer builder. Helps creating a new {@link Sniffer}.
     */
    public static final class Builder {
        public static final long DEFAULT_SNIFF_INTERVAL = TimeUnit.MINUTES.toMillis(5);
        public static final long DEFAULT_SNIFF_AFTER_FAILURE_DELAY = TimeUnit.MINUTES.toMillis(1);

        private final RestClient restClient;
        private final HostsSniffer hostsSniffer;
        private long sniffIntervalMillis = DEFAULT_SNIFF_INTERVAL;
        private boolean sniffOnFailure = true;
        private long sniffAfterFailureDelayMillis = DEFAULT_SNIFF_AFTER_FAILURE_DELAY;

        /**
         * Creates a new builder instance by providing the {@link RestClient} that will be used to communicate with elasticsearch,
         * and the
         */
        private Builder(RestClient restClient, HostsSniffer hostsSniffer) {
            Objects.requireNonNull(restClient, "restClient cannot be null");
            this.restClient = restClient;
            Objects.requireNonNull(hostsSniffer, "hostsSniffer cannot be null");
            this.hostsSniffer = hostsSniffer;
        }

        /**
         * Sets the interval between consecutive ordinary sniff executions in milliseconds. Will be honoured when
         * sniffOnFailure is disabled or when there are no failures between consecutive sniff executions.
         * @throws IllegalArgumentException if sniffIntervalMillis is not greater than 0
         */
        public Builder setSniffIntervalMillis(int sniffIntervalMillis) {
            if (sniffIntervalMillis <= 0) {
                throw new IllegalArgumentException("sniffIntervalMillis must be greater than 0");
            }
            this.sniffIntervalMillis = sniffIntervalMillis;
            return this;
        }

        /**
         * Enables/disables sniffing on failure. If enabled, at each failure nodes will be reloaded, and a new sniff execution will
         * be scheduled after a shorter time than usual (sniffAfterFailureDelayMillis).
         */
        public Builder setSniffOnFailure(boolean sniffOnFailure) {
            this.sniffOnFailure = sniffOnFailure;
            return this;
        }

        /**
         * Sets the delay of a sniff execution scheduled after a failure (in milliseconds)
         */
        public Builder setSniffAfterFailureDelayMillis(int sniffAfterFailureDelayMillis) {
            if (sniffAfterFailureDelayMillis <= 0) {
                throw new IllegalArgumentException("sniffAfterFailureDelayMillis must be greater than 0");
            }
            this.sniffAfterFailureDelayMillis = sniffAfterFailureDelayMillis;
            return this;
        }

        /**
         * Creates the {@link Sniffer} based on the provided configuration.
         */
        public Sniffer build() {
            return new Sniffer(restClient, hostsSniffer, sniffIntervalMillis, sniffOnFailure, sniffAfterFailureDelayMillis);
        }
    }
}
