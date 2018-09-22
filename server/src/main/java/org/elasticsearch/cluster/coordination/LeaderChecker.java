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

package org.elasticsearch.cluster.coordination;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool.Names;
import org.elasticsearch.transport.ConnectTransportException;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportException;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.transport.TransportRequestOptions;
import org.elasticsearch.transport.TransportRequestOptions.Type;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponse.Empty;
import org.elasticsearch.transport.TransportResponseHandler;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The LeaderChecker is responsible for allowing followers to check that the currently elected leader is still connected and healthy. We are
 * fairly lenient, possibly allowing multiple checks to fail before considering the leader to be faulty, to allow for the leader to
 * temporarily stand down on occasion, e.g. if it needs to move to a higher term. On deciding that the leader has failed a follower will
 * become a candidate and attempt to become a leader itself.
 */
public class LeaderChecker extends AbstractComponent {

    public static final String LEADER_CHECK_ACTION_NAME = "internal:coordination/fault_detection/leader_check";

    // the time between checks sent to the leader
    public static final Setting<TimeValue> LEADER_CHECK_INTERVAL_SETTING =
        Setting.timeSetting("cluster.fault_detection.leader_check.interval",
            TimeValue.timeValueMillis(1000), TimeValue.timeValueMillis(100), Setting.Property.NodeScope);

    // the timeout for each check sent to the leader
    public static final Setting<TimeValue> LEADER_CHECK_TIMEOUT_SETTING =
        Setting.timeSetting("cluster.fault_detection.leader_check.timeout",
            TimeValue.timeValueMillis(30000), TimeValue.timeValueMillis(1), Setting.Property.NodeScope);

    // the number of failed checks that must happen before the leader is considered to have failed.
    public static final Setting<Integer> LEADER_CHECK_RETRY_COUNT_SETTING =
        Setting.intSetting("cluster.fault_detection.leader_check.retry_count", 3, 1, Setting.Property.NodeScope);

    private final TimeValue leaderCheckInterval;
    private final TimeValue leaderCheckTimeout;
    private final int leaderCheckRetryCount;
    private final TransportService transportService;
    private final Runnable onLeaderFailure;

    private volatile DiscoveryNodes lastPublishedDiscoveryNodes;

    public LeaderChecker(final Settings settings, final TransportService transportService, final Runnable onLeaderFailure) {
        super(settings);
        leaderCheckInterval = LEADER_CHECK_INTERVAL_SETTING.get(settings);
        leaderCheckTimeout = LEADER_CHECK_TIMEOUT_SETTING.get(settings);
        leaderCheckRetryCount = LEADER_CHECK_RETRY_COUNT_SETTING.get(settings);
        this.transportService = transportService;
        this.onLeaderFailure = onLeaderFailure;

        transportService.registerRequestHandler(LEADER_CHECK_ACTION_NAME, Names.SAME, LeaderCheckRequest::new, this::handleLeaderCheck);
    }

    /**
     * Start a leader checker for the given leader. Should only be called after successfully joining this leader.
     *
     * @param leader the node to be checked as leader
     * @return a `Releasable` that can be used to stop this checker.
     */
    public Releasable startLeaderChecker(final DiscoveryNode leader) {
        assert transportService.getLocalNode().equals(leader) == false;
        CheckScheduler checkScheduler = new CheckScheduler(leader);
        checkScheduler.handleWakeUp();
        return checkScheduler;
    }

    /**
     * Update the "known" discovery nodes. Should be called on the leader before a new cluster state is published to reflect the new
     * publication targets, and also called if a leader becomes a non-leader.
     * TODO if heartbeats can make nodes become followers then this needs to be called before a heartbeat is sent to a new node too.
     *
     * isLocalNodeElectedMaster() should reflect whether this node is a leader, and nodeExists()
     * should indicate whether nodes are known publication targets or not.
     */
    public void setLastPublishedDiscoveryNodes(DiscoveryNodes discoveryNodes) {
        logger.trace("updating last-published nodes: {}", discoveryNodes);
        lastPublishedDiscoveryNodes = discoveryNodes;
    }

    private void handleLeaderCheck(LeaderCheckRequest request, TransportChannel transportChannel, Task task) throws IOException {
        final DiscoveryNodes lastPublishedDiscoveryNodes = this.lastPublishedDiscoveryNodes;
        assert lastPublishedDiscoveryNodes != null;

        if (lastPublishedDiscoveryNodes.isLocalNodeElectedMaster() == false) {
            logger.debug("non-master handling {}", request);
            transportChannel.sendResponse(new CoordinationStateRejectedException("non-leader rejecting leader check"));
        } else if (lastPublishedDiscoveryNodes.nodeExists(request.getSender()) == false) {
            logger.debug("leader check from unknown node: {}", request);
            transportChannel.sendResponse(new CoordinationStateRejectedException("leader check from unknown node"));
        } else {
            logger.trace("handling {}", request);
            transportChannel.sendResponse(Empty.INSTANCE);
        }
    }

    private class CheckScheduler implements Releasable {

        private final AtomicBoolean isClosed = new AtomicBoolean();
        private final AtomicLong failureCountSinceLastSuccess = new AtomicLong();
        private final DiscoveryNode leader;

        CheckScheduler(final DiscoveryNode leader) {
            this.leader = leader;
        }

        @Override
        public void close() {
            if (isClosed.compareAndSet(false, true) == false) {
                logger.debug("already closed");
            } else {
                logger.debug("closed");
            }
        }

        void handleWakeUp() {
            if (isClosed.get()) {
                logger.debug("closed check scheduler woken up, doing nothing");
                return;
            }

            logger.trace("checking {} with [{}] = {}", leader, LEADER_CHECK_TIMEOUT_SETTING.getKey(), leaderCheckTimeout);

            // TODO lag detection:
            // In the PoC, the leader sent its current version to the follower in the response to a LeaderCheck, so the follower
            // could detect if it was lagging. We'd prefer this to be implemented on the leader, so the response is just
            // TransportResponse.Empty here.
            transportService.sendRequest(leader, LEADER_CHECK_ACTION_NAME, new LeaderCheckRequest(transportService.getLocalNode()),
                TransportRequestOptions.builder().withTimeout(leaderCheckTimeout).withType(Type.PING).build(),

                new TransportResponseHandler<TransportResponse.Empty>() {
                    @Override
                    public void handleResponse(Empty response) {
                        if (isClosed.get()) {
                            logger.debug("closed check scheduler received a response, doing nothing");
                            return;
                        }

                        failureCountSinceLastSuccess.set(0);
                        scheduleNextWakeUp(); // logs trace message indicating success
                    }

                    @Override
                    public void handleException(TransportException exp) {
                        if (isClosed.get()) {
                            logger.debug("closed check scheduler received a response, doing nothing");
                            return;
                        }

                        if (exp instanceof ConnectTransportException || exp.getCause() instanceof ConnectTransportException) {
                            logger.debug(new ParameterizedMessage("leader [{}] disconnected, failing immediately", leader), exp);
                            leaderFailed();
                            return;
                        }

                        long failureCount = failureCountSinceLastSuccess.incrementAndGet();
                        if (failureCount >= leaderCheckRetryCount) {
                            logger.debug(new ParameterizedMessage("{} consecutive failures (limit [{}] is {}) so leader [{}] has failed",
                                failureCount, LEADER_CHECK_RETRY_COUNT_SETTING.getKey(), leaderCheckRetryCount, leader), exp);
                            leaderFailed();
                            return;
                        }

                        logger.debug(new ParameterizedMessage("{} consecutive failures (limit [{}] is {}) with leader [{}]",
                            failureCount, LEADER_CHECK_RETRY_COUNT_SETTING.getKey(), leaderCheckRetryCount, leader), exp);
                        scheduleNextWakeUp();
                    }

                    @Override
                    public String executor() {
                        return Names.SAME;
                    }
                });
        }

        private void leaderFailed() {
            if (isClosed.compareAndSet(false, true)) {
                transportService.getThreadPool().generic().execute(onLeaderFailure);
            } else {
                logger.debug("already closed, not failing leader");
            }
        }

        private void scheduleNextWakeUp() {
            logger.trace("scheduling next check of {} for [{}] = {}", leader, LEADER_CHECK_INTERVAL_SETTING.getKey(), leaderCheckInterval);
            transportService.getThreadPool().schedule(leaderCheckInterval, Names.SAME, new Runnable() {
                @Override
                public void run() {
                    handleWakeUp();
                }

                @Override
                public String toString() {
                    return "scheduled check of leader " + leader;
                }
            });
        }
    }

    public static class LeaderCheckRequest extends TransportRequest {

        private final DiscoveryNode sender;

        public LeaderCheckRequest(final DiscoveryNode sender) {
            this.sender = sender;
        }

        public LeaderCheckRequest(final StreamInput in) throws IOException {
            super(in);
            sender = new DiscoveryNode(in);
        }

        @Override
        public void writeTo(final StreamOutput out) throws IOException {
            super.writeTo(out);
            sender.writeTo(out);
        }

        public DiscoveryNode getSender() {
            return sender;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final LeaderCheckRequest that = (LeaderCheckRequest) o;
            return Objects.equals(sender, that.sender);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sender);
        }

        @Override
        public String toString() {
            return "LeaderCheckRequest{" +
                "sender=" + sender +
                '}';
        }
    }
}

