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

package com.colasoft.opensearch.rest.action.admin.cluster;

import com.colasoft.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateRequest;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateResponse;
import com.colasoft.opensearch.client.Requests;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestResponse;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.rest.action.RestBuilderListener;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * Transport action to get settings
 *
 * @opensearch.api
 */
public class RestClusterGetSettingsAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestClusterGetSettingsAction.class);

    private final Settings settings;
    private final ClusterSettings clusterSettings;
    private final SettingsFilter settingsFilter;

    public RestClusterGetSettingsAction(Settings settings, ClusterSettings clusterSettings, SettingsFilter settingsFilter) {
        this.settings = settings;
        this.clusterSettings = clusterSettings;
        this.settingsFilter = settingsFilter;
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_cluster/settings"));
    }

    @Override
    public String getName() {
        return "cluster_get_settings_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        ClusterStateRequest clusterStateRequest = Requests.clusterStateRequest().routingTable(false).nodes(false);
        final boolean renderDefaults = request.paramAsBoolean("include_defaults", false);
        clusterStateRequest.local(request.paramAsBoolean("local", clusterStateRequest.local()));
        clusterStateRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", clusterStateRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(clusterStateRequest, request);
        return channel -> client.admin().cluster().state(clusterStateRequest, new RestBuilderListener<ClusterStateResponse>(channel) {
            @Override
            public RestResponse buildResponse(ClusterStateResponse response, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(RestStatus.OK, renderResponse(response.getState(), renderDefaults, builder, request));
            }
        });
    }

    @Override
    protected Set<String> responseParams() {
        return Settings.FORMAT_PARAMS;
    }

    @Override
    public boolean canTripCircuitBreaker() {
        return false;
    }

    private XContentBuilder renderResponse(ClusterState state, boolean renderDefaults, XContentBuilder builder, ToXContent.Params params)
        throws IOException {
        return response(state, renderDefaults, settingsFilter, clusterSettings, settings).toXContent(builder, params);
    }

    static ClusterGetSettingsResponse response(
        final ClusterState state,
        final boolean renderDefaults,
        final SettingsFilter settingsFilter,
        final ClusterSettings clusterSettings,
        final Settings settings
    ) {
        return new ClusterGetSettingsResponse(
            settingsFilter.filter(state.metadata().persistentSettings()),
            settingsFilter.filter(state.metadata().transientSettings()),
            renderDefaults ? settingsFilter.filter(clusterSettings.diff(state.metadata().settings(), settings)) : Settings.EMPTY
        );
    }

}
