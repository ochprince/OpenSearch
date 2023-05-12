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

package com.colasoft.opensearch.http;

import com.colasoft.opensearch.action.index.IndexRequest;
import com.colasoft.opensearch.action.support.WriteRequest;
import com.colasoft.opensearch.client.Request;
import com.colasoft.opensearch.client.RequestOptions;
import com.colasoft.opensearch.client.Response;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.indices.SystemIndexDescriptor;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SystemIndexPlugin;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.colasoft.opensearch.test.rest.OpenSearchRestTestCase.entityAsMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;

public class SystemIndexRestIT extends HttpSmokeTestCase {

    private Set<String> assertedWarnings = new HashSet<>();

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        List<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(SystemIndexTestPlugin.class);
        return plugins;
    }

    public void testSystemIndexAccessBlockedByDefault() throws Exception {
        // create index
        {
            Request putDocRequest = new Request("POST", "/_sys_index_test/add_doc/42");
            Response resp = getRestClient().performRequest(putDocRequest);
            assertThat(resp.getStatusLine().getStatusCode(), equalTo(201));
        }


        // make sure the system index now exists
        assertBusy(() -> {
            Request searchRequest = new Request("GET", "/" + SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "/_count");
            searchRequest.setOptions(expectWarnings("this request accesses system indices: [" + SystemIndexTestPlugin.SYSTEM_INDEX_NAME +
                "], but in a future major version, direct access to system indices will be prevented by default"));

            // Disallow no indices to cause an exception if the flag above doesn't work
            searchRequest.addParameter("allow_no_indices", "false");
            searchRequest.setJsonEntity("{\"query\": {\"match\":  {\"some_field\":  \"some_value\"}}}");

            final Response searchResponse = getRestClient().performRequest(searchRequest);
            assertThat(searchResponse.getStatusLine().getStatusCode(), is(200));
            Map<String, Object> responseMap = entityAsMap(searchResponse);
            assertThat(responseMap, hasKey("count"));
            assertThat(responseMap.get("count"), equalTo(1));
        });

        // And with a partial wildcard
        assertDeprecationWarningOnAccess(".test-*", SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        // And with a total wildcard
        assertDeprecationWarningOnAccess(randomFrom("*", "_all"), SystemIndexTestPlugin.SYSTEM_INDEX_NAME);

        // Try to index a doc directly
        {
            String expectedWarning = "this request accesses system indices: [" + SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "], but in a " +
                "future major version, direct access to system indices will be prevented by default";
            Request putDocDirectlyRequest = new Request("PUT", "/" + SystemIndexTestPlugin.SYSTEM_INDEX_NAME + "/_doc/43");
            putDocDirectlyRequest.setJsonEntity("{\"some_field\":  \"some_other_value\"}");
            putDocDirectlyRequest.setOptions(expectWarnings(expectedWarning));
            Response response = getRestClient().performRequest(putDocDirectlyRequest);
            assertThat(response.getStatusLine().getStatusCode(), equalTo(201));
        }
    }

    private void assertDeprecationWarningOnAccess(String queryPattern, String warningIndexName) throws IOException {
        String expectedWarning = "this request accesses system indices: [" + warningIndexName + "], but in a " +
            "future major version, direct access to system indices will be prevented by default";
        Request searchRequest = new Request("GET", "/" + queryPattern + randomFrom("/_count", "/_search"));
        searchRequest.setJsonEntity("{\"query\": {\"match\":  {\"some_field\":  \"some_value\"}}}");
        // Disallow no indices to cause an exception if this resolves to zero indices, so that we're sure it resolved the index
        searchRequest.addParameter("allow_no_indices", "false");
        if (!assertedWarnings.contains(expectedWarning)) {
            searchRequest.setOptions(expectWarnings(expectedWarning));
            assertedWarnings.add(expectedWarning);
        }
        Response response = getRestClient().performRequest(searchRequest);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
    }

    private RequestOptions expectWarnings(String expectedWarning) {
        final RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        if (!assertedWarnings.contains(expectedWarning)) {
            builder.setWarningsHandler(w -> w.contains(expectedWarning) == false || w.size() != 1);
            assertedWarnings.add(expectedWarning);
        }
        return builder.build();
    }


    public static class SystemIndexTestPlugin extends Plugin implements SystemIndexPlugin {

        public static final String SYSTEM_INDEX_NAME = ".test-system-idx";

        @Override
        public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                                 IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                                 IndexNameExpressionResolver indexNameExpressionResolver,
                                                 Supplier<DiscoveryNodes> nodesInCluster) {
            return List.of(new AddDocRestHandler());
        }

        @Override
        public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
            return Collections.singletonList(new SystemIndexDescriptor(SYSTEM_INDEX_NAME, "System indices for tests"));
        }

        public static class AddDocRestHandler extends BaseRestHandler {
            @Override
            public boolean allowSystemIndexAccessByDefault() {
                return true;
            }

            @Override
            public String getName() {
                return "system_index_test_doc_adder";
            }

            @Override
            public List<Route> routes() {
                return List.of(new Route(RestRequest.Method.POST, "/_sys_index_test/add_doc/{id}"));
            }

            @Override
            protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
                IndexRequest indexRequest = new IndexRequest(SYSTEM_INDEX_NAME);
                indexRequest.id(request.param("id"));
                indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                indexRequest.source(Map.of("some_field", "some_value"));
                return channel -> client.index(indexRequest,
                    new RestStatusToXContentListener<>(channel, r -> r.getLocation(indexRequest.routing())));
            }
        }
    }
}
