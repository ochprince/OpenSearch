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

package com.colasoft.opensearch.action.admin.cluster.node.info;

import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.nodes.BaseNodesResponse;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.core.xcontent.ToXContentFragment;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.http.HttpInfo;
import com.colasoft.opensearch.ingest.IngestInfo;
import com.colasoft.opensearch.monitor.jvm.JvmInfo;
import com.colasoft.opensearch.monitor.os.OsInfo;
import com.colasoft.opensearch.monitor.process.ProcessInfo;
import com.colasoft.opensearch.search.aggregations.support.AggregationInfo;
import com.colasoft.opensearch.search.pipeline.SearchPipelineInfo;
import com.colasoft.opensearch.threadpool.ThreadPoolInfo;
import com.colasoft.opensearch.transport.TransportInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Transport response for OpenSearch Node Information
 *
 * @opensearch.internal
 */
public class NodesInfoResponse extends BaseNodesResponse<NodeInfo> implements ToXContentFragment {

    public NodesInfoResponse(StreamInput in) throws IOException {
        super(in);
    }

    public NodesInfoResponse(ClusterName clusterName, List<NodeInfo> nodes, List<FailedNodeException> failures) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<NodeInfo> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(NodeInfo::new);
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<NodeInfo> nodes) throws IOException {
        out.writeList(nodes);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject("nodes");
        for (NodeInfo nodeInfo : getNodes()) {
            builder.startObject(nodeInfo.getNode().getId());

            builder.field("name", nodeInfo.getNode().getName());
            builder.field("transport_address", nodeInfo.getNode().getAddress().toString());
            builder.field("host", nodeInfo.getNode().getHostName());
            builder.field("ip", nodeInfo.getNode().getHostAddress());

            builder.field("version", nodeInfo.getVersion());
            builder.field("build_type", nodeInfo.getBuild().type().displayName());
            builder.field("build_hash", nodeInfo.getBuild().hash());
            if (nodeInfo.getTotalIndexingBuffer() != null) {
                builder.humanReadableField("total_indexing_buffer", "total_indexing_buffer_in_bytes", nodeInfo.getTotalIndexingBuffer());
            }

            builder.startArray("roles");
            for (DiscoveryNodeRole role : nodeInfo.getNode().getRoles()) {
                builder.value(role.roleName());
            }
            builder.endArray();

            if (!nodeInfo.getNode().getAttributes().isEmpty()) {
                builder.startObject("attributes");
                for (Map.Entry<String, String> entry : nodeInfo.getNode().getAttributes().entrySet()) {
                    builder.field(entry.getKey(), entry.getValue());
                }
                builder.endObject();
            }

            if (nodeInfo.getSettings() != null) {
                builder.startObject("settings");
                Settings settings = nodeInfo.getSettings();
                settings.toXContent(builder, params);
                builder.endObject();
            }

            if (nodeInfo.getInfo(OsInfo.class) != null) {
                nodeInfo.getInfo(OsInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(ProcessInfo.class) != null) {
                nodeInfo.getInfo(ProcessInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(JvmInfo.class) != null) {
                nodeInfo.getInfo(JvmInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(ThreadPoolInfo.class) != null) {
                nodeInfo.getInfo(ThreadPoolInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(TransportInfo.class) != null) {
                nodeInfo.getInfo(TransportInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(HttpInfo.class) != null) {
                nodeInfo.getInfo(HttpInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(PluginsAndModules.class) != null) {
                nodeInfo.getInfo(PluginsAndModules.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(IngestInfo.class) != null) {
                nodeInfo.getInfo(IngestInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(AggregationInfo.class) != null) {
                nodeInfo.getInfo(AggregationInfo.class).toXContent(builder, params);
            }
            if (nodeInfo.getInfo(SearchPipelineInfo.class) != null) {
                nodeInfo.getInfo(SearchPipelineInfo.class).toXContent(builder, params);
            }

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
