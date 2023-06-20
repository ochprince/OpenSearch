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

package com.colasoft.opensearch.rest.action.admin.cluster;

import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateRequest;
import com.colasoft.opensearch.client.Requests;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * Returns decommissioned attribute information
 *
 * @opensearch.api
 */
public class RestGetDecommissionStateAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_cluster/decommission/awareness/{awareness_attribute_name}/_status"));
    }

    @Override
    public String getName() {
        return "get_decommission_state_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        GetDecommissionStateRequest getDecommissionStateRequest = Requests.getDecommissionStateRequest();
        String attributeName = request.param("awareness_attribute_name");
        getDecommissionStateRequest.attributeName(attributeName);
        return channel -> client.admin().cluster().getDecommissionState(getDecommissionStateRequest, new RestToXContentListener<>(channel));
    }
}
