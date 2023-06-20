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

package com.colasoft.opensearch.action.admin.indices.dangling.find;

import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.nodes.BaseNodesResponse;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;

/**
 * Models a response to a {@link FindDanglingIndexRequest}. A find request queries every node in the
 * cluster looking for a dangling index with a specific UUID.
 *
 * @opensearch.internal
 */
public class FindDanglingIndexResponse extends BaseNodesResponse<NodeFindDanglingIndexResponse> {

    public FindDanglingIndexResponse(StreamInput in) throws IOException {
        super(in);
    }

    public FindDanglingIndexResponse(
        ClusterName clusterName,
        List<NodeFindDanglingIndexResponse> nodes,
        List<FailedNodeException> failures
    ) {
        super(clusterName, nodes, failures);
    }

    @Override
    protected List<NodeFindDanglingIndexResponse> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(NodeFindDanglingIndexResponse::new);
    }

    @Override
    protected void writeNodesTo(StreamOutput out, List<NodeFindDanglingIndexResponse> nodes) throws IOException {
        out.writeList(nodes);
    }
}
