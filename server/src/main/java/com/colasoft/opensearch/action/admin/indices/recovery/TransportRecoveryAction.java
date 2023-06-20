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

package com.colasoft.opensearch.action.admin.indices.recovery;

import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.DefaultShardOperationFailedException;
import com.colasoft.opensearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardsIterator;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.recovery.RecoveryState;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport action for shard recovery operation. This transport action does not actually
 * perform shard recovery, it only reports on recoveries (both active and complete).
 *
 * @opensearch.internal
 */
public class TransportRecoveryAction extends TransportBroadcastByNodeAction<RecoveryRequest, RecoveryResponse, RecoveryState> {

    private final IndicesService indicesService;

    @Inject
    public TransportRecoveryAction(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            RecoveryAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            RecoveryRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.indicesService = indicesService;
    }

    @Override
    protected RecoveryState readShardResult(StreamInput in) throws IOException {
        return new RecoveryState(in);
    }

    @Override
    protected RecoveryResponse newResponse(
        RecoveryRequest request,
        int totalShards,
        int successfulShards,
        int failedShards,
        List<RecoveryState> responses,
        List<DefaultShardOperationFailedException> shardFailures,
        ClusterState clusterState
    ) {
        Map<String, List<RecoveryState>> shardResponses = new HashMap<>();
        for (RecoveryState recoveryState : responses) {
            if (recoveryState == null) {
                continue;
            }
            String indexName = recoveryState.getShardId().getIndexName();
            if (!shardResponses.containsKey(indexName)) {
                shardResponses.put(indexName, new ArrayList<>());
            }
            if (request.activeOnly()) {
                if (recoveryState.getStage() != RecoveryState.Stage.DONE) {
                    shardResponses.get(indexName).add(recoveryState);
                }
            } else {
                shardResponses.get(indexName).add(recoveryState);
            }
        }
        return new RecoveryResponse(totalShards, successfulShards, failedShards, shardResponses, shardFailures);
    }

    @Override
    protected RecoveryRequest readRequestFrom(StreamInput in) throws IOException {
        return new RecoveryRequest(in);
    }

    @Override
    protected RecoveryState shardOperation(RecoveryRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.getShard(shardRouting.shardId().id());
        return indexShard.recoveryState();
    }

    @Override
    protected ShardsIterator shards(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.routingTable().allShardsIncludingRelocationTargets(concreteIndices);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, RecoveryRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }
}
