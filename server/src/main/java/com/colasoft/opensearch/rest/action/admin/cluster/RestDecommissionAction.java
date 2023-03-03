/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.rest.action.admin.cluster;

import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionRequest;
import com.colasoft.opensearch.client.Requests;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttribute;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.RestToXContentListener;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.rest.RestRequest.Method.PUT;

/**
 * Registers decommission action
 *
 * @opensearch.api
 */
public class RestDecommissionAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return singletonList(new Route(PUT, "/_cluster/decommission/awareness/{awareness_attribute_name}/{awareness_attribute_value}"));
    }

    @Override
    public String getName() {
        return "decommission_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        DecommissionRequest decommissionRequest = createRequest(request);
        return channel -> client.admin().cluster().decommission(decommissionRequest, new RestToXContentListener<>(channel));
    }

    DecommissionRequest createRequest(RestRequest request) throws IOException {
        DecommissionRequest decommissionRequest = Requests.decommissionRequest();
        String attributeName = request.param("awareness_attribute_name");
        String attributeValue = request.param("awareness_attribute_value");
        // Check if we have no delay set.
        boolean noDelay = request.paramAsBoolean("no_delay", false);
        decommissionRequest.setNoDelay(noDelay);

        if (request.hasParam("delay_timeout")) {
            TimeValue delayTimeout = request.paramAsTime("delay_timeout", DecommissionRequest.DEFAULT_NODE_DRAINING_TIMEOUT);
            decommissionRequest.setDelayTimeout(delayTimeout);
        }
        return decommissionRequest.setDecommissionAttribute(new DecommissionAttribute(attributeName, attributeValue));
    }
}
