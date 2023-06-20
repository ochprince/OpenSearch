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

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put;

import com.colasoft.opensearch.action.ActionType;

/**
 * Action to update weights for weighted round-robin shard routing policy.
 *
 * @opensearch.internal
 */
public final class ClusterAddWeightedRoutingAction extends ActionType<ClusterPutWeightedRoutingResponse> {

    public static final ClusterAddWeightedRoutingAction INSTANCE = new ClusterAddWeightedRoutingAction();
    public static final String NAME = "cluster:admin/routing/awareness/weights/put";

    private ClusterAddWeightedRoutingAction() {
        super(NAME, ClusterPutWeightedRoutingResponse::new);
    }
}
