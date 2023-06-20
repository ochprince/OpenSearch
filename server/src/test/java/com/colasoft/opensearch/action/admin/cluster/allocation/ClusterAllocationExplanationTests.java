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

package com.colasoft.opensearch.action.admin.cluster.allocation;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardRoutingState;
import com.colasoft.opensearch.cluster.routing.TestShardRouting;
import com.colasoft.opensearch.cluster.routing.UnassignedInfo;
import com.colasoft.opensearch.cluster.routing.allocation.AllocateUnassignedDecision;
import com.colasoft.opensearch.cluster.routing.allocation.AllocationDecision;
import com.colasoft.opensearch.cluster.routing.allocation.MoveDecision;
import com.colasoft.opensearch.cluster.routing.allocation.ShardAllocationDecision;
import com.colasoft.opensearch.cluster.routing.allocation.decider.Decision;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

/**
 * Tests for the cluster allocation explanation
 */
public final class ClusterAllocationExplanationTests extends OpenSearchTestCase {

    public void testDecisionEquality() {
        Decision.Multi d = new Decision.Multi();
        Decision.Multi d2 = new Decision.Multi();
        d.add(Decision.single(Decision.Type.NO, "no label", "because I said no"));
        d.add(Decision.single(Decision.Type.YES, "yes label", "yes please"));
        d.add(Decision.single(Decision.Type.THROTTLE, "throttle label", "wait a sec"));
        d2.add(Decision.single(Decision.Type.NO, "no label", "because I said no"));
        d2.add(Decision.single(Decision.Type.YES, "yes label", "yes please"));
        d2.add(Decision.single(Decision.Type.THROTTLE, "throttle label", "wait a sec"));
        assertEquals(d, d2);
    }

    public void testExplanationSerialization() throws Exception {
        ClusterAllocationExplanation cae = randomClusterAllocationExplanation(randomBoolean());
        BytesStreamOutput out = new BytesStreamOutput();
        cae.writeTo(out);
        StreamInput in = out.bytes().streamInput();
        ClusterAllocationExplanation cae2 = new ClusterAllocationExplanation(in);
        assertEquals(cae.getShard(), cae2.getShard());
        assertEquals(cae.isPrimary(), cae2.isPrimary());
        assertTrue(cae2.isPrimary());
        assertEquals(cae.getUnassignedInfo(), cae2.getUnassignedInfo());
        assertEquals(cae.getCurrentNode(), cae2.getCurrentNode());
        assertEquals(cae.getShardState(), cae2.getShardState());
        if (cae.getClusterInfo() == null) {
            assertNull(cae2.getClusterInfo());
        } else {
            assertNotNull(cae2.getClusterInfo());
            assertEquals(
                cae.getClusterInfo().getNodeMostAvailableDiskUsages().size(),
                cae2.getClusterInfo().getNodeMostAvailableDiskUsages().size()
            );
        }
        assertEquals(cae.getShardAllocationDecision().getAllocateDecision(), cae2.getShardAllocationDecision().getAllocateDecision());
        assertEquals(cae.getShardAllocationDecision().getMoveDecision(), cae2.getShardAllocationDecision().getMoveDecision());
    }

    public void testExplanationToXContent() throws Exception {
        ClusterAllocationExplanation cae = randomClusterAllocationExplanation(true);
        XContentBuilder builder = XContentFactory.jsonBuilder();
        cae.toXContent(builder, ToXContent.EMPTY_PARAMS);
        assertEquals(
            "{\"index\":\"idx\",\"shard\":0,\"primary\":true,\"current_state\":\"started\",\"current_node\":"
                + "{\"id\":\"node-0\",\"name\":\"\",\"transport_address\":\""
                + cae.getCurrentNode().getAddress()
                + "\",\"weight_ranking\":3},\"can_remain_on_current_node\":\"yes\",\"can_rebalance_cluster\":\"yes\","
                + "\"can_rebalance_to_other_node\":\"no\",\"rebalance_explanation\":\"cannot rebalance as no target node exists "
                + "that can both allocate this shard and improve the cluster balance\"}",
            Strings.toString(builder)
        );
    }

    private static ClusterAllocationExplanation randomClusterAllocationExplanation(boolean assignedShard) {
        ShardRouting shardRouting = TestShardRouting.newShardRouting(
            new ShardId(new Index("idx", "123"), 0),
            assignedShard ? "node-0" : null,
            true,
            assignedShard ? ShardRoutingState.STARTED : ShardRoutingState.UNASSIGNED
        );
        DiscoveryNode node = assignedShard
            ? new DiscoveryNode("node-0", buildNewFakeTransportAddress(), emptyMap(), emptySet(), Version.CURRENT)
            : null;
        ShardAllocationDecision shardAllocationDecision;
        if (assignedShard) {
            MoveDecision moveDecision = MoveDecision.cannotRebalance(Decision.YES, AllocationDecision.NO, 3, null)
                .withRemainDecision(Decision.YES);
            shardAllocationDecision = new ShardAllocationDecision(AllocateUnassignedDecision.NOT_TAKEN, moveDecision);
        } else {
            AllocateUnassignedDecision allocateDecision = AllocateUnassignedDecision.no(UnassignedInfo.AllocationStatus.DECIDERS_NO, null);
            shardAllocationDecision = new ShardAllocationDecision(allocateDecision, MoveDecision.NOT_TAKEN);
        }
        return new ClusterAllocationExplanation(shardRouting, node, null, null, shardAllocationDecision);
    }
}
