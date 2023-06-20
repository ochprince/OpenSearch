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

package com.colasoft.opensearch.rest.action.search;

import com.colasoft.opensearch.action.search.GetSearchPipelineRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestStatusToXContentListener;

import java.io.IOException;
import java.util.List;

import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * REST action to retrieve search pipelines
 *
 *  @opensearch.internal
 */
public class RestGetSearchPipelineAction extends BaseRestHandler {
    @Override
    public String getName() {
        return "search_get_pipeline_action";
    }

    @Override
    public List<Route> routes() {
        return List.of(new Route(GET, "/_search/pipeline"), new Route(GET, "/_search/pipeline/{id}"));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        GetSearchPipelineRequest request = new GetSearchPipelineRequest(Strings.splitStringByCommaToArray(restRequest.param("id")));
        request.clusterManagerNodeTimeout(restRequest.paramAsTime("cluster_manager_timeout", request.clusterManagerNodeTimeout()));
        parseDeprecatedMasterTimeoutParameter(request, restRequest);
        return channel -> client.admin().cluster().getSearchPipeline(request, new RestStatusToXContentListener<>(channel));
    }
}
