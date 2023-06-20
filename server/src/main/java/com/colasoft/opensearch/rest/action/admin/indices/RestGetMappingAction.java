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

package com.colasoft.opensearch.rest.action.admin.indices;

import com.colasoft.opensearch.OpenSearchParseException;
import com.colasoft.opensearch.OpenSearchTimeoutException;
import com.colasoft.opensearch.action.ActionRunnable;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsResponse;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestResponse;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.rest.action.RestActionListener;
import com.colasoft.opensearch.rest.action.RestBuilderListener;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * Transport action to get mapping
 *
 * @opensearch.api
 */
public class RestGetMappingAction extends BaseRestHandler {

    private final ThreadPool threadPool;

    public RestGetMappingAction(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                new Route(GET, "/_mapping"),
                new Route(GET, "/_mappings"),
                new Route(GET, "/{index}/_mapping"),
                new Route(GET, "/{index}/_mappings")
            )
        );
    }

    @Override
    public String getName() {
        return "get_mapping_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        final String[] indices = Strings.splitStringByCommaToArray(request.param("index"));

        final GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
        getMappingsRequest.indices(indices);
        getMappingsRequest.indicesOptions(IndicesOptions.fromRequest(request, getMappingsRequest.indicesOptions()));
        TimeValue clusterManagerTimeout = request.paramAsTime("cluster_manager_timeout", getMappingsRequest.clusterManagerNodeTimeout());
        // Remove the if condition and statements inside after removing MASTER_ROLE.
        if (request.hasParam("master_timeout")) {
            if (request.hasParam("cluster_manager_timeout")) {
                throw new OpenSearchParseException(DUPLICATE_PARAMETER_ERROR_MESSAGE);
            }
            clusterManagerTimeout = request.paramAsTime("master_timeout", getMappingsRequest.clusterManagerNodeTimeout());
        }
        final TimeValue timeout = clusterManagerTimeout;
        getMappingsRequest.clusterManagerNodeTimeout(timeout);
        getMappingsRequest.local(request.paramAsBoolean("local", getMappingsRequest.local()));
        return channel -> client.admin().indices().getMappings(getMappingsRequest, new RestActionListener<GetMappingsResponse>(channel) {

            @Override
            protected void processResponse(GetMappingsResponse getMappingsResponse) {
                final long startTimeMs = threadPool.relativeTimeInMillis();
                // Process serialization on GENERIC pool since the serialization of the raw mappings to XContent can be too slow to execute
                // on an IO thread
                threadPool.executor(ThreadPool.Names.MANAGEMENT)
                    .execute(ActionRunnable.wrap(this, l -> new RestBuilderListener<GetMappingsResponse>(channel) {
                        @Override
                        public RestResponse buildResponse(final GetMappingsResponse response, final XContentBuilder builder)
                            throws Exception {
                            if (threadPool.relativeTimeInMillis() - startTimeMs > timeout.millis()) {
                                throw new OpenSearchTimeoutException("Timed out getting mappings");
                            }
                            builder.startObject();
                            response.toXContent(builder, request);
                            builder.endObject();
                            return new BytesRestResponse(RestStatus.OK, builder);
                        }
                    }.onResponse(getMappingsResponse)));
            }
        });
    }
}
