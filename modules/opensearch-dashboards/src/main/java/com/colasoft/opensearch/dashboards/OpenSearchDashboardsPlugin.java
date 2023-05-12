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

package com.colasoft.opensearch.dashboards;

import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Setting.Property;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.index.reindex.RestDeleteByQueryAction;
import com.colasoft.opensearch.indices.SystemIndexDescriptor;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SystemIndexPlugin;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.rest.action.admin.indices.RestCreateIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetIndicesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndexPutAliasAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestRefreshAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestUpdateSettingsAction;
import com.colasoft.opensearch.rest.action.document.RestBulkAction;
import com.colasoft.opensearch.rest.action.document.RestDeleteAction;
import com.colasoft.opensearch.rest.action.document.RestGetAction;
import com.colasoft.opensearch.rest.action.document.RestIndexAction;
import com.colasoft.opensearch.rest.action.document.RestIndexAction.AutoIdHandler;
import com.colasoft.opensearch.rest.action.document.RestIndexAction.CreateHandler;
import com.colasoft.opensearch.rest.action.document.RestMultiGetAction;
import com.colasoft.opensearch.rest.action.document.RestUpdateAction;
import com.colasoft.opensearch.rest.action.search.RestClearScrollAction;
import com.colasoft.opensearch.rest.action.search.RestSearchAction;
import com.colasoft.opensearch.rest.action.search.RestSearchScrollAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

public class OpenSearchDashboardsPlugin extends Plugin implements SystemIndexPlugin {

    public static final Setting<List<String>> OPENSEARCH_DASHBOARDS_INDEX_NAMES_SETTING = Setting.listSetting(
        "opensearch_dashboards.system_indices",
        unmodifiableList(
            Arrays.asList(
                ".opensearch_dashboards",
                ".opensearch_dashboards_*",
                ".reporting-*",
                ".apm-agent-configuration",
                ".apm-custom-link"
            )
        ),
        Function.identity(),
        Property.NodeScope
    );

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        return unmodifiableList(
            OPENSEARCH_DASHBOARDS_INDEX_NAMES_SETTING.get(settings)
                .stream()
                .map(pattern -> new SystemIndexDescriptor(pattern, "System index used by OpenSearch Dashboards"))
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        // TODO need to figure out what subset of system indices OpenSearch Dashboards should have access to via these APIs
        return unmodifiableList(
            Arrays.asList(
                // Based on https://github.com/elastic/kibana/issues/49764
                // apis needed to perform migrations... ideally these will go away
                new OpenSearchDashboardsWrappedRestHandler(new RestCreateIndexAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestGetAliasesAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestIndexPutAliasAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestRefreshAction()),

                // apis needed to access saved objects
                new OpenSearchDashboardsWrappedRestHandler(new RestGetAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestMultiGetAction(settings)),
                new OpenSearchDashboardsWrappedRestHandler(new RestSearchAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestBulkAction(settings)),
                new OpenSearchDashboardsWrappedRestHandler(new RestDeleteAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestDeleteByQueryAction()),

                // api used for testing
                new OpenSearchDashboardsWrappedRestHandler(new RestUpdateSettingsAction()),

                // apis used specifically by reporting
                new OpenSearchDashboardsWrappedRestHandler(new RestGetIndicesAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestIndexAction()),
                new OpenSearchDashboardsWrappedRestHandler(new CreateHandler()),
                new OpenSearchDashboardsWrappedRestHandler(new AutoIdHandler(nodesInCluster)),
                new OpenSearchDashboardsWrappedRestHandler(new RestUpdateAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestSearchScrollAction()),
                new OpenSearchDashboardsWrappedRestHandler(new RestClearScrollAction())
            )
        );

    }

    @Override
    public List<Setting<?>> getSettings() {
        return Collections.singletonList(OPENSEARCH_DASHBOARDS_INDEX_NAMES_SETTING);
    }

    static class OpenSearchDashboardsWrappedRestHandler extends BaseRestHandler.Wrapper {

        OpenSearchDashboardsWrappedRestHandler(BaseRestHandler delegate) {
            super(delegate);
        }

        @Override
        public String getName() {
            return "opensearch_dashboards_" + super.getName();
        }

        @Override
        public boolean allowSystemIndexAccessByDefault() {
            return true;
        }

        @Override
        public List<Route> routes() {
            return unmodifiableList(
                super.routes().stream()
                    .map(route -> new Route(route.getMethod(), "/_opensearch_dashboards" + route.getPath()))
                    .collect(Collectors.toList())
            );
        }
    }
}
