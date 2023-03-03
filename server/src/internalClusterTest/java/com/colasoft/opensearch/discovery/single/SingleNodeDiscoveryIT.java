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

package com.colasoft.opensearch.discovery.single;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.coordination.JoinHelper;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.node.Node.DiscoverySettings;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.InternalTestCluster;
import com.colasoft.opensearch.test.MockHttpTransport;
import com.colasoft.opensearch.test.MockLogAppender;
import com.colasoft.opensearch.test.NodeConfigurationSource;
import com.colasoft.opensearch.transport.RemoteTransportException;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 1, numClientNodes = 0, supportsDedicatedMasters = false, autoManageMasterNodes = false)
public class SingleNodeDiscoveryIT extends OpenSearchIntegTestCase {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
            .put(super.nodeSettings(nodeOrdinal))
            .put("discovery.type", "single-node")
            .put("transport.port", getPortRange())
            .build();
    }

    public void testSingleNodesDoNotDiscoverEachOther() throws IOException, InterruptedException {
        final TransportService service = internalCluster().getInstance(TransportService.class);
        final int port = service.boundAddress().publishAddress().getPort();
        final NodeConfigurationSource configurationSource = new NodeConfigurationSource() {
            @Override
            public Settings nodeSettings(int nodeOrdinal) {
                return Settings.builder()
                    .put("discovery.type", "single-node")
                    .put("transport.type", getTestTransportType())
                    /*
                     * We align the port ranges of the two as then with zen discovery these two
                     * nodes would find each other.
                     */
                    .put("transport.port", port + "-" + (port + 5 - 1))
                    .build();
            }

            @Override
            public Path nodeConfigPath(int nodeOrdinal) {
                return null;
            }
        };
        try (
            InternalTestCluster other = new InternalTestCluster(
                randomLong(),
                createTempDir(),
                false,
                false,
                1,
                1,
                internalCluster().getClusterName(),
                configurationSource,
                0,
                "other",
                Arrays.asList(getTestTransportPlugin(), MockHttpTransport.TestPlugin.class),
                Function.identity()
            )
        ) {
            other.beforeTest(random());
            final ClusterState first = internalCluster().getInstance(ClusterService.class).state();
            final ClusterState second = other.getInstance(ClusterService.class).state();
            assertThat(first.nodes().getSize(), equalTo(1));
            assertThat(second.nodes().getSize(), equalTo(1));
            assertThat(first.nodes().getClusterManagerNodeId(), not(equalTo(second.nodes().getClusterManagerNodeId())));
            assertThat(first.metadata().clusterUUID(), not(equalTo(second.metadata().clusterUUID())));
        }
    }

    public void testCannotJoinNodeWithSingleNodeDiscovery() throws Exception {
        Logger clusterLogger = LogManager.getLogger(JoinHelper.class);
        try (MockLogAppender mockAppender = MockLogAppender.createForLoggers(clusterLogger)) {
            mockAppender.addExpectation(
                new MockLogAppender.SeenEventExpectation("test", JoinHelper.class.getCanonicalName(), Level.INFO, "failed to join") {

                    @Override
                    public boolean innerMatch(final LogEvent event) {
                        return event.getThrown() != null
                            && event.getThrown().getClass() == RemoteTransportException.class
                            && event.getThrown().getCause() != null
                            && event.getThrown().getCause().getClass() == IllegalStateException.class
                            && event.getThrown()
                                .getCause()
                                .getMessage()
                                .contains("cannot join node with [discovery.type] set to [single-node]");
                    }
                }
            );
            final TransportService service = internalCluster().getInstance(TransportService.class);
            final int port = service.boundAddress().publishAddress().getPort();
            final NodeConfigurationSource configurationSource = new NodeConfigurationSource() {
                @Override
                public Settings nodeSettings(int nodeOrdinal) {
                    return Settings.builder()
                        .put("discovery.type", "zen")
                        .put("transport.type", getTestTransportType())
                        .put(DiscoverySettings.INITIAL_STATE_TIMEOUT_SETTING.getKey(), "0s")
                        /*
                         * We align the port ranges of the two as then with zen discovery these two
                         * nodes would find each other.
                         */
                        .put("transport.port", port + "-" + (port + 5 - 1))
                        .build();
                }

                @Override
                public Path nodeConfigPath(int nodeOrdinal) {
                    return null;
                }
            };
            try (
                InternalTestCluster other = new InternalTestCluster(
                    randomLong(),
                    createTempDir(),
                    false,
                    false,
                    1,
                    1,
                    internalCluster().getClusterName(),
                    configurationSource,
                    0,
                    "other",
                    Arrays.asList(getTestTransportPlugin(), MockHttpTransport.TestPlugin.class),
                    Function.identity()
                )
            ) {
                other.beforeTest(random());
                final ClusterState first = internalCluster().getInstance(ClusterService.class).state();
                assertThat(first.nodes().getSize(), equalTo(1));
                assertBusy(() -> mockAppender.assertAllExpectationsMatched());
            }
        }
    }

    public void testStatePersistence() throws Exception {
        createIndex("test");
        internalCluster().fullRestart();
        assertTrue(client().admin().indices().prepareExists("test").get().isExists());
    }

}
