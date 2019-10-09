/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.enrich.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.master.TransportMasterNodeAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.enrich.action.ExecuteEnrichPolicyAction;
import org.elasticsearch.xpack.core.enrich.action.ExecuteEnrichPolicyStatus;
import org.elasticsearch.xpack.enrich.EnrichPolicyExecutor;
import org.elasticsearch.xpack.enrich.EnrichPolicyLocks;

import java.io.IOException;

public class TransportExecuteEnrichPolicyAction
    extends TransportMasterNodeAction<ExecuteEnrichPolicyAction.Request, ExecuteEnrichPolicyAction.Response> {

    private final EnrichPolicyExecutor executor;

    @Inject
    public TransportExecuteEnrichPolicyAction(Settings settings,
                                              Client client,
                                              TransportService transportService,
                                              ClusterService clusterService,
                                              ThreadPool threadPool,
                                              ActionFilters actionFilters,
                                              IndexNameExpressionResolver indexNameExpressionResolver,
                                              EnrichPolicyLocks enrichPolicyLocks) {
        super(ExecuteEnrichPolicyAction.NAME, transportService, clusterService, threadPool, actionFilters,
            ExecuteEnrichPolicyAction.Request::new, indexNameExpressionResolver);
        this.executor = new EnrichPolicyExecutor(settings, clusterService, client, transportService.getTaskManager(), threadPool,
            new IndexNameExpressionResolver(), enrichPolicyLocks, System::currentTimeMillis);
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ExecuteEnrichPolicyAction.Response read(StreamInput in) throws IOException {
        return new ExecuteEnrichPolicyAction.Response(in);
    }

    @Override
    protected void masterOperation(ExecuteEnrichPolicyAction.Request request, ClusterState state,
                                   ActionListener<ExecuteEnrichPolicyAction.Response> listener) {
        executor.runPolicy(request, new ActionListener<ExecuteEnrichPolicyStatus>() {
            @Override
            public void onResponse(ExecuteEnrichPolicyStatus executionStatus) {
                listener.onResponse(new ExecuteEnrichPolicyAction.Response(executionStatus));
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    @Override
    protected ClusterBlockException checkBlock(ExecuteEnrichPolicyAction.Request request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }
}
