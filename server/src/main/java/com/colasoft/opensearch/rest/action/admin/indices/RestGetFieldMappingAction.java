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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetadata;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestResponse;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.rest.action.RestBuilderListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;
import static com.colasoft.opensearch.rest.RestStatus.NOT_FOUND;
import static com.colasoft.opensearch.rest.RestStatus.OK;

/**
 * Transport action to get field mapping
 *
 * @opensearch.api
 */
public class RestGetFieldMappingAction extends BaseRestHandler {
    private static final Logger logger = LogManager.getLogger(RestGetFieldMappingAction.class);
    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(logger.getName());

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_mapping/field/{fields}"), new Route(GET, "/{index}/_mapping/field/{fields}")));
    }

    @Override
    public String getName() {
        return "get_field_mapping_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        final String[] indices = Strings.splitStringByCommaToArray(request.param("index"));
        final String[] fields = Strings.splitStringByCommaToArray(request.param("fields"));

        GetFieldMappingsRequest getMappingsRequest = new GetFieldMappingsRequest();
        getMappingsRequest.indices(indices).fields(fields).includeDefaults(request.paramAsBoolean("include_defaults", false));
        getMappingsRequest.indicesOptions(IndicesOptions.fromRequest(request, getMappingsRequest.indicesOptions()));

        if (request.hasParam("local")) {
            deprecationLogger.deprecate(
                "get_field_mapping_local",
                "Use [local] in get field mapping requests is deprecated. " + "The parameter will be removed in the next major version"
            );
        }
        getMappingsRequest.local(request.paramAsBoolean("local", getMappingsRequest.local()));
        return channel -> client.admin()
            .indices()
            .getFieldMappings(getMappingsRequest, new RestBuilderListener<GetFieldMappingsResponse>(channel) {
                @Override
                public RestResponse buildResponse(GetFieldMappingsResponse response, XContentBuilder builder) throws Exception {
                    Map<String, Map<String, FieldMappingMetadata>> mappingsByIndex = response.mappings();

                    RestStatus status = OK;
                    if (mappingsByIndex.isEmpty() && fields.length > 0) {
                        status = NOT_FOUND;
                    }
                    response.toXContent(builder, request);
                    return new BytesRestResponse(status, builder);
                }
            });
    }

}
