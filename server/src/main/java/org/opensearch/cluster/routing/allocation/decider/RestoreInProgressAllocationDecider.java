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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.cluster.routing.allocation.decider;

import org.opensearch.cluster.RestoreInProgress;
import org.opensearch.cluster.routing.RecoverySource;
import org.opensearch.cluster.routing.RoutingNode;
import org.opensearch.cluster.routing.ShardRouting;
import org.opensearch.cluster.routing.allocation.RoutingAllocation;

/**
 * This {@link AllocationDecider} prevents shards that have failed to be
 * restored from a snapshot to be allocated.
 */
public class RestoreInProgressAllocationDecider extends AllocationDecider {

    public static final String NAME = "restore_in_progress";

    @Override
    public Decision canAllocate(final ShardRouting shardRouting, final RoutingNode node, final RoutingAllocation allocation) {
        return canAllocate(shardRouting, allocation);
    }

    @Override
    public Decision canAllocate(final ShardRouting shardRouting, final RoutingAllocation allocation) {
        final RecoverySource recoverySource = shardRouting.recoverySource();
        if (recoverySource == null || recoverySource.getType() != RecoverySource.Type.SNAPSHOT) {
            return allocation.decision(Decision.YES, NAME, "ignored as shard is not being recovered from a snapshot");
        }

        final RecoverySource.SnapshotRecoverySource source = (RecoverySource.SnapshotRecoverySource) recoverySource;
        if (source.restoreUUID().equals(RecoverySource.SnapshotRecoverySource.NO_API_RESTORE_UUID)) {
            return allocation.decision(Decision.YES, NAME, "not an API-level restore");
        }

        final RestoreInProgress restoresInProgress = allocation.custom(RestoreInProgress.TYPE);

        if (restoresInProgress != null) {
            RestoreInProgress.Entry restoreInProgress = restoresInProgress.get(source.restoreUUID());
            if (restoreInProgress != null) {
                RestoreInProgress.ShardRestoreStatus shardRestoreStatus = restoreInProgress.shards().get(shardRouting.shardId());
                if (shardRestoreStatus != null && shardRestoreStatus.state().completed() == false) {
                    assert shardRestoreStatus.state() != RestoreInProgress.State.SUCCESS : "expected shard [" + shardRouting
                        + "] to be in initializing state but got [" + shardRestoreStatus.state() + "]";
                    return allocation.decision(Decision.YES, NAME, "shard is currently being restored");
                }
            }
        }
        return allocation.decision(Decision.NO, NAME, "shard has failed to be restored from the snapshot [%s] because of [%s] - " +
            "manually close or delete the index [%s] in order to retry to restore the snapshot again or use the reroute API to force the " +
            "allocation of an empty primary shard",
            source.snapshot(), shardRouting.unassignedInfo().getDetails(), shardRouting.getIndexName());
    }

    @Override
    public Decision canForceAllocatePrimary(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        assert shardRouting.primary() : "must not call canForceAllocatePrimary on a non-primary shard " + shardRouting;
        return canAllocate(shardRouting, node, allocation);
    }
}
