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

package com.colasoft.opensearch.rest.action.ingest;

import com.colasoft.opensearch.action.ingest.GetPipelineRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * Transport action to get an ingest pipeline
 *
 * @opensearch.api
 */
public class RestGetPipelineAction extends BaseRestHandler {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestGetPipelineAction.class);

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_ingest/pipeline"), new Route(GET, "/_ingest/pipeline/{id}")));
    }

    @Override
    public String getName() {
        return "ingest_get_pipeline_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetPipelineRequest request = new GetPipelineRequest(Strings.splitStringByCommaToArray(restRequest.param("id")));
        request.clusterManagerNodeTimeout(restRequest.paramAsTime("cluster_manager_timeout", request.clusterManagerNodeTimeout()));
        parseDeprecatedMasterTimeoutParameter(request, restRequest);
        return channel -> client.admin().cluster().getPipeline(request, new RestStatusToXContentListener<>(channel));
    }
}
