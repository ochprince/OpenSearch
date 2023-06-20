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

package com.colasoft.opensearch.action.admin.cluster.configuration;

import org.junit.BeforeClass;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.coordination.CoordinationMetadata;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static com.colasoft.opensearch.action.admin.cluster.configuration.VotingConfigExclusionsHelper.addExclusionAndGetState;
import static com.colasoft.opensearch.action.admin.cluster.configuration.VotingConfigExclusionsHelper.clearExclusionsAndGetState;
import static com.colasoft.opensearch.action.admin.cluster.configuration.VotingConfigExclusionsHelper.resolveVotingConfigExclusionsAndCheckMaximum;

public class VotingConfigExclusionsHelperTests extends OpenSearchTestCase {

    private static DiscoveryNode localNode, otherNode1, otherNode2, otherDataNode;
    private static CoordinationMetadata.VotingConfigExclusion localNodeExclusion, otherNode1Exclusion, otherNode2Exclusion;
    private static ClusterState initialClusterState;

    public void testAddExclusionAndGetState() {
        ClusterState updatedState = addExclusionAndGetState(initialClusterState, Set.of(localNodeExclusion), 2);
        assertTrue(updatedState.coordinationMetadata().getVotingConfigExclusions().contains(localNodeExclusion));
        assertEquals(1, updatedState.coordinationMetadata().getVotingConfigExclusions().size());
    }

    public void testResolveVotingConfigExclusions() {
        AddVotingConfigExclusionsRequest request = new AddVotingConfigExclusionsRequest(
            Strings.EMPTY_ARRAY,
            new String[] { "other1" },
            Strings.EMPTY_ARRAY,
            TimeValue.timeValueSeconds(30)
        );
        Set<CoordinationMetadata.VotingConfigExclusion> votingConfigExclusions = resolveVotingConfigExclusionsAndCheckMaximum(
            request,
            initialClusterState,
            10
        );
        assertEquals(1, votingConfigExclusions.size());
        assertTrue(votingConfigExclusions.contains(otherNode1Exclusion));
    }

    public void testResolveVotingConfigExclusionFailsWhenLimitExceeded() {
        AddVotingConfigExclusionsRequest request = new AddVotingConfigExclusionsRequest(
            Strings.EMPTY_ARRAY,
            new String[] { "other1", "other2" },
            Strings.EMPTY_ARRAY,
            TimeValue.timeValueSeconds(30)
        );
        expectThrows(IllegalArgumentException.class, () -> resolveVotingConfigExclusionsAndCheckMaximum(request, initialClusterState, 1));
    }

    public void testClearExclusionAndGetState() {
        ClusterState updatedState = addExclusionAndGetState(initialClusterState, Set.of(localNodeExclusion), 2);
        assertTrue(updatedState.coordinationMetadata().getVotingConfigExclusions().contains(localNodeExclusion));
        updatedState = clearExclusionsAndGetState(updatedState);
        assertTrue(updatedState.coordinationMetadata().getVotingConfigExclusions().isEmpty());
    }

    @BeforeClass
    public static void createBaseClusterState() {
        localNode = makeDiscoveryNode("local");
        localNodeExclusion = new CoordinationMetadata.VotingConfigExclusion(localNode);
        otherNode1 = makeDiscoveryNode("other1");
        otherNode1Exclusion = new CoordinationMetadata.VotingConfigExclusion(otherNode1);
        otherNode2 = makeDiscoveryNode("other2");
        otherNode2Exclusion = new CoordinationMetadata.VotingConfigExclusion(otherNode2);
        otherDataNode = new DiscoveryNode("data", "data", buildNewFakeTransportAddress(), emptyMap(), emptySet(), Version.CURRENT);
        final CoordinationMetadata.VotingConfiguration allNodesConfig = CoordinationMetadata.VotingConfiguration.of(
            localNode,
            otherNode1,
            otherNode2
        );
        initialClusterState = ClusterState.builder(new ClusterName("cluster"))
            .nodes(
                new DiscoveryNodes.Builder().add(localNode)
                    .add(otherNode1)
                    .add(otherNode2)
                    .add(otherDataNode)
                    .localNodeId(localNode.getId())
                    .clusterManagerNodeId(localNode.getId())
            )
            .metadata(
                Metadata.builder()
                    .coordinationMetadata(
                        CoordinationMetadata.builder()
                            .lastAcceptedConfiguration(allNodesConfig)
                            .lastCommittedConfiguration(allNodesConfig)
                            .build()
                    )
            )
            .build();
    }

    private static DiscoveryNode makeDiscoveryNode(String name) {
        return new DiscoveryNode(
            name,
            name,
            buildNewFakeTransportAddress(),
            emptyMap(),
            singleton(DiscoveryNodeRole.CLUSTER_MANAGER_ROLE),
            Version.CURRENT
        );
    }
}
