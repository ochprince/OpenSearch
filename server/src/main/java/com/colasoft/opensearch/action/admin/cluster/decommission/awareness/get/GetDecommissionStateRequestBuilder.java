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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get;

import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeReadOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Get decommission request builder
 *
 * @opensearch.internal
 */
public class GetDecommissionStateRequestBuilder extends ClusterManagerNodeReadOperationRequestBuilder<
    GetDecommissionStateRequest,
    GetDecommissionStateResponse,
    GetDecommissionStateRequestBuilder> {

    /**
     * Creates new get decommissioned attributes request builder
     */
    public GetDecommissionStateRequestBuilder(OpenSearchClient client, GetDecommissionStateAction action) {
        super(client, action, new GetDecommissionStateRequest());
    }

    /**
     * @param attributeName name of attribute
     * @return current object
     */
    public GetDecommissionStateRequestBuilder setAttributeName(String attributeName) {
        request.attributeName(attributeName);
        return this;
    }
}
