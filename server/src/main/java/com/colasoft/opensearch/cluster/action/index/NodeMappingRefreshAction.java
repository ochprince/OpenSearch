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

package com.colasoft.opensearch.cluster.action.index;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.IndicesRequest;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.metadata.MetadataMappingService;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.EmptyTransportResponseHandler;
import com.colasoft.opensearch.transport.TransportChannel;
import com.colasoft.opensearch.transport.TransportRequest;
import com.colasoft.opensearch.transport.TransportRequestHandler;
import com.colasoft.opensearch.transport.TransportResponse;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;

/**
 * Transport action for refreshing the Node Mapping
 *
 * @opensearch.internal
 */
public class NodeMappingRefreshAction {

    private static final Logger logger = LogManager.getLogger(NodeMappingRefreshAction.class);

    public static final String ACTION_NAME = "internal:cluster/node/mapping/refresh";

    private final TransportService transportService;
    private final MetadataMappingService metadataMappingService;

    @Inject
    public NodeMappingRefreshAction(TransportService transportService, MetadataMappingService metadataMappingService) {
        this.transportService = transportService;
        this.metadataMappingService = metadataMappingService;
        transportService.registerRequestHandler(
            ACTION_NAME,
            ThreadPool.Names.SAME,
            NodeMappingRefreshRequest::new,
            new NodeMappingRefreshTransportHandler()
        );
    }

    public void nodeMappingRefresh(final DiscoveryNode clusterManagerNode, final NodeMappingRefreshRequest request) {
        if (clusterManagerNode == null) {
            logger.warn("can't send mapping refresh for [{}], no cluster-manager known.", request.index());
            return;
        }
        transportService.sendRequest(clusterManagerNode, ACTION_NAME, request, EmptyTransportResponseHandler.INSTANCE_SAME);
    }

    /**
     * A handler for a node mapping refresh transport request.
     *
     * @opensearch.internal
     */
    private class NodeMappingRefreshTransportHandler implements TransportRequestHandler<NodeMappingRefreshRequest> {

        @Override
        public void messageReceived(NodeMappingRefreshRequest request, TransportChannel channel, Task task) throws Exception {
            metadataMappingService.refreshMapping(request.index(), request.indexUUID());
            channel.sendResponse(TransportResponse.Empty.INSTANCE);
        }
    }

    /**
     * Request to refresh node mapping.
     *
     * @opensearch.internal
     */
    public static class NodeMappingRefreshRequest extends TransportRequest implements IndicesRequest {

        private String index;
        private String indexUUID = IndexMetadata.INDEX_UUID_NA_VALUE;
        private String nodeId;

        public NodeMappingRefreshRequest(StreamInput in) throws IOException {
            super(in);
            index = in.readString();
            nodeId = in.readString();
            indexUUID = in.readString();
        }

        public NodeMappingRefreshRequest(String index, String indexUUID, String nodeId) {
            this.index = index;
            this.indexUUID = indexUUID;
            this.nodeId = nodeId;
        }

        @Override
        public String[] indices() {
            return new String[] { index };
        }

        @Override
        public IndicesOptions indicesOptions() {
            return IndicesOptions.strictSingleIndexNoExpandForbidClosed();
        }

        public String index() {
            return index;
        }

        public String indexUUID() {
            return indexUUID;
        }

        public String nodeId() {
            return nodeId;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(index);
            out.writeString(nodeId);
            out.writeString(indexUUID);
        }
    }
}
