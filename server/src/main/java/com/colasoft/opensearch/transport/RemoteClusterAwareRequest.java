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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.transport;

import com.colasoft.opensearch.cluster.node.DiscoveryNode;

/**
 * Request for remote clusters
 *
 * @opensearch.internal
 */
public interface RemoteClusterAwareRequest {

    /**
     * Returns the preferred discovery node for this request. The remote cluster client will attempt to send
     * this request directly to this node. Otherwise, it will send the request as a proxy action that will
     * be routed by the remote cluster to this node.
     *
     * @return preferred discovery node
     */
    DiscoveryNode getPreferredTargetNode();

}
