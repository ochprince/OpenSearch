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

import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresAction;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresRequest;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresResponse;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestResponse;
import com.colasoft.opensearch.rest.action.RestBuilderListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;
import static com.colasoft.opensearch.rest.RestStatus.OK;

/**
 * Rest action for {@link IndicesShardStoresAction}
 *
 * @opensearch.api
 */
public class RestIndicesShardStoresAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_shard_stores"), new Route(GET, "/{index}/_shard_stores")));
    }

    @Override
    public String getName() {
        return "indices_shard_stores_action";
    }

    @Override
    public boolean allowSystemIndexAccessByDefault() {
        return true;
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        IndicesShardStoresRequest indicesShardStoresRequest = new IndicesShardStoresRequest(
            Strings.splitStringByCommaToArray(request.param("index"))
        );
        if (request.hasParam("status")) {
            indicesShardStoresRequest.shardStatuses(Strings.splitStringByCommaToArray(request.param("status")));
        }
        indicesShardStoresRequest.indicesOptions(IndicesOptions.fromRequest(request, indicesShardStoresRequest.indicesOptions()));
        return channel -> client.admin()
            .indices()
            .shardStores(indicesShardStoresRequest, new RestBuilderListener<IndicesShardStoresResponse>(channel) {
                @Override
                public RestResponse buildResponse(IndicesShardStoresResponse response, XContentBuilder builder) throws Exception {
                    builder.startObject();
                    response.toXContent(builder, request);
                    builder.endObject();
                    return new BytesRestResponse(OK, builder);
                }
            });
    }
}
