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

package com.colasoft.opensearch.action.admin.cluster.repositories.cleanup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRunnable;
import com.colasoft.opensearch.action.StepListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.RepositoryCleanupInProgress;
import com.colasoft.opensearch.cluster.SnapshotDeletionsInProgress;
import com.colasoft.opensearch.cluster.SnapshotsInProgress;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.repositories.RepositoriesService;
import com.colasoft.opensearch.repositories.Repository;
import com.colasoft.opensearch.repositories.RepositoryCleanupResult;
import com.colasoft.opensearch.repositories.RepositoryData;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreRepository;
import com.colasoft.opensearch.snapshots.SnapshotsService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Collections;

/**
 * Repository cleanup action for repository implementations based on {@link BlobStoreRepository}.
 *
 * The steps taken by the repository cleanup operation are as follows:
 * <ol>
 *     <li>Check that there are no running repository cleanup, snapshot create, or snapshot delete actions
 *     and add an entry for the repository that is to be cleaned up to {@link RepositoryCleanupInProgress}</li>
 *     <li>Run cleanup actions on the repository. Note, these are executed exclusively on the cluster-manager node.
 *     For the precise operations execute see {@link BlobStoreRepository#cleanup}</li>
 *     <li>Remove the entry in {@link RepositoryCleanupInProgress} in the first step.</li>
 * </ol>
 *
 * On cluster-manager failover during the cleanup operation it is simply removed from the cluster state. This is safe because the logic in
 * {@link BlobStoreRepository#cleanup} ensures that the repository state id has not changed between creation of the cluster state entry
 * and any delete/write operations. TODO: This will not work if we also want to clean up at the shard level as those will involve writes
 *                                        as well as deletes.
 *
 * @opensearch.internal
 */
public final class TransportCleanupRepositoryAction extends TransportClusterManagerNodeAction<
    CleanupRepositoryRequest,
    CleanupRepositoryResponse> {

    private static final Logger logger = LogManager.getLogger(TransportCleanupRepositoryAction.class);

    private static final Version MIN_VERSION = LegacyESVersion.V_7_4_0;

    private final RepositoriesService repositoriesService;

    private final SnapshotsService snapshotsService;

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Inject
    public TransportCleanupRepositoryAction(
        TransportService transportService,
        ClusterService clusterService,
        RepositoriesService repositoriesService,
        SnapshotsService snapshotsService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            CleanupRepositoryAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            CleanupRepositoryRequest::new,
            indexNameExpressionResolver
        );
        this.repositoriesService = repositoriesService;
        this.snapshotsService = snapshotsService;
        // We add a state applier that will remove any dangling repository cleanup actions on cluster-manager failover.
        // This is safe to do since cleanups will increment the repository state id before executing any operations to prevent concurrent
        // operations from corrupting the repository. This is the same safety mechanism used by snapshot deletes.
        if (DiscoveryNode.isClusterManagerNode(clusterService.getSettings())) {
            addClusterStateApplier(clusterService);
        }
    }

    private static void addClusterStateApplier(ClusterService clusterService) {
        clusterService.addStateApplier(event -> {
            if (event.localNodeClusterManager() && event.previousState().nodes().isLocalNodeElectedClusterManager() == false) {
                final RepositoryCleanupInProgress repositoryCleanupInProgress = event.state()
                    .custom(RepositoryCleanupInProgress.TYPE, RepositoryCleanupInProgress.EMPTY);
                if (repositoryCleanupInProgress.hasCleanupInProgress() == false) {
                    return;
                }
                clusterService.submitStateUpdateTask(
                    "clean up repository cleanup task after cluster-manager failover",
                    new ClusterStateUpdateTask() {
                        @Override
                        public ClusterState execute(ClusterState currentState) {
                            return removeInProgressCleanup(currentState);
                        }

                        @Override
                        public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                            logger.debug("Removed repository cleanup task [{}] from cluster state", repositoryCleanupInProgress);
                        }

                        @Override
                        public void onFailure(String source, Exception e) {
                            logger.warn("Failed to remove repository cleanup task [{}] from cluster state", repositoryCleanupInProgress);
                        }
                    }
                );
            }
        });
    }

    private static ClusterState removeInProgressCleanup(final ClusterState currentState) {
        return currentState.custom(RepositoryCleanupInProgress.TYPE, RepositoryCleanupInProgress.EMPTY).hasCleanupInProgress()
            ? ClusterState.builder(currentState).putCustom(RepositoryCleanupInProgress.TYPE, RepositoryCleanupInProgress.EMPTY).build()
            : currentState;
    }

    @Override
    protected CleanupRepositoryResponse read(StreamInput in) throws IOException {
        return new CleanupRepositoryResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        CleanupRepositoryRequest request,
        ClusterState state,
        ActionListener<CleanupRepositoryResponse> listener
    ) {
        if (state.nodes().getMinNodeVersion().onOrAfter(MIN_VERSION)) {
            cleanupRepo(request.name(), ActionListener.map(listener, CleanupRepositoryResponse::new));
        } else {
            throw new IllegalArgumentException(
                "Repository cleanup is only supported from version ["
                    + MIN_VERSION
                    + "] but the oldest node version in the cluster is ["
                    + state.nodes().getMinNodeVersion()
                    + ']'
            );
        }
    }

    @Override
    protected ClusterBlockException checkBlock(CleanupRepositoryRequest request, ClusterState state) {
        // Cluster is not affected but we look up repositories in metadata
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    /**
     * Runs cleanup operations on the given repository.
     * @param repositoryName Repository to clean up
     * @param listener Listener for cleanup result
     */
    private void cleanupRepo(String repositoryName, ActionListener<RepositoryCleanupResult> listener) {
        final Repository repository = repositoriesService.repository(repositoryName);
        if (repository instanceof BlobStoreRepository == false) {
            listener.onFailure(new IllegalArgumentException("Repository [" + repositoryName + "] does not support repository cleanup"));
            return;
        }
        final BlobStoreRepository blobStoreRepository = (BlobStoreRepository) repository;
        final StepListener<RepositoryData> repositoryDataListener = new StepListener<>();
        repository.getRepositoryData(repositoryDataListener);
        repositoryDataListener.whenComplete(repositoryData -> {
            final long repositoryStateId = repositoryData.getGenId();
            logger.info("Running cleanup operations on repository [{}][{}]", repositoryName, repositoryStateId);
            clusterService.submitStateUpdateTask(
                "cleanup repository [" + repositoryName + "][" + repositoryStateId + ']',
                new ClusterStateUpdateTask() {

                    private boolean startedCleanup = false;

                    @Override
                    public ClusterState execute(ClusterState currentState) {
                        final RepositoryCleanupInProgress repositoryCleanupInProgress = currentState.custom(
                            RepositoryCleanupInProgress.TYPE,
                            RepositoryCleanupInProgress.EMPTY
                        );
                        if (repositoryCleanupInProgress.hasCleanupInProgress()) {
                            throw new IllegalStateException(
                                "Cannot cleanup ["
                                    + repositoryName
                                    + "] - a repository cleanup is already in-progress in ["
                                    + repositoryCleanupInProgress
                                    + "]"
                            );
                        }
                        final SnapshotDeletionsInProgress deletionsInProgress = currentState.custom(
                            SnapshotDeletionsInProgress.TYPE,
                            SnapshotDeletionsInProgress.EMPTY
                        );
                        if (deletionsInProgress.hasDeletionsInProgress()) {
                            throw new IllegalStateException(
                                "Cannot cleanup ["
                                    + repositoryName
                                    + "] - a snapshot is currently being deleted in ["
                                    + deletionsInProgress
                                    + "]"
                            );
                        }
                        SnapshotsInProgress snapshots = currentState.custom(SnapshotsInProgress.TYPE, SnapshotsInProgress.EMPTY);
                        if (snapshots.entries().isEmpty() == false) {
                            throw new IllegalStateException(
                                "Cannot cleanup [" + repositoryName + "] - a snapshot is currently running in [" + snapshots + "]"
                            );
                        }
                        return ClusterState.builder(currentState)
                            .putCustom(
                                RepositoryCleanupInProgress.TYPE,
                                new RepositoryCleanupInProgress(
                                    Collections.singletonList(RepositoryCleanupInProgress.startedEntry(repositoryName, repositoryStateId))
                                )
                            )
                            .build();
                    }

                    @Override
                    public void onFailure(String source, Exception e) {
                        after(e, null);
                    }

                    @Override
                    public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                        startedCleanup = true;
                        logger.debug("Initialized repository cleanup in cluster state for [{}][{}]", repositoryName, repositoryStateId);
                        threadPool.executor(ThreadPool.Names.SNAPSHOT)
                            .execute(
                                ActionRunnable.wrap(
                                    listener,
                                    l -> blobStoreRepository.cleanup(
                                        repositoryStateId,
                                        snapshotsService.minCompatibleVersion(newState.nodes().getMinNodeVersion(), repositoryData, null),
                                        ActionListener.wrap(result -> after(null, result), e -> after(e, null))
                                    )
                                )
                            );
                    }

                    private void after(@Nullable Exception failure, @Nullable RepositoryCleanupResult result) {
                        if (failure == null) {
                            logger.debug("Finished repository cleanup operations on [{}][{}]", repositoryName, repositoryStateId);
                        } else {
                            logger.debug(
                                () -> new ParameterizedMessage(
                                    "Failed to finish repository cleanup operations on [{}][{}]",
                                    repositoryName,
                                    repositoryStateId
                                ),
                                failure
                            );
                        }
                        assert failure != null || result != null;
                        if (startedCleanup == false) {
                            logger.debug("No cleanup task to remove from cluster state because we failed to start one", failure);
                            listener.onFailure(failure);
                            return;
                        }
                        clusterService.submitStateUpdateTask(
                            "remove repository cleanup task [" + repositoryName + "][" + repositoryStateId + ']',
                            new ClusterStateUpdateTask() {
                                @Override
                                public ClusterState execute(ClusterState currentState) {
                                    return removeInProgressCleanup(currentState);
                                }

                                @Override
                                public void onFailure(String source, Exception e) {
                                    if (failure != null) {
                                        e.addSuppressed(failure);
                                    }
                                    logger.warn(
                                        () -> new ParameterizedMessage("[{}] failed to remove repository cleanup task", repositoryName),
                                        e
                                    );
                                    listener.onFailure(e);
                                }

                                @Override
                                public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                                    if (failure == null) {
                                        logger.info(
                                            "Done with repository cleanup on [{}][{}] with result [{}]",
                                            repositoryName,
                                            repositoryStateId,
                                            result
                                        );
                                        listener.onResponse(result);
                                    } else {
                                        logger.warn(
                                            () -> new ParameterizedMessage(
                                                "Failed to run repository cleanup operations on [{}][{}]",
                                                repositoryName,
                                                repositoryStateId
                                            ),
                                            failure
                                        );
                                        listener.onFailure(failure);
                                    }
                                }
                            }
                        );
                    }
                }
            );
        }, listener::onFailure);
    }
}
