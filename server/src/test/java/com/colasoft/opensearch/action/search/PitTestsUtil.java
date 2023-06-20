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

package com.colasoft.opensearch.action.search;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.junit.Assert;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionFuture;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateRequest;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentResponse;
import com.colasoft.opensearch.action.admin.indices.segments.PitSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.PitSegmentsRequest;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.util.concurrent.AtomicArray;
import com.colasoft.opensearch.index.query.IdsQueryBuilder;
import com.colasoft.opensearch.index.query.MatchAllQueryBuilder;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.search.SearchPhaseResult;
import com.colasoft.opensearch.search.SearchShardTarget;
import com.colasoft.opensearch.search.internal.AliasFilter;
import com.colasoft.opensearch.search.internal.ShardSearchContextId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static com.colasoft.opensearch.test.OpenSearchTestCase.between;
import static com.colasoft.opensearch.test.OpenSearchTestCase.randomAlphaOfLength;
import static com.colasoft.opensearch.test.OpenSearchTestCase.randomBoolean;

/**
 * Helper class for common pit tests functions
 */
public class PitTestsUtil {
    private PitTestsUtil() {}

    public static QueryBuilder randomQueryBuilder() {
        if (randomBoolean()) {
            return new TermQueryBuilder(randomAlphaOfLength(10), randomAlphaOfLength(10));
        } else if (randomBoolean()) {
            return new MatchAllQueryBuilder();
        } else {
            return new IdsQueryBuilder().addIds(randomAlphaOfLength(10));
        }
    }

    public static String getPitId() {
        AtomicArray<SearchPhaseResult> array = new AtomicArray<>(3);
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult1 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId("a", 1),
            null
        );
        testSearchPhaseResult1.setSearchShardTarget(new SearchShardTarget("node_1", new ShardId("idx", "uuid1", 2), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult2 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId("b", 12),
            null
        );
        testSearchPhaseResult2.setSearchShardTarget(new SearchShardTarget("node_2", new ShardId("idy", "uuid2", 42), null, null));
        SearchAsyncActionTests.TestSearchPhaseResult testSearchPhaseResult3 = new SearchAsyncActionTests.TestSearchPhaseResult(
            new ShardSearchContextId("c", 42),
            null
        );
        testSearchPhaseResult3.setSearchShardTarget(new SearchShardTarget("node_3", new ShardId("idy", "uuid2", 43), null, null));
        array.setOnce(0, testSearchPhaseResult1);
        array.setOnce(1, testSearchPhaseResult2);
        array.setOnce(2, testSearchPhaseResult3);

        final Version version = Version.CURRENT;
        final Map<String, AliasFilter> aliasFilters = new HashMap<>();
        for (SearchPhaseResult result : array.asList()) {
            final AliasFilter aliasFilter;
            if (randomBoolean()) {
                aliasFilter = new AliasFilter(randomQueryBuilder());
            } else if (randomBoolean()) {
                aliasFilter = new AliasFilter(randomQueryBuilder(), "alias-" + between(1, 10));
            } else {
                aliasFilter = AliasFilter.EMPTY;
            }
            if (randomBoolean()) {
                aliasFilters.put(result.getSearchShardTarget().getShardId().getIndex().getUUID(), aliasFilter);
            }
        }
        return SearchContextId.encode(array.asList(), aliasFilters, version);
    }

    public static void assertUsingGetAllPits(Client client, String id, long creationTime) throws ExecutionException, InterruptedException {
        final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.local(false);
        clusterStateRequest.clear().nodes(true).routingTable(true).indices("*");
        ClusterStateResponse clusterStateResponse = client.admin().cluster().state(clusterStateRequest).get();
        final List<DiscoveryNode> nodes = new LinkedList<>();
        for (ObjectCursor<DiscoveryNode> cursor : clusterStateResponse.getState().nodes().getDataNodes().values()) {
            DiscoveryNode node = cursor.value;
            nodes.add(node);
        }
        DiscoveryNode[] disNodesArr = new DiscoveryNode[nodes.size()];
        nodes.toArray(disNodesArr);
        GetAllPitNodesRequest getAllPITNodesRequest = new GetAllPitNodesRequest(disNodesArr);
        ActionFuture<GetAllPitNodesResponse> execute1 = client.execute(GetAllPitsAction.INSTANCE, getAllPITNodesRequest);
        GetAllPitNodesResponse getPitResponse = execute1.get();
        assertTrue(getPitResponse.getPitInfos().get(0).getPitId().contains(id));
        Assert.assertEquals(getPitResponse.getPitInfos().get(0).getCreationTime(), creationTime);
    }

    public static void assertGetAllPitsEmpty(Client client) throws ExecutionException, InterruptedException {
        final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.local(false);
        clusterStateRequest.clear().nodes(true).routingTable(true).indices("*");
        ClusterStateResponse clusterStateResponse = client.admin().cluster().state(clusterStateRequest).get();
        final List<DiscoveryNode> nodes = new LinkedList<>();
        for (ObjectCursor<DiscoveryNode> cursor : clusterStateResponse.getState().nodes().getDataNodes().values()) {
            DiscoveryNode node = cursor.value;
            nodes.add(node);
        }
        DiscoveryNode[] disNodesArr = new DiscoveryNode[nodes.size()];
        nodes.toArray(disNodesArr);
        GetAllPitNodesRequest getAllPITNodesRequest = new GetAllPitNodesRequest(disNodesArr);
        ActionFuture<GetAllPitNodesResponse> execute1 = client.execute(GetAllPitsAction.INSTANCE, getAllPITNodesRequest);
        GetAllPitNodesResponse getPitResponse = execute1.get();
        Assert.assertEquals(0, getPitResponse.getPitInfos().size());
    }

    public static void assertSegments(boolean isEmpty, String index, long expectedShardSize, Client client, String pitId) {
        PitSegmentsRequest pitSegmentsRequest;
        pitSegmentsRequest = new PitSegmentsRequest();
        List<String> pitIds = new ArrayList<>();
        pitIds.add(pitId);
        pitSegmentsRequest.clearAndSetPitIds(pitIds);
        IndicesSegmentResponse indicesSegmentResponse = client.execute(PitSegmentsAction.INSTANCE, pitSegmentsRequest).actionGet();
        assertTrue(indicesSegmentResponse.getShardFailures() == null || indicesSegmentResponse.getShardFailures().length == 0);
        assertEquals(indicesSegmentResponse.getIndices().isEmpty(), isEmpty);
        if (!isEmpty) {
            assertTrue(indicesSegmentResponse.getIndices().get(index) != null);
            assertTrue(indicesSegmentResponse.getIndices().get(index).getIndex().equalsIgnoreCase(index));
            assertEquals(expectedShardSize, indicesSegmentResponse.getIndices().get(index).getShards().size());
        }
    }

    public static void assertSegments(boolean isEmpty, String index, long expectedShardSize, Client client) {
        PitSegmentsRequest pitSegmentsRequest = new PitSegmentsRequest("_all");
        IndicesSegmentResponse indicesSegmentResponse = client.execute(PitSegmentsAction.INSTANCE, pitSegmentsRequest).actionGet();
        assertTrue(indicesSegmentResponse.getShardFailures() == null || indicesSegmentResponse.getShardFailures().length == 0);
        assertEquals(indicesSegmentResponse.getIndices().isEmpty(), isEmpty);
        if (!isEmpty) {
            assertTrue(indicesSegmentResponse.getIndices().get(index) != null);
            assertTrue(indicesSegmentResponse.getIndices().get(index).getIndex().equalsIgnoreCase(index));
            assertEquals(expectedShardSize, indicesSegmentResponse.getIndices().get(index).getShards().size());
        }
    }

    public static void assertSegments(boolean isEmpty, Client client) {
        assertSegments(isEmpty, "index", 2, client);
    }

    public static void assertSegments(boolean isEmpty, Client client, String pitId) {
        assertSegments(isEmpty, "index", 2, client, pitId);
    }
}
