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

package com.colasoft.opensearch.action.admin.cluster.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.OpenSearchTimeoutException;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ClusterStateObserver;
import com.colasoft.opensearch.cluster.ClusterStateObserver.Listener;
import com.colasoft.opensearch.cluster.ClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.coordination.CoordinationMetadata.VotingConfigExclusion;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Priority;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool.Names;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.function.Predicate;

import static com.colasoft.opensearch.action.admin.cluster.configuration.VotingConfigExclusionsHelper.clearExclusionsAndGetState;

/**
 * Transport endpoint action for clearing exclusions to voting config
 *
 * @opensearch.internal
 */
public class TransportClearVotingConfigExclusionsAction extends TransportClusterManagerNodeAction<
    ClearVotingConfigExclusionsRequest,
    ClearVotingConfigExclusionsResponse> {

    private static final Logger logger = LogManager.getLogger(TransportClearVotingConfigExclusionsAction.class);

    @Inject
    public TransportClearVotingConfigExclusionsAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ClearVotingConfigExclusionsAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            ClearVotingConfigExclusionsRequest::new,
            indexNameExpressionResolver
        );
    }

    @Override
    protected String executor() {
        return Names.SAME;
    }

    @Override
    protected ClearVotingConfigExclusionsResponse read(StreamInput in) throws IOException {
        return new ClearVotingConfigExclusionsResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        ClearVotingConfigExclusionsRequest request,
        ClusterState initialState,
        ActionListener<ClearVotingConfigExclusionsResponse> listener
    ) throws Exception {

        final long startTimeMillis = threadPool.relativeTimeInMillis();

        final Predicate<ClusterState> allExclusionsRemoved = newState -> {
            for (VotingConfigExclusion tombstone : initialState.getVotingConfigExclusions()) {
                // NB checking for the existence of any node with this persistent ID, because persistent IDs are how votes are counted.
                if (newState.nodes().nodeExists(tombstone.getNodeId())) {
                    return false;
                }
            }
            return true;
        };

        if (request.getWaitForRemoval() && allExclusionsRemoved.test(initialState) == false) {
            final ClusterStateObserver clusterStateObserver = new ClusterStateObserver(
                initialState,
                clusterService,
                request.getTimeout(),
                logger,
                threadPool.getThreadContext()
            );

            clusterStateObserver.waitForNextChange(new Listener() {
                @Override
                public void onNewClusterState(ClusterState state) {
                    submitClearVotingConfigExclusionsTask(request, startTimeMillis, listener);
                }

                @Override
                public void onClusterServiceClose() {
                    listener.onFailure(
                        new OpenSearchException(
                            "cluster service closed while waiting for removal of nodes " + initialState.getVotingConfigExclusions()
                        )
                    );
                }

                @Override
                public void onTimeout(TimeValue timeout) {
                    listener.onFailure(
                        new OpenSearchTimeoutException(
                            "timed out waiting for removal of nodes; if nodes should not be removed, set waitForRemoval to false. "
                                + initialState.getVotingConfigExclusions()
                        )
                    );
                }
            }, allExclusionsRemoved);
        } else {
            submitClearVotingConfigExclusionsTask(request, startTimeMillis, listener);
        }
    }

    private void submitClearVotingConfigExclusionsTask(
        ClearVotingConfigExclusionsRequest request,
        long startTimeMillis,
        ActionListener<ClearVotingConfigExclusionsResponse> listener
    ) {
        clusterService.submitStateUpdateTask("clear-voting-config-exclusions", new ClusterStateUpdateTask(Priority.URGENT) {
            @Override
            public ClusterState execute(ClusterState currentState) {
                return clearExclusionsAndGetState(currentState);
            }

            @Override
            public void onFailure(String source, Exception e) {
                listener.onFailure(e);
            }

            @Override
            public TimeValue timeout() {
                return TimeValue.timeValueMillis(request.getTimeout().millis() + startTimeMillis - threadPool.relativeTimeInMillis());
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                listener.onResponse(new ClearVotingConfigExclusionsResponse());
            }
        });
    }

    @Override
    protected ClusterBlockException checkBlock(ClearVotingConfigExclusionsRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
