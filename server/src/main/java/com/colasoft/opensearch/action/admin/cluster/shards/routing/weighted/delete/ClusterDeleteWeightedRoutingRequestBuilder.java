/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete;

import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Request builder to delete weights for weighted round-robin shard routing policy.
 *
 * @opensearch.internal
 */
public class ClusterDeleteWeightedRoutingRequestBuilder extends ClusterManagerNodeOperationRequestBuilder<
    ClusterDeleteWeightedRoutingRequest,
    ClusterDeleteWeightedRoutingResponse,
    ClusterDeleteWeightedRoutingRequestBuilder> {

    public ClusterDeleteWeightedRoutingRequestBuilder(OpenSearchClient client, ClusterDeleteWeightedRoutingAction action) {
        super(client, action, new ClusterDeleteWeightedRoutingRequest());
    }

    public ClusterDeleteWeightedRoutingRequestBuilder setVersion(long version) {
        request.setVersion(version);
        return this;
    }

    public ClusterDeleteWeightedRoutingRequestBuilder setAwarenessAttribute(String attribute) {
        request.setAwarenessAttribute(attribute);
        return this;
    }

}
