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

package com.colasoft.opensearch.plugin.noop;

import com.colasoft.opensearch.plugin.noop.action.bulk.NoopBulkAction;
import com.colasoft.opensearch.plugin.noop.action.bulk.RestNoopBulkAction;
import com.colasoft.opensearch.plugin.noop.action.bulk.TransportNoopBulkAction;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.plugin.noop.action.search.NoopSearchAction;
import com.colasoft.opensearch.plugin.noop.action.search.RestNoopSearchAction;
import com.colasoft.opensearch.plugin.noop.action.search.TransportNoopSearchAction;
import com.colasoft.opensearch.plugins.ActionPlugin;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class NoopPlugin extends Plugin implements ActionPlugin {
    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
            new ActionHandler<>(NoopBulkAction.INSTANCE, TransportNoopBulkAction.class),
            new ActionHandler<>(NoopSearchAction.INSTANCE, TransportNoopSearchAction.class)
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
        return Arrays.asList(new RestNoopBulkAction(), new RestNoopSearchAction());
    }
}
