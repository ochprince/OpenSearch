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

package org.opensearch.action.support.master;

import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionResponse;
import org.opensearch.cluster.ClusterState;

public class TransportMasterNodeActionUtils {

    /**
     * Allows to directly call {@link TransportMasterNodeAction#masterOperation(MasterNodeRequest, ClusterState, ActionListener)} which is
     * a protected method.
     */
    public static <Request extends MasterNodeRequest<Request>, Response extends ActionResponse> void runMasterOperation(
        TransportMasterNodeAction<Request, Response> masterNodeAction,
        Request request,
        ClusterState clusterState,
        ActionListener<Response> actionListener
    ) throws Exception {
        assert masterNodeAction.checkBlock(request, clusterState) == null;
        masterNodeAction.masterOperation(request, clusterState, actionListener);
    }
}
