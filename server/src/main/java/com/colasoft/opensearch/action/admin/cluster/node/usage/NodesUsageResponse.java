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

package com.colasoft.opensearch.action.admin.cluster.node.usage;

import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.nodes.BaseNodesResponse;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ToXContentFragment;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.List;

/**
 * The response for the nodes usage api which contains the individual usage
 * statistics for all nodes queried.
 *
 * @opensearch.internal
 */
public class NodesUsageResponse extends BaseNodesResponse<NodeUsage> implements ToXContentFragment {

    public NodesUsageResponse(StreamInput in) throws IOException {
        super(in);
    }

    public NodesUsageResponse(ClusterName clusterName, List<NodeUsage> nodes, List<FailedNodeException> failures) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<NodeUsage> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(NodeUsage::new);
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<NodeUsage> nodes) throws IOException {
        out.writeList(nodes);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject("nodes");
        for (NodeUsage nodeUsage : getNodes()) {
            builder.startObject(nodeUsage.getNode().getId());
            builder.field("timestamp", nodeUsage.getTimestamp());
            nodeUsage.toXContent(builder, params);

            builder.endObject();
        }
        builder.endObject();

        return builder;
    }

    @Override
    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
            builder.startObject();
            toXContent(builder, EMPTY_PARAMS);
            builder.endObject();
            return Strings.toString(builder);
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

}
