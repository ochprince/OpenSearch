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

package com.colasoft.opensearch.action.admin.indices.get;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.IndicesRequest;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.replication.ClusterStateCreationUtils;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.common.settings.SettingsModule;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.test.OpenSearchSingleNodeTestCase;
import com.colasoft.opensearch.test.transport.CapturingTransport;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class GetIndexActionTests extends OpenSearchSingleNodeTestCase {

    private TransportService transportService;
    private ClusterService clusterService;
    private IndicesService indicesService;
    private ThreadPool threadPool;
    private SettingsFilter settingsFilter;
    private final String indexName = "test_index";

    private TestTransportGetIndexAction getIndexAction;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        settingsFilter = new SettingsModule(Settings.EMPTY, emptyList(), emptyList(), emptySet()).getSettingsFilter();
        threadPool = new TestThreadPool("GetIndexActionTests");
        clusterService = getInstanceFromNode(ClusterService.class);
        indicesService = getInstanceFromNode(IndicesService.class);
        CapturingTransport capturingTransport = new CapturingTransport();
        transportService = capturingTransport.createTransportService(
            clusterService.getSettings(),
            threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> clusterService.localNode(),
            null,
            emptySet()
        );
        transportService.start();
        transportService.acceptIncomingRequests();
        getIndexAction = new GetIndexActionTests.TestTransportGetIndexAction();
    }

    @After
    public void tearDown() throws Exception {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        threadPool = null;
        super.tearDown();
    }

    public void testIncludeDefaults() {
        GetIndexRequest defaultsRequest = new GetIndexRequest().indices(indexName).includeDefaults(true);
        getIndexAction.execute(
            null,
            defaultsRequest,
            ActionListener.wrap(
                defaultsResponse -> assertNotNull(
                    "index.refresh_interval should be set as we are including defaults",
                    defaultsResponse.getSetting(indexName, "index.refresh_interval")
                ),
                exception -> {
                    throw new AssertionError(exception);
                }
            )
        );
    }

    public void testDoNotIncludeDefaults() {
        GetIndexRequest noDefaultsRequest = new GetIndexRequest().indices(indexName);
        getIndexAction.execute(
            null,
            noDefaultsRequest,
            ActionListener.wrap(
                noDefaultsResponse -> assertNull(
                    "index.refresh_interval should be null as it was never set",
                    noDefaultsResponse.getSetting(indexName, "index.refresh_interval")
                ),
                exception -> {
                    throw new AssertionError(exception);
                }
            )
        );
    }

    class TestTransportGetIndexAction extends TransportGetIndexAction {

        TestTransportGetIndexAction() {
            super(
                GetIndexActionTests.this.transportService,
                GetIndexActionTests.this.clusterService,
                GetIndexActionTests.this.threadPool,
                settingsFilter,
                new ActionFilters(emptySet()),
                new GetIndexActionTests.Resolver(),
                indicesService,
                IndexScopedSettings.DEFAULT_SCOPED_SETTINGS
            );
        }

        @Override
        protected void doClusterManagerOperation(
            GetIndexRequest request,
            String[] concreteIndices,
            ClusterState state,
            ActionListener<GetIndexResponse> listener
        ) {
            ClusterState stateWithIndex = ClusterStateCreationUtils.state(indexName, 1, 1);
            super.doClusterManagerOperation(request, concreteIndices, stateWithIndex, listener);
        }
    }

    static class Resolver extends IndexNameExpressionResolver {
        Resolver() {
            super(new ThreadContext(Settings.EMPTY));
        }

        @Override
        public String[] concreteIndexNames(ClusterState state, IndicesRequest request) {
            return request.indices();
        }

        @Override
        public Index[] concreteIndices(ClusterState state, IndicesRequest request) {
            Index[] out = new Index[request.indices().length];
            for (int x = 0; x < out.length; x++) {
                out[x] = new Index(request.indices()[x], "_na_");
            }
            return out;
        }
    }
}
