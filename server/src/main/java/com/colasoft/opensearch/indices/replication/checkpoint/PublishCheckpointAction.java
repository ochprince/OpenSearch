/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.indices.replication.checkpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.lucene.store.AlreadyClosedException;
import com.colasoft.opensearch.ExceptionsHelper;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.replication.ReplicationResponse;
import com.colasoft.opensearch.action.support.replication.ReplicationTask;
import com.colasoft.opensearch.action.support.replication.TransportReplicationAction;
import com.colasoft.opensearch.cluster.action.shard.ShardStateAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.index.IndexNotFoundException;
import com.colasoft.opensearch.index.shard.IndexShard;
import com.colasoft.opensearch.index.shard.IndexShardClosedException;
import com.colasoft.opensearch.index.shard.ShardNotInPrimaryModeException;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.replication.SegmentReplicationTargetService;
import com.colasoft.opensearch.indices.replication.common.ReplicationTimer;
import com.colasoft.opensearch.node.NodeClosedException;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Objects;

import com.colasoft.opensearch.action.support.replication.ReplicationMode;

/**
 * Replication action responsible for publishing checkpoint to a replica shard.
 *
 * @opensearch.internal
 */

public class PublishCheckpointAction extends TransportReplicationAction<
    PublishCheckpointRequest,
    PublishCheckpointRequest,
    ReplicationResponse> {

    public static final String ACTION_NAME = "indices:admin/publishCheckpoint";
    protected static Logger logger = LogManager.getLogger(PublishCheckpointAction.class);

    private final SegmentReplicationTargetService replicationService;

    @Inject
    public PublishCheckpointAction(
        Settings settings,
        TransportService transportService,
        ClusterService clusterService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ShardStateAction shardStateAction,
        ActionFilters actionFilters,
        SegmentReplicationTargetService targetService
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
            PublishCheckpointRequest::new,
            PublishCheckpointRequest::new,
            ThreadPool.Names.REFRESH
        );
        this.replicationService = targetService;
    }

    @Override
    protected ReplicationResponse newResponseInstance(StreamInput in) throws IOException {
        return new ReplicationResponse(in);
    }

    @Override
    protected void doExecute(Task task, PublishCheckpointRequest request, ActionListener<ReplicationResponse> listener) {
        assert false : "use PublishCheckpointAction#publish";
    }

    @Override
    public ReplicationMode getReplicationMode(IndexShard indexShard) {
        if (indexShard.isRemoteTranslogEnabled()) {
            return ReplicationMode.FULL_REPLICATION;
        }
        return super.getReplicationMode(indexShard);
    }

    /**
     * Publish checkpoint request to shard
     */
    final void publish(IndexShard indexShard, ReplicationCheckpoint checkpoint) {
        String primaryAllocationId = indexShard.routingEntry().allocationId().getId();
        long primaryTerm = indexShard.getPendingPrimaryTerm();
        final ThreadContext threadContext = threadPool.getThreadContext();
        try (ThreadContext.StoredContext ignore = threadContext.stashContext()) {
            // we have to execute under the system context so that if security is enabled the sync is authorized
            threadContext.markAsSystemContext();
            PublishCheckpointRequest request = new PublishCheckpointRequest(checkpoint);
            final ReplicationTask task = (ReplicationTask) taskManager.register("transport", "segrep_publish_checkpoint", request);
            final ReplicationTimer timer = new ReplicationTimer();
            timer.start();
            transportService.sendChildRequest(
                indexShard.recoveryState().getTargetNode(),
                transportPrimaryAction,
                new ConcreteShardRequest<>(request, primaryAllocationId, primaryTerm),
                task,
                transportOptions,
                new TransportResponseHandler<ReplicationResponse>() {
                    @Override
                    public ReplicationResponse read(StreamInput in) throws IOException {
                        return newResponseInstance(in);
                    }

                    @Override
                    public String executor() {
                        return ThreadPool.Names.SAME;
                    }

                    @Override
                    public void handleResponse(ReplicationResponse response) {
                        timer.stop();
                        logger.trace(
                            () -> new ParameterizedMessage(
                                "[shardId {}] Completed publishing checkpoint [{}], timing: {}",
                                indexShard.shardId().getId(),
                                checkpoint,
                                timer.time()
                            )
                        );
                        task.setPhase("finished");
                        taskManager.unregister(task);
                    }

                    @Override
                    public void handleException(TransportException e) {
                        timer.stop();
                        logger.trace("[shardId {}] Failed to publish checkpoint, timing: {}", indexShard.shardId().getId(), timer.time());
                        task.setPhase("finished");
                        taskManager.unregister(task);
                        if (ExceptionsHelper.unwrap(
                            e,
                            NodeClosedException.class,
                            IndexNotFoundException.class,
                            AlreadyClosedException.class,
                            IndexShardClosedException.class,
                            ShardNotInPrimaryModeException.class
                        ) != null) {
                            // Node is shutting down or the index was deleted or the shard is closed
                            return;
                        }
                        logger.warn(
                            new ParameterizedMessage("{} segment replication checkpoint publishing failed", indexShard.shardId()),
                            e
                        );
                    }
                }
            );
            logger.trace(
                () -> new ParameterizedMessage(
                    "[shardId {}] Publishing replication checkpoint [{}]",
                    checkpoint.getShardId().getId(),
                    checkpoint
                )
            );
        }
    }

    @Override
    protected void shardOperationOnPrimary(
        PublishCheckpointRequest request,
        IndexShard primary,
        ActionListener<PrimaryResult<PublishCheckpointRequest, ReplicationResponse>> listener
    ) {
        ActionListener.completeWith(listener, () -> new PrimaryResult<>(request, new ReplicationResponse()));
    }

    @Override
    protected void shardOperationOnReplica(PublishCheckpointRequest request, IndexShard replica, ActionListener<ReplicaResult> listener) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(replica);
        ActionListener.completeWith(listener, () -> {
            logger.trace(() -> new ParameterizedMessage("Checkpoint {} received on replica {}", request, replica.shardId()));
            if (request.getCheckpoint().getShardId().equals(replica.shardId())) {
                replicationService.onNewCheckpoint(request.getCheckpoint(), replica);
            }
            return new ReplicaResult();
        });
    }
}
