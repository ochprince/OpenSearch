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

import com.colasoft.opensearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.client.Requests.putRepositoryRequest;
import static com.colasoft.opensearch.rest.RestRequest.Method.POST;
import static com.colasoft.opensearch.rest.RestRequest.Method.PUT;

/**
 * Registers repositories
 *
 * @opensearch.api
 */
public class RestPutRepositoryAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestPutRepositoryAction.class);

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(POST, "/_snapshot/{repository}"), new Route(PUT, "/_snapshot/{repository}")));
    }

    @Override
    public String getName() {
        return "put_repository_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        PutRepositoryRequest putRepositoryRequest = putRepositoryRequest(request.param("repository"));
        try (XContentParser parser = request.contentParser()) {
            putRepositoryRequest.source(parser.mapOrdered());
        }
        putRepositoryRequest.verify(request.paramAsBoolean("verify", true));
        putRepositoryRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", putRepositoryRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(putRepositoryRequest, request);
        putRepositoryRequest.timeout(request.paramAsTime("timeout", putRepositoryRequest.timeout()));
        return channel -> client.admin().cluster().putRepository(putRepositoryRequest, new RestToXContentListener<>(channel));
    }
}
