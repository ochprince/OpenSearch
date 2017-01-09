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

package org.elasticsearch.rest.action.admin.indices;

import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.RestBuilderListener;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.RestActions.buildBroadcastShardsHeader;

public class RestClearIndicesCacheAction extends BaseRestHandler {

    @Inject
    public RestClearIndicesCacheAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/_cache/clear", this);
        controller.registerHandler(POST, "/{index}/_cache/clear", this);

        controller.registerHandler(GET, "/_cache/clear", this);
        controller.registerHandler(GET, "/{index}/_cache/clear", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        ClearIndicesCacheRequest clearIndicesCacheRequest = new ClearIndicesCacheRequest(
                Strings.splitStringByCommaToArray(request.param("index")));
        clearIndicesCacheRequest.indicesOptions(IndicesOptions.fromRequest(request, clearIndicesCacheRequest.indicesOptions()));
        fromRequest(request, clearIndicesCacheRequest);
        return channel ->
                client.admin().indices().clearCache(clearIndicesCacheRequest, new RestBuilderListener<ClearIndicesCacheResponse>(channel) {
            @Override
            public RestResponse buildResponse(ClearIndicesCacheResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                buildBroadcastShardsHeader(builder, request, response);
                builder.endObject();
                return new BytesRestResponse(OK, builder);
            }
        });
    }

    @Override
    public boolean canTripCircuitBreaker() {
        return false;
    }

    public static ClearIndicesCacheRequest fromRequest(final RestRequest request, ClearIndicesCacheRequest clearIndicesCacheRequest) {

        for (Map.Entry<String, String> entry : request.params().entrySet()) {
            if (Fields.QUERY.match(entry.getKey())) {
                clearIndicesCacheRequest.queryCache(request.paramAsBoolean(entry.getKey(), clearIndicesCacheRequest.queryCache()));
            }
            if (Fields.REQUEST_CACHE.match(entry.getKey())) {
                clearIndicesCacheRequest.requestCache(request.paramAsBoolean(entry.getKey(), clearIndicesCacheRequest.requestCache()));
            }
            if (Fields.FIELD_DATA.match(entry.getKey())) {
                clearIndicesCacheRequest.fieldDataCache(request.paramAsBoolean(entry.getKey(), clearIndicesCacheRequest.fieldDataCache()));
            }
            if (Fields.RECYCLER.match(entry.getKey())) {
                clearIndicesCacheRequest.recycler(request.paramAsBoolean(entry.getKey(), clearIndicesCacheRequest.recycler()));
            }
            if (Fields.FIELDS.match(entry.getKey())) {
                clearIndicesCacheRequest.fields(request.paramAsStringArray(entry.getKey(), clearIndicesCacheRequest.fields()));
            }
        }

        return clearIndicesCacheRequest;
    }

    public static class Fields {
        public static final ParseField QUERY = new ParseField("query", "filter", "filter_cache");
        public static final ParseField REQUEST_CACHE = new ParseField("request_cache");
        public static final ParseField FIELD_DATA = new ParseField("field_data", "fielddata");
        public static final ParseField RECYCLER = new ParseField("recycler");
        public static final ParseField FIELDS = new ParseField("fields");
    }

}
