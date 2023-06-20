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

import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.client.Requests.cleanupRepositoryRequest;
import static com.colasoft.opensearch.rest.RestRequest.Method.POST;

/**
 * Cleans up a repository
 *
 * @opensearch.api
 */
public class RestCleanupRepositoryAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestCleanupRepositoryAction.class);

    @Override
    public List<Route> routes() {
        return singletonList(new Route(POST, "/_snapshot/{repository}/_cleanup"));
    }

    @Override
    public String getName() {
        return "cleanup_repository_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        CleanupRepositoryRequest cleanupRepositoryRequest = cleanupRepositoryRequest(request.param("repository"));
        cleanupRepositoryRequest.timeout(request.paramAsTime("timeout", cleanupRepositoryRequest.timeout()));
        cleanupRepositoryRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", cleanupRepositoryRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(cleanupRepositoryRequest, request);
        return channel -> client.admin().cluster().cleanupRepository(cleanupRepositoryRequest, new RestToXContentListener<>(channel));
    }
}
