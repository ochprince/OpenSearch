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

import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeAction;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;
import static com.colasoft.opensearch.rest.RestRequest.Method.POST;

/**
 * Transport action to analyze an action
 *
 * @opensearch.api
 */
public class RestAnalyzeAction extends BaseRestHandler {

    /**
     * Fields for parsing and toXContent
     *
     * @opensearch.internal
     */
    public static class Fields {
        public static final ParseField ANALYZER = new ParseField("analyzer");
        public static final ParseField TEXT = new ParseField("text");
        public static final ParseField FIELD = new ParseField("field");
        public static final ParseField TOKENIZER = new ParseField("tokenizer");
        public static final ParseField TOKEN_FILTERS = new ParseField("filter");
        public static final ParseField CHAR_FILTERS = new ParseField("char_filter");
        public static final ParseField EXPLAIN = new ParseField("explain");
        public static final ParseField ATTRIBUTES = new ParseField("attributes");
        public static final ParseField NORMALIZER = new ParseField("normalizer");
    }

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(
                new Route(GET, "/_analyze"),
                new Route(POST, "/_analyze"),
                new Route(GET, "/{index}/_analyze"),
                new Route(POST, "/{index}/_analyze")
            )
        );
    }

    @Override
    public String getName() {
        return "analyze_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        try (XContentParser parser = request.contentOrSourceParamParser()) {
            AnalyzeAction.Request analyzeRequest = AnalyzeAction.Request.fromXContent(parser, request.param("index"));
            return channel -> client.admin().indices().analyze(analyzeRequest, new RestToXContentListener<>(channel));
        }
    }

}
