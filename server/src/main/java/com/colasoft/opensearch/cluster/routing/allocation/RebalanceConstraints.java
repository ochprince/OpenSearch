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

package com.colasoft.opensearch.cluster.routing.allocation;

import com.colasoft.opensearch.cluster.routing.allocation.allocator.BalancedShardsAllocator;
import com.colasoft.opensearch.cluster.routing.allocation.allocator.ShardsBalancer;

import java.util.HashMap;
import java.util.Map;

import static com.colasoft.opensearch.cluster.routing.allocation.ConstraintTypes.INDEX_PRIMARY_SHARD_BALANCE_CONSTRAINT_ID;
import static com.colasoft.opensearch.cluster.routing.allocation.ConstraintTypes.isPerIndexPrimaryShardsPerNodeBreached;

/**
 * Constraints applied during rebalancing round; specify conditions which, if breached, reduce the
 * priority of a node for receiving shard relocations.
 *
 * @opensearch.internal
 */
public class RebalanceConstraints {

    private Map<String, Constraint> constraints;

    public RebalanceConstraints() {
        this.constraints = new HashMap<>();
        this.constraints.putIfAbsent(INDEX_PRIMARY_SHARD_BALANCE_CONSTRAINT_ID, new Constraint(isPerIndexPrimaryShardsPerNodeBreached()));
    }

    public void updateRebalanceConstraint(String constraint, boolean enable) {
        this.constraints.get(constraint).setEnable(enable);
    }

    public long weight(ShardsBalancer balancer, BalancedShardsAllocator.ModelNode node, String index) {
        Constraint.ConstraintParams params = new Constraint.ConstraintParams(balancer, node, index);
        return params.weight(constraints);
    }
}
