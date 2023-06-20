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

package com.colasoft.opensearch.index.reindex;

import java.util.Optional;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.AutoCreateIndex;
import com.colasoft.opensearch.action.support.HandledTransportAction;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Setting.Property;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.reindex.spi.RemoteReindexExtension;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class TransportReindexAction extends HandledTransportAction<ReindexRequest, BulkByScrollResponse> {
    static final Setting<List<String>> REMOTE_CLUSTER_WHITELIST = Setting.listSetting(
        "reindex.remote.whitelist",
        emptyList(),
        Function.identity(),
        Property.NodeScope,
        Property.Deprecated
    );
    // The setting below is going to replace the above.
    // To keep backwards compatibility, the old usage is remained, and it's also used as the fallback for the new usage.
    public static final Setting<List<String>> REMOTE_CLUSTER_ALLOWLIST = Setting.listSetting(
        "reindex.remote.allowlist",
        REMOTE_CLUSTER_WHITELIST,
        Function.identity(),
        Property.NodeScope
    );
    public static Optional<RemoteReindexExtension> remoteExtension = Optional.empty();

    private final ReindexValidator reindexValidator;
    private final Reindexer reindexer;

    @Inject
    public TransportReindexAction(
        Settings settings,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        ClusterService clusterService,
        ScriptService scriptService,
        AutoCreateIndex autoCreateIndex,
        Client client,
        TransportService transportService,
        ReindexSslConfig sslConfig
    ) {
        super(ReindexAction.NAME, transportService, actionFilters, ReindexRequest::new);
        this.reindexValidator = new ReindexValidator(settings, clusterService, indexNameExpressionResolver, autoCreateIndex);
        this.reindexer = new Reindexer(clusterService, client, threadPool, scriptService, sslConfig, remoteExtension);
    }

    @Override
    protected void doExecute(Task task, ReindexRequest request, ActionListener<BulkByScrollResponse> listener) {
        reindexValidator.initialValidation(request);
        BulkByScrollTask bulkByScrollTask = (BulkByScrollTask) task;
        reindexer.initTask(bulkByScrollTask, request, new ActionListener<Void>() {
            @Override
            public void onResponse(Void v) {
                reindexer.execute(bulkByScrollTask, request, listener);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }
}
