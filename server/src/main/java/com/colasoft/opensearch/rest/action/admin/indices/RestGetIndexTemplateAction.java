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

import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;
import static com.colasoft.opensearch.rest.RestRequest.Method.HEAD;
import static com.colasoft.opensearch.rest.RestStatus.NOT_FOUND;
import static com.colasoft.opensearch.rest.RestStatus.OK;

/**
 * The REST handler for get template and head template APIs.
 *
 * @opensearch.api
 */
public class RestGetIndexTemplateAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestGetIndexTemplateAction.class);

    @Override
    public List<Route> routes() {
        return unmodifiableList(
            asList(new Route(GET, "/_template"), new Route(GET, "/_template/{name}"), new Route(HEAD, "/_template/{name}"))
        );
    }

    @Override
    public String getName() {
        return "get_index_template_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        final String[] names = Strings.splitStringByCommaToArray(request.param("name"));

        final GetIndexTemplatesRequest getIndexTemplatesRequest = new GetIndexTemplatesRequest(names);
        getIndexTemplatesRequest.local(request.paramAsBoolean("local", getIndexTemplatesRequest.local()));
        getIndexTemplatesRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", getIndexTemplatesRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(getIndexTemplatesRequest, request);

        final boolean implicitAll = getIndexTemplatesRequest.names().length == 0;

        return channel -> client.admin()
            .indices()
            .getTemplates(getIndexTemplatesRequest, new RestToXContentListener<GetIndexTemplatesResponse>(channel) {
                @Override
                protected RestStatus getStatus(final GetIndexTemplatesResponse response) {
                    final boolean templateExists = response.getIndexTemplates().isEmpty() == false;
                    return (templateExists || implicitAll) ? OK : NOT_FOUND;
                }
            });
    }

    @Override
    protected Set<String> responseParams() {
        return Settings.FORMAT_PARAMS;
    }

}
