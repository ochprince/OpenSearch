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

package com.colasoft.opensearch.action.resync;

import org.apache.logging.log4j.message.ParameterizedMessage;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.replication.ReplicationOperation;
import com.colasoft.opensearch.action.support.replication.ReplicationResponse;
import com.colasoft.opensearch.action.support.replication.TransportReplicationAction;
import com.colasoft.opensearch.action.support.replication.TransportWriteAction;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.IndexingPressureService;
import com.colasoft.opensearch.index.engine.Engine;
import com.colasoft.opensearch.index.seqno.SequenceNumbers;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.PrimaryReplicaSyncer;
import com.colasoft.opensearch.index.translog.Translog;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.SystemIndices;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool.Names;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Perform replication resync
 *
 * @opensearch.internal
 */
public class TransportResyncReplicationAction extends TransportWriteAction<
    ResyncReplicationRequest,
    ResyncReplicationRequest,
    ResyncReplicationResponse> implements PrimaryReplicaSyncer.SyncAction {

    private static String ACTION_NAME = "internal:index/seq_no/resync";
    private static final Function<IndexShard, String> EXECUTOR_NAME_FUNCTION = shard -> {
        if (shard.indexSettings().getIndexMetadata().isSystem()) {
            return Names.SYSTEM_WRITE;
        } else {
            return Names.WRITE;
        }
    };

    @Inject
    public TransportResyncReplicationAction(
        Settings settings,
        TransportService transportService,
        ClusterService clusterService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ShardStateAction shardStateAction,
        ActionFilters actionFilters,
        IndexingPressureService indexingPressureService,
        SystemIndices systemIndices
    ) {
        super(
            settings,
            ACTION_NAME,
            transportService,
            clusterService,
            indicesService,
            threadPool,
            shardStateAction,
            actionFilters,
            ResyncReplicationRequest::new,
            ResyncReplicationRequest::new,
            EXECUTOR_NAME_FUNCTION,
            true, /* we should never reject resync because of thread pool capacity on primary */
            indexingPressureService,
            systemIndices
        );
    }

    @Override
    protected void doExecute(Task parentTask, ResyncReplicationRequest request, ActionListener<ResyncReplicationResponse> listener) {
        assert false : "use TransportResyncReplicationAction#sync";
    }

    @Override
    protected ResyncReplicationResponse newResponseInstance(StreamInput in) throws IOException {
        return new ResyncReplicationResponse(in);
    }

    @Override
    protected ReplicationOperation.Replicas newReplicasProxy() {
        return new ResyncActionReplicasProxy();
    }

    @Override
    protected ClusterBlockLevel globalBlockLevel() {
        // resync should never be blocked because it's an internal action
        return null;
    }

    @Override
    public ClusterBlockLevel indexBlockLevel() {
        // resync should never be blocked because it's an internal action
        return null;
    }

    @Override
    protected void dispatchedShardOperationOnPrimary(
        ResyncReplicationRequest request,
        IndexShard primary,
        ActionListener<PrimaryResult<ResyncReplicationRequest, ResyncReplicationResponse>> listener
    ) {
        ActionListener.completeWith(
            listener,
            () -> new WritePrimaryResult<>(performOnPrimary(request), new ResyncReplicationResponse(), null, null, primary, logger)
        );
    }

    @Override
    protected long primaryOperationSize(ResyncReplicationRequest request) {
        return Stream.of(request.getOperations()).mapToLong(Translog.Operation::estimateSize).sum();
    }

    public static ResyncReplicationRequest performOnPrimary(ResyncReplicationRequest request) {
        return request;
    }

    @Override
    protected void dispatchedShardOperationOnReplica(
        ResyncReplicationRequest request,
        IndexShard replica,
        ActionListener<ReplicaResult> listener
    ) {
        ActionListener.completeWith(listener, () -> {
            Translog.Location location = performOnReplica(request, replica);
            return new WriteReplicaResult<>(request, location, null, replica, logger);
        });
    }

    @Override
    protected long replicaOperationSize(ResyncReplicationRequest request) {
        return Stream.of(request.getOperations()).mapToLong(Translog.Operation::estimateSize).sum();
    }

    public static Translog.Location performOnReplica(ResyncReplicationRequest request, IndexShard replica) throws Exception {
        Translog.Location location = null;
        /*
         * Operations received from resync do not have auto_id_timestamp individually, we need to bootstrap this max_seen_timestamp
         * (at least the highest timestamp from any of these operations) to make sure that we will disable optimization for the same
         * append-only requests with timestamp (sources of these operations) that are replicated; otherwise we may have duplicates.
         */
        replica.updateMaxUnsafeAutoIdTimestamp(request.getMaxSeenAutoIdTimestampOnPrimary());
        for (Translog.Operation operation : request.getOperations()) {
            final Engine.Result operationResult = replica.applyTranslogOperation(operation, Engine.Operation.Origin.REPLICA);
            if (operationResult.getResultType() == Engine.Result.Type.MAPPING_UPDATE_REQUIRED) {
                throw new TransportReplicationAction.RetryOnReplicaException(
                    replica.shardId(),
                    "Mappings are not available on the replica yet, triggered update: " + operationResult.getRequiredMappingUpdate()
                );
            }
            location = syncOperationResultOrThrow(operationResult, location);
        }
        if (request.getTrimAboveSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO) {
            replica.trimOperationOfPreviousPrimaryTerms(request.getTrimAboveSeqNo());
        }
        return location;
    }

    @Override
    public void sync(
        ResyncReplicationRequest request,
        Task parentTask,
        String primaryAllocationId,
        long primaryTerm,
        ActionListener<ResyncReplicationResponse> listener
    ) {
        // skip reroute phase
        transportService.sendChildRequest(
            clusterService.localNode(),
            transportPrimaryAction,
            new ConcreteShardRequest<>(request, primaryAllocationId, primaryTerm),
            parentTask,
            transportOptions,
            new TransportResponseHandler<ResyncReplicationResponse>() {
                @Override
                public ResyncReplicationResponse read(StreamInput in) throws IOException {
                    return newResponseInstance(in);
                }

                @Override
                public String executor() {
                    return ThreadPool.Names.SAME;
                }

                @Override
                public void handleResponse(ResyncReplicationResponse response) {
                    final ReplicationResponse.ShardInfo.Failure[] failures = response.getShardInfo().getFailures();
                    // noinspection ForLoopReplaceableByForEach
                    for (int i = 0; i < failures.length; i++) {
                        final ReplicationResponse.ShardInfo.Failure f = failures[i];
                        logger.info(
                            new ParameterizedMessage(
                                "{} primary-replica resync to replica on node [{}] failed",
                                f.fullShardId(),
                                f.nodeId()
                            ),
                            f.getCause()
                        );
                    }
                    listener.onResponse(response);
                }

                @Override
                public void handleException(TransportException exp) {
                    listener.onFailure(exp);
                }
            }
        );
    }

    /**
     * A proxy for primary-replica resync operations which are performed on replicas when a new primary is promoted.
     * Replica shards fail to execute resync operations will be failed but won't be marked as stale.
     * This avoids marking shards as stale during cluster restart but enforces primary-replica resync mandatory.
     *
     * @opensearch.internal
     */
    class ResyncActionReplicasProxy extends ReplicasProxy {

        @Override
        public void failShardIfNeeded(
            ShardRouting replica,
            long primaryTerm,
            String message,
            Exception exception,
            ActionListener<Void> listener
        ) {
            shardStateAction.remoteShardFailed(
                replica.shardId(),
                replica.allocationId().getId(),
                primaryTerm,
                false,
                message,
                exception,
                listener
            );
        }
    }
}
