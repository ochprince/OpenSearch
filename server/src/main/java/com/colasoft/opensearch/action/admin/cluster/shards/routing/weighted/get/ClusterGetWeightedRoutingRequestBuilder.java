/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get;

import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeReadOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Request builder to get weights for weighted round-robin search routing policy.
 *
 * @opensearch.internal
 */
public class ClusterGetWeightedRoutingRequestBuilder extends ClusterManagerNodeReadOperationRequestBuilder<
    ClusterGetWeightedRoutingRequest,
    ClusterGetWeightedRoutingResponse,
    ClusterGetWeightedRoutingRequestBuilder> {

    public ClusterGetWeightedRoutingRequestBuilder(OpenSearchClient client, ClusterGetWeightedRoutingAction action) {
        super(client, action, new ClusterGetWeightedRoutingRequest());
    }

    public ClusterGetWeightedRoutingRequestBuilder setRequestLocal(boolean local) {
        request.local(local);
        return this;
    }

    public ClusterGetWeightedRoutingRequestBuilder setAwarenessAttribute(String attribute) {
        request.setAwarenessAttribute(attribute);
        return this;
    }
}
