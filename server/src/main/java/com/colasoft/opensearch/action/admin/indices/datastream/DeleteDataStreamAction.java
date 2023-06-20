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

package com.colasoft.opensearch.action.admin.indices.datastream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.IndicesRequest;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.DataStream;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.metadata.MetadataDeleteIndexService;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskKeys;
import com.colasoft.opensearch.cluster.service.ClusterManagerTaskThrottler;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Priority;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.regex.Regex;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.common.util.CollectionUtils;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.snapshots.SnapshotInProgressException;
import com.colasoft.opensearch.snapshots.SnapshotsService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.colasoft.opensearch.action.ValidateActions.addValidationError;

/**
 * Transport action for deleting a datastream
 *
 * @opensearch.internal
 */
public class DeleteDataStreamAction extends ActionType<AcknowledgedResponse> {

    private static final Logger logger = LogManager.getLogger(DeleteDataStreamAction.class);

    public static final DeleteDataStreamAction INSTANCE = new DeleteDataStreamAction();
    public static final String NAME = "indices:admin/data_stream/delete";

    private DeleteDataStreamAction() {
        super(NAME, AcknowledgedResponse::new);
    }

    /**
     * Request for deleting data streams
     *
     * @opensearch.internal
     */
    public static class Request extends ClusterManagerNodeRequest<Request> implements IndicesRequest.Replaceable {

        private String[] names;

        public Request(String[] names) {
            this.names = Objects.requireNonNull(names);
        }

        @Override
        public ActionRequestValidationException validate() {
            ActionRequestValidationException validationException = null;
            if (CollectionUtils.isEmpty(names)) {
                validationException = addValidationError("no data stream(s) specified", validationException);
            }
            return validationException;
        }

        public Request(StreamInput in) throws IOException {
            super(in);
            this.names = in.readStringArray();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeStringArray(names);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Arrays.equals(names, request.names);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(names);
        }

        @Override
        public String[] indices() {
            return names;
        }

        @Override
        public IndicesOptions indicesOptions() {
            // this doesn't really matter since data stream name resolution isn't affected by IndicesOptions and
            // a data stream's backing indices are retrieved from its metadata
            return IndicesOptions.fromOptions(false, true, true, true, false, false, true, false);
        }

        @Override
        public boolean includeDataStreams() {
            return true;
        }

        @Override
        public IndicesRequest indices(String... indices) {
            this.names = indices;
            return this;
        }
    }

    /**
     * Transport action for deleting data streams
     *
     * @opensearch.internal
     */
    public static class TransportAction extends TransportClusterManagerNodeAction<Request, AcknowledgedResponse> {

        private final MetadataDeleteIndexService deleteIndexService;
        private final ClusterManagerTaskThrottler.ThrottlingKey removeDataStreamTaskKey;

        @Inject
        public TransportAction(
            TransportService transportService,
            ClusterService clusterService,
            ThreadPool threadPool,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            MetadataDeleteIndexService deleteIndexService
        ) {
            super(NAME, transportService, clusterService, threadPool, actionFilters, Request::new, indexNameExpressionResolver);
            this.deleteIndexService = deleteIndexService;
            // Task is onboarded for throttling, it will get retried from associated TransportClusterManagerNodeAction.
            removeDataStreamTaskKey = clusterService.registerClusterManagerTask(ClusterManagerTaskKeys.REMOVE_DATA_STREAM_KEY, true);
        }

        @Override
        protected String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        protected AcknowledgedResponse read(StreamInput in) throws IOException {
            return new AcknowledgedResponse(in);
        }

        @Override
        protected void clusterManagerOperation(Request request, ClusterState state, ActionListener<AcknowledgedResponse> listener)
            throws Exception {
            clusterService.submitStateUpdateTask(
                "remove-data-stream [" + Strings.arrayToCommaDelimitedString(request.names) + "]",
                new ClusterStateUpdateTask(Priority.HIGH) {

                    @Override
                    public TimeValue timeout() {
                        return request.clusterManagerNodeTimeout();
                    }

                    @Override
                    public void onFailure(String source, Exception e) {
                        listener.onFailure(e);
                    }

                    @Override
                    public ClusterManagerTaskThrottler.ThrottlingKey getClusterManagerThrottlingKey() {
                        return removeDataStreamTaskKey;
                    }

                    @Override
                    public ClusterState execute(ClusterState currentState) {
                        return removeDataStream(deleteIndexService, currentState, request);
                    }

                    @Override
                    public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                        listener.onResponse(new AcknowledgedResponse(true));
                    }
                }
            );
        }

        static ClusterState removeDataStream(MetadataDeleteIndexService deleteIndexService, ClusterState currentState, Request request) {
            Set<String> dataStreams = new HashSet<>();
            Set<String> snapshottingDataStreams = new HashSet<>();
            for (String name : request.names) {
                for (String dataStreamName : currentState.metadata().dataStreams().keySet()) {
                    if (Regex.simpleMatch(name, dataStreamName)) {
                        dataStreams.add(dataStreamName);
                    }
                }

                snapshottingDataStreams.addAll(SnapshotsService.snapshottingDataStreams(currentState, dataStreams));
            }

            if (snapshottingDataStreams.isEmpty() == false) {
                throw new SnapshotInProgressException(
                    "Cannot delete data streams that are being snapshotted: "
                        + snapshottingDataStreams
                        + ". Try again after snapshot finishes or cancel the currently running snapshot."
                );
            }

            Set<Index> backingIndicesToRemove = new HashSet<>();
            for (String dataStreamName : dataStreams) {
                DataStream dataStream = currentState.metadata().dataStreams().get(dataStreamName);
                assert dataStream != null;
                backingIndicesToRemove.addAll(dataStream.getIndices());
            }

            // first delete the data streams and then the indices:
            // (this to avoid data stream validation from failing when deleting an index that is part of a data stream
            // without updating the data stream)
            // TODO: change order when delete index api also updates the data stream the index to be removed is member of
            Metadata.Builder metadata = Metadata.builder(currentState.metadata());
            for (String ds : dataStreams) {
                logger.info("removing data stream [{}]", ds);
                metadata.removeDataStream(ds);
            }
            currentState = ClusterState.builder(currentState).metadata(metadata).build();
            return deleteIndexService.deleteIndices(currentState, backingIndicesToRemove);
        }

        @Override
        protected ClusterBlockException checkBlock(Request request, ClusterState state) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }
    }

}
