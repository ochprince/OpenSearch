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

package com.colasoft.opensearch.cluster.service;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.replication.ClusterStateCreationUtils;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FakeThreadPoolClusterManagerServiceTests extends OpenSearchTestCase {

    public void testFakeClusterManagerService() {
        List<Runnable> runnableTasks = new ArrayList<>();
        AtomicReference<ClusterState> lastClusterStateRef = new AtomicReference<>();
        DiscoveryNode discoveryNode = new DiscoveryNode(
            "node",
            OpenSearchTestCase.buildNewFakeTransportAddress(),
            Collections.emptyMap(),
            new HashSet<>(DiscoveryNodeRole.BUILT_IN_ROLES),
            Version.CURRENT
        );
        lastClusterStateRef.set(ClusterStateCreationUtils.state(discoveryNode, discoveryNode));
        long firstClusterStateVersion = lastClusterStateRef.get().version();
        AtomicReference<ActionListener<Void>> publishingCallback = new AtomicReference<>();
        final ThreadContext context = new ThreadContext(Settings.EMPTY);
        final ThreadPool mockThreadPool = mock(ThreadPool.class);
        when(mockThreadPool.getThreadContext()).thenReturn(context);

        final ExecutorService executorService = mock(ExecutorService.class);
        doAnswer(invocationOnMock -> runnableTasks.add((Runnable) invocationOnMock.getArguments()[0])).when(executorService).execute(any());
        when(mockThreadPool.generic()).thenReturn(executorService);

        FakeThreadPoolClusterManagerService clusterManagerService = new FakeThreadPoolClusterManagerService(
            "test_node",
            "test",
            mockThreadPool,
            runnableTasks::add
        );
        clusterManagerService.setClusterStateSupplier(lastClusterStateRef::get);
        clusterManagerService.setClusterStatePublisher((event, publishListener, ackListener) -> {
            lastClusterStateRef.set(event.state());
            publishingCallback.set(publishListener);
        });
        clusterManagerService.start();

        AtomicBoolean firstTaskCompleted = new AtomicBoolean();
        clusterManagerService.submitStateUpdateTask("test1", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                return ClusterState.builder(currentState)
                    .metadata(Metadata.builder(currentState.metadata()).put(indexBuilder("test1")))
                    .build();
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                assertFalse(firstTaskCompleted.get());
                firstTaskCompleted.set(true);
            }

            @Override
            public void onFailure(String source, Exception e) {
                throw new AssertionError();
            }
        });
        assertThat(runnableTasks.size(), equalTo(1));
        assertThat(lastClusterStateRef.get().metadata().indices().size(), equalTo(0));
        assertThat(lastClusterStateRef.get().version(), equalTo(firstClusterStateVersion));
        assertNull(publishingCallback.get());
        assertFalse(firstTaskCompleted.get());

        final Runnable scheduleTask = runnableTasks.remove(0);
        assertThat(scheduleTask, hasToString("cluster-manager service scheduling next task"));
        scheduleTask.run();

        final Runnable publishTask = runnableTasks.remove(0);
        assertThat(publishTask, hasToString(containsString("publish change of cluster state")));
        publishTask.run();

        assertThat(lastClusterStateRef.get().metadata().indices().size(), equalTo(1));
        assertThat(lastClusterStateRef.get().version(), equalTo(firstClusterStateVersion + 1));
        assertNotNull(publishingCallback.get());
        assertFalse(firstTaskCompleted.get());
        assertThat(runnableTasks.size(), equalTo(0));

        AtomicBoolean secondTaskCompleted = new AtomicBoolean();
        clusterManagerService.submitStateUpdateTask("test2", new ClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                return ClusterState.builder(currentState)
                    .metadata(Metadata.builder(currentState.metadata()).put(indexBuilder("test2")))
                    .build();
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                assertFalse(secondTaskCompleted.get());
                secondTaskCompleted.set(true);
            }

            @Override
            public void onFailure(String source, Exception e) {
                throw new AssertionError();
            }
        });
        assertThat(runnableTasks.size(), equalTo(0));

        publishingCallback.getAndSet(null).onResponse(null);
        assertTrue(firstTaskCompleted.get());
        assertThat(runnableTasks.size(), equalTo(1)); // check that new task gets queued

        runnableTasks.remove(0).run(); // schedule again
        runnableTasks.remove(0).run(); // publish again
        assertThat(lastClusterStateRef.get().metadata().indices().size(), equalTo(2));
        assertThat(lastClusterStateRef.get().version(), equalTo(firstClusterStateVersion + 2));
        assertNotNull(publishingCallback.get());
        assertFalse(secondTaskCompleted.get());
        publishingCallback.getAndSet(null).onResponse(null);
        assertTrue(secondTaskCompleted.get());
        assertThat(runnableTasks.size(), equalTo(0)); // check that no more tasks are queued
    }

    private static IndexMetadata.Builder indexBuilder(String index) {
        return IndexMetadata.builder(index)
            .settings(
                settings(Version.CURRENT).put(IndexMetadata.SETTING_NUMBER_OF_SHARDS, 1).put(IndexMetadata.SETTING_NUMBER_OF_REPLICAS, 0)
            );
    }
}
