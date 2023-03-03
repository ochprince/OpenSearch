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

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.LatchedActionListener;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.UUIDs;
import com.colasoft.opensearch.common.util.concurrent.AtomicArray;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.search.SearchPhaseResult;
import com.colasoft.opensearch.search.SearchShardTarget;
import com.colasoft.opensearch.search.internal.ShardSearchContextId;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.VersionUtils;
import com.colasoft.opensearch.transport.NodeNotConnectedException;
import com.colasoft.opensearch.transport.Transport;
import com.colasoft.opensearch.transport.TransportResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ClearScrollControllerTests extends OpenSearchTestCase {

    public void testClearAll() throws InterruptedException {
        DiscoveryNode node1 = new DiscoveryNode("node_1", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node2 = new DiscoveryNode("node_2", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node3 = new DiscoveryNode("node_3", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNodes nodes = DiscoveryNodes.builder().add(node1).add(node2).add(node3).build();
        CountDownLatch latch = new CountDownLatch(1);
        ActionListener<ClearScrollResponse> listener = new LatchedActionListener<>(new ActionListener<ClearScrollResponse>() {
            @Override
            public void onResponse(ClearScrollResponse clearScrollResponse) {
                assertEquals(3, clearScrollResponse.getNumFreed());
                assertTrue(clearScrollResponse.isSucceeded());
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        }, latch);
        List<DiscoveryNode> nodesInvoked = new CopyOnWriteArrayList<>();
        SearchTransportService searchTransportService = new SearchTransportService(null, null) {
            @Override
            public void sendClearAllScrollContexts(Transport.Connection connection, ActionListener<TransportResponse> listener) {
                nodesInvoked.add(connection.getNode());
                Thread t = new Thread(() -> listener.onResponse(TransportResponse.Empty.INSTANCE)); // response is unused
                t.start();
            }

            @Override
            public Transport.Connection getConnection(String clusterAlias, DiscoveryNode node) {
                return new SearchAsyncActionTests.MockConnection(node);
            }
        };
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.scrollIds(Arrays.asList("_all"));
        ClearScrollController controller = new ClearScrollController(clearScrollRequest, listener, nodes, logger, searchTransportService);
        controller.run();
        latch.await();
        assertEquals(3, nodesInvoked.size());
        Collections.sort(nodesInvoked, Comparator.comparing(DiscoveryNode::getId));
        assertEquals(nodesInvoked, Arrays.asList(node1, node2, node3));
    }

    public void testClearScrollIds() throws IOException, InterruptedException {
        DiscoveryNode node1 = new DiscoveryNode("node_1", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node2 = new DiscoveryNode("node_2", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node3 = new DiscoveryNode("node_3", buildNewFakeTransportAddress(), Version.CURRENT);
        AtomicArray<SearchPhaseResult> array = new AtomicArray<>(3);
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult1 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 1),
            node1
        );
        testSearchPhaseResult1.setSearchShardTarget(new SearchShardTarget("node_1", new ShardId("idx", "uuid1", 2), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult2 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 12),
            node2
        );
        testSearchPhaseResult2.setSearchShardTarget(new SearchShardTarget("node_2", new ShardId("idy", "uuid2", 42), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult3 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 42),
            node3
        );
        testSearchPhaseResult3.setSearchShardTarget(new SearchShardTarget("node_3", new ShardId("idy", "uuid2", 43), null, null));
        array.setOnce(0, testSearchPhaseResult1);
        array.setOnce(1, testSearchPhaseResult2);
        array.setOnce(2, testSearchPhaseResult3);
        AtomicInteger numFreed = new AtomicInteger(0);
        String scrollId = TransportSearchHelper.buildScrollId(array, VersionUtils.randomVersion(random()));
        DiscoveryNodes nodes = DiscoveryNodes.builder().add(node1).add(node2).add(node3).build();
        CountDownLatch latch = new CountDownLatch(1);
        ActionListener<ClearScrollResponse> listener = new LatchedActionListener<>(new ActionListener<ClearScrollResponse>() {
            @Override
            public void onResponse(ClearScrollResponse clearScrollResponse) {
                assertEquals(numFreed.get(), clearScrollResponse.getNumFreed());
                assertTrue(clearScrollResponse.isSucceeded());
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        }, latch);
        List<DiscoveryNode> nodesInvoked = new CopyOnWriteArrayList<>();
        SearchTransportService searchTransportService = new SearchTransportService(null, null) {

            @Override
            public void sendFreeContext(
                Transport.Connection connection,
                ShardSearchContextId contextId,
                ActionListener<SearchFreeContextResponse> listener
            ) {
                nodesInvoked.add(connection.getNode());
                boolean freed = randomBoolean();
                if (freed) {
                    numFreed.incrementAndGet();
                }
                Thread t = new Thread(() -> listener.onResponse(new SearchFreeContextResponse(freed)));
                t.start();
            }

            @Override
            public Transport.Connection getConnection(String clusterAlias, DiscoveryNode node) {
                return new SearchAsyncActionTests.MockConnection(node);
            }
        };
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.scrollIds(Arrays.asList(scrollId));
        ClearScrollController controller = new ClearScrollController(clearScrollRequest, listener, nodes, logger, searchTransportService);
        controller.run();
        latch.await();
        assertEquals(3, nodesInvoked.size());
        Collections.sort(nodesInvoked, Comparator.comparing(DiscoveryNode::getId));
        assertEquals(nodesInvoked, Arrays.asList(node1, node2, node3));
    }

    public void testClearScrollIdsWithFailure() throws IOException, InterruptedException {
        DiscoveryNode node1 = new DiscoveryNode("node_1", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node2 = new DiscoveryNode("node_2", buildNewFakeTransportAddress(), Version.CURRENT);
        DiscoveryNode node3 = new DiscoveryNode("node_3", buildNewFakeTransportAddress(), Version.CURRENT);
        AtomicArray<SearchPhaseResult> array = new AtomicArray<>(3);
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult1 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 1),
            node1
        );
        testSearchPhaseResult1.setSearchShardTarget(new SearchShardTarget("node_1", new ShardId("idx", "uuid1", 2), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult2 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 12),
            node2
        );
        testSearchPhaseResult2.setSearchShardTarget(new SearchShardTarget("node_2", new ShardId("idy", "uuid2", 42), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult3 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId(UUIDs.randomBase64UUID(), 42),
            node3
        );
        testSearchPhaseResult3.setSearchShardTarget(new SearchShardTarget("node_3", new ShardId("idy", "uuid2", 43), null, null));
        array.setOnce(0, testSearchPhaseResult1);
        array.setOnce(1, testSearchPhaseResult2);
        array.setOnce(2, testSearchPhaseResult3);
        AtomicInteger numFreed = new AtomicInteger(0);
        AtomicInteger numFailures = new AtomicInteger(0);
        AtomicInteger numConnectionFailures = new AtomicInteger(0);
        String scrollId = TransportSearchHelper.buildScrollId(array, VersionUtils.randomVersion(random()));
        DiscoveryNodes nodes = DiscoveryNodes.builder().add(node1).add(node2).add(node3).build();
        CountDownLatch latch = new CountDownLatch(1);

        ActionListener<ClearScrollResponse> listener = new LatchedActionListener<>(new ActionListener<ClearScrollResponse>() {
            @Override
            public void onResponse(ClearScrollResponse clearScrollResponse) {
                assertEquals(numFreed.get(), clearScrollResponse.getNumFreed());
                if (numFailures.get() > 0) {
                    assertFalse(clearScrollResponse.isSucceeded());
                } else {
                    assertTrue(clearScrollResponse.isSucceeded());
                }
            }

            @Override
            public void onFailure(Exception e) {
                throw new AssertionError(e);
            }
        }, latch);
        List<DiscoveryNode> nodesInvoked = new CopyOnWriteArrayList<>();
        SearchTransportService searchTransportService = new SearchTransportService(null, null) {

            @Override
            public void sendFreeContext(
                Transport.Connection connection,
                ShardSearchContextId contextId,
                ActionListener<SearchFreeContextResponse> listener
            ) {
                nodesInvoked.add(connection.getNode());
                boolean freed = randomBoolean();
                boolean fail = randomBoolean();
                Thread t = new Thread(() -> {
                    if (fail) {
                        numFailures.incrementAndGet();
                        listener.onFailure(new IllegalArgumentException("boom"));
                    } else {
                        if (freed) {
                            numFreed.incrementAndGet();
                        }
                        listener.onResponse(new SearchFreeContextResponse(freed));
                    }
                });
                t.start();
            }

            @Override
            public Transport.Connection getConnection(String clusterAlias, DiscoveryNode node) {
                if (randomBoolean()) {
                    numFailures.incrementAndGet();
                    numConnectionFailures.incrementAndGet();
                    throw new NodeNotConnectedException(node, "boom");
                }
                return new SearchAsyncActionTests.MockConnection(node);
            }
        };
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.scrollIds(Arrays.asList(scrollId));
        ClearScrollController controller = new ClearScrollController(clearScrollRequest, listener, nodes, logger, searchTransportService);
        controller.run();
        latch.await();
        assertEquals(3 - numConnectionFailures.get(), nodesInvoked.size());
    }
}
