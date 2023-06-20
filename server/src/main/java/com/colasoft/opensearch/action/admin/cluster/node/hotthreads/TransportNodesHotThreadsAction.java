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

package com.colasoft.opensearch.action.admin.cluster.node.hotthreads;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.nodes.BaseNodeRequest;
import com.colasoft.opensearch.action.support.nodes.TransportNodesAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.monitor.jvm.HotThreads;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

/**
 * Transport action for OpenSearch Hot Threads
 *
 * @opensearch.internal
 */
public class TransportNodesHotThreadsAction extends TransportNodesAction<
    NodesHotThreadsRequest,
    NodesHotThreadsResponse,
    TransportNodesHotThreadsAction.NodeRequest,
    NodeHotThreads> {

    @Inject
    public TransportNodesHotThreadsAction(
        ThreadPool threadPool,
        ClusterService clusterService,
        TransportService transportService,
        ActionFilters actionFilters
    ) {
        super(
            NodesHotThreadsAction.NAME,
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            NodesHotThreadsRequest::new,
            NodeRequest::new,
            ThreadPool.Names.GENERIC,
            NodeHotThreads.class
        );
    }

    @Override
    protected NodesHotThreadsResponse newResponse(
        NodesHotThreadsRequest request,
        List<NodeHotThreads> responses,
        List<FailedNodeException> failures
    ) {
        return new NodesHotThreadsResponse(clusterService.getClusterName(), responses, failures);
    }

    @Override
    protected NodeRequest newNodeRequest(NodesHotThreadsRequest request) {
        return new NodeRequest(request);
    }

    @Override
    protected NodeHotThreads newNodeResponse(StreamInput in) throws IOException {
        return new NodeHotThreads(in);
    }

    @Override
    protected NodeHotThreads nodeOperation(NodeRequest request) {
        HotThreads hotThreads = new HotThreads().busiestThreads(request.request.threads)
            .type(request.request.type)
            .interval(request.request.interval)
            .threadElementsSnapshotCount(request.request.snapshots)
            .ignoreIdleThreads(request.request.ignoreIdleThreads);
        try {
            return new NodeHotThreads(clusterService.localNode(), hotThreads.detect());
        } catch (Exception e) {
            throw new OpenSearchException("failed to detect hot threads", e);
        }
    }

    /**
     * Inner node request
     *
     * @opensearch.internal
     */
    public static class NodeRequest extends BaseNodeRequest {

        NodesHotThreadsRequest request;

        public NodeRequest(StreamInput in) throws IOException {
            super(in);
            request = new NodesHotThreadsRequest(in);
        }

        NodeRequest(NodesHotThreadsRequest request) {
            this.request = request;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            request.writeTo(out);
        }
    }
}
