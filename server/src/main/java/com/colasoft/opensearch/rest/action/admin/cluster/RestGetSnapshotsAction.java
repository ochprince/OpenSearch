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

package com.colasoft.opensearch.rest.action.admin.cluster;

import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.client.Requests.getSnapshotsRequest;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * Returns information about snapshot
 *
 * @opensearch.api
 */
public class RestGetSnapshotsAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestGetSnapshotsAction.class);

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_snapshot/{repository}/{snapshot}"));
    }

    @Override
    public String getName() {
        return "get_snapshots_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        String repository = request.param("repository");
        String[] snapshots = request.paramAsStringArray("snapshot", Strings.EMPTY_ARRAY);

        GetSnapshotsRequest getSnapshotsRequest = getSnapshotsRequest(repository).snapshots(snapshots);
        getSnapshotsRequest.ignoreUnavailable(request.paramAsBoolean("ignore_unavailable", getSnapshotsRequest.ignoreUnavailable()));
        getSnapshotsRequest.verbose(request.paramAsBoolean("verbose", getSnapshotsRequest.verbose()));
        getSnapshotsRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", getSnapshotsRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(getSnapshotsRequest, request);
        return channel -> client.admin().cluster().getSnapshots(getSnapshotsRequest, new RestToXContentListener<>(channel));
    }
}
