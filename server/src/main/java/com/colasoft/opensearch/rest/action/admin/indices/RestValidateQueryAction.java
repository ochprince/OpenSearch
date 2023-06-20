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

import com.colasoft.opensearch.action.admin.indices.validate.query.QueryExplanation;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryRequest;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryResponse;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.ParsingException;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestChannel;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestActions;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;
import static com.colasoft.opensearch.rest.RestRequest.Method.POST;
import static com.colasoft.opensearch.rest.RestStatus.OK;

/**
 * Transport action to validate a query
 *
 * @opensearch.api
 */
public class RestValidateQueryAction extends BaseRestHandler {
    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                new Route(GET, "/_validate/query"),
                new Route(POST, "/_validate/query"),
                new Route(GET, "/{index}/_validate/query"),
                new Route(POST, "/{index}/_validate/query")
            )
        );
    }

    @Override
    public String getName() {
        return "validate_query_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        ValidateQueryRequest validateQueryRequest = new ValidateQueryRequest(Strings.splitStringByCommaToArray(request.param("index")));
        validateQueryRequest.indicesOptions(IndicesOptions.fromRequest(request, validateQueryRequest.indicesOptions()));
        validateQueryRequest.explain(request.paramAsBoolean("explain", false));

        validateQueryRequest.rewrite(request.paramAsBoolean("rewrite", false));
        validateQueryRequest.allShards(request.paramAsBoolean("all_shards", false));

        Exception bodyParsingException = null;
        try {
            request.withContentOrSourceParamParserOrNull(parser -> {
                if (parser != null) {
                    validateQueryRequest.query(RestActions.getQueryContent(parser));
                } else if (request.hasParam("q")) {
                    validateQueryRequest.query(RestActions.urlParamsToQueryBuilder(request));
                }
            });
        } catch (Exception e) {
            bodyParsingException = e;
        }

        final Exception finalBodyParsingException = bodyParsingException;
        return channel -> {
            if (finalBodyParsingException != null) {
                if (finalBodyParsingException instanceof ParsingException) {
                    handleException(validateQueryRequest, ((ParsingException) finalBodyParsingException).getDetailedMessage(), channel);
                } else {
                    handleException(validateQueryRequest, finalBodyParsingException.getMessage(), channel);
                }
            } else {
                client.admin().indices().validateQuery(validateQueryRequest, new RestToXContentListener<>(channel));
            }
        };
    }

    private void handleException(final ValidateQueryRequest request, final String message, final RestChannel channel) throws IOException {
        channel.sendResponse(buildErrorResponse(channel.newBuilder(), message, request.explain()));
    }

    private static BytesRestResponse buildErrorResponse(XContentBuilder builder, String error, boolean explain) throws IOException {
        builder.startObject();
        builder.field(ValidateQueryResponse.VALID_FIELD, false);
        if (explain) {
            builder.field(QueryExplanation.ERROR_FIELD, error);
        }
        builder.endObject();
        return new BytesRestResponse(OK, builder);
    }
}
