/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin;

import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoResponse;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoAction;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodeInfo;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodeStats;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsAction;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsResponse;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryAction;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryResponse;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsAction;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsResponse;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.test.transport.StubbableTransport;
import com.colasoft.opensearch.transport.ReceiveTimeoutTransportException;
import com.colasoft.opensearch.transport.TransportService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.containsString;

@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.TEST, numDataNodes = 0)
public class ClientTimeoutIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(MockTransportService.TestPlugin.class);
    }

    public void testNodesInfoTimeout() {
        String clusterManagerNode = internalCluster().startClusterManagerOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        String anotherDataNode = internalCluster().startDataOnlyNode();

        // Happy case
        NodesInfoResponse response = dataNodeClient().admin().cluster().prepareNodesInfo().get();
        assertThat(response.getNodes().size(), equalTo(3));

        // simulate timeout on bad node.
        simulateTimeoutAtTransport(dataNode, anotherDataNode, NodesInfoAction.NAME);

        // One bad data node
        response = dataNodeClient().admin().cluster().prepareNodesInfo().get();
        ArrayList<String> nodes = new ArrayList<String>();
        for (NodeInfo node : response.getNodes()) {
            nodes.add(node.getNode().getName());
        }
        assertThat(response.getNodes().size(), equalTo(2));
        assertThat(nodes.contains(clusterManagerNode), is(true));
    }

    public void testNodesStatsTimeout() {
        String clusterManagerNode = internalCluster().startClusterManagerOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        String anotherDataNode = internalCluster().startDataOnlyNode();
        TimeValue timeout = TimeValue.timeValueMillis(1000);

        // Happy case
        NodesStatsResponse response1 = dataNodeClient().admin().cluster().prepareNodesStats().get();
        assertThat(response1.getNodes().size(), equalTo(3));

        // One bad data node
        simulateTimeoutAtTransport(dataNode, anotherDataNode, NodesStatsAction.NAME);

        NodesStatsResponse response = dataNodeClient().admin().cluster().prepareNodesStats().get();
        ArrayList<String> nodes = new ArrayList<String>();
        for (NodeStats node : response.getNodes()) {
            nodes.add(node.getNode().getName());
        }
        assertThat(response.getNodes().size(), equalTo(2));
        assertThat(nodes.contains(clusterManagerNode), is(true));
    }

    public void testListTasksTimeout() {
        String clusterManagerNode = internalCluster().startClusterManagerOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        String anotherDataNode = internalCluster().startDataOnlyNode();
        TimeValue timeout = TimeValue.timeValueMillis(1000);

        // Happy case
        ListTasksResponse response1 = dataNodeClient().admin().cluster().prepareListTasks().get();
        assertThat(response1.getPerNodeTasks().keySet().size(), equalTo(3));

        // One bad data node
        simulateTimeoutAtTransport(dataNode, anotherDataNode, NodesStatsAction.NAME);

        ListTasksResponse response = dataNodeClient().admin().cluster().prepareListTasks().get();
        assertNull(response.getPerNodeTasks().get(anotherDataNode));
    }

    public void testRecoveriesWithTimeout() {
        internalCluster().startClusterManagerOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        String anotherDataNode = internalCluster().startDataOnlyNode();

        int numShards = 4;
        assertAcked(
            prepareCreate(
                "test-index",
                0,
                Settings.builder()
                    .put("number_of_shards", numShards)
                    .put("routing.allocation.total_shards_per_node", 2)
                    .put("number_of_replicas", 0)
            )
        );
        ensureGreen();
        final long numDocs = scaledRandomIntBetween(50, 100);
        for (int i = 0; i < numDocs; i++) {
            index("test-index", "doc", Integer.toString(i));
        }
        refresh("test-index");
        ensureSearchable("test-index");

        // Happy case
        RecoveryResponse recoveryResponse = dataNodeClient().admin().indices().prepareRecoveries().get();
        assertThat(recoveryResponse.getTotalShards(), equalTo(numShards));
        assertThat(recoveryResponse.getSuccessfulShards(), equalTo(numShards));

        // simulate timeout on bad node.
        simulateTimeoutAtTransport(dataNode, anotherDataNode, RecoveryAction.NAME);

        // verify response with bad node.
        recoveryResponse = dataNodeClient().admin().indices().prepareRecoveries().get();
        assertThat(recoveryResponse.getTotalShards(), equalTo(numShards));
        assertThat(recoveryResponse.getSuccessfulShards(), equalTo(numShards / 2));
        assertThat(recoveryResponse.getFailedShards(), equalTo(numShards / 2));
        assertThat(recoveryResponse.getShardFailures()[0].reason(), containsString("ReceiveTimeoutTransportException"));
    }

    public void testStatsWithTimeout() {
        internalCluster().startClusterManagerOnlyNode();
        String dataNode = internalCluster().startDataOnlyNode();
        String anotherDataNode = internalCluster().startDataOnlyNode();

        int numShards = 4;
        logger.info("-->  creating index");
        assertAcked(
            prepareCreate(
                "test-index",
                0,
                Settings.builder()
                    .put("number_of_shards", numShards)
                    .put("routing.allocation.total_shards_per_node", 2)
                    .put("number_of_replicas", 0)
            )
        );
        ensureGreen();
        final long numDocs = scaledRandomIntBetween(50, 100);
        for (int i = 0; i < numDocs; i++) {
            index("test-index", "doc", Integer.toString(i));
        }
        refresh("test-index");
        ensureSearchable("test-index");

        // happy case
        IndicesStatsResponse indicesStats = dataNodeClient().admin().indices().prepareStats().setDocs(true).get();
        assertThat(indicesStats.getTotalShards(), equalTo(numShards));
        assertThat(indicesStats.getSuccessfulShards(), equalTo(numShards));

        // simulate timeout on bad node.
        simulateTimeoutAtTransport(dataNode, anotherDataNode, IndicesStatsAction.NAME);

        // verify indices state response with bad node.
        indicesStats = dataNodeClient().admin().indices().prepareStats().setDocs(true).get();
        assertThat(indicesStats.getTotalShards(), equalTo(numShards));
        assertThat(indicesStats.getFailedShards(), equalTo(numShards / 2));
        assertThat(indicesStats.getSuccessfulShards(), equalTo(numShards / 2));
        assertThat(indicesStats.getTotal().getDocs().getCount(), lessThan(numDocs));
        assertThat(indicesStats.getShardFailures()[0].reason(), containsString("ReceiveTimeoutTransportException"));
    }

    private void simulateTimeoutAtTransport(String dataNode, String anotherDataNode, String transportActionName) {
        MockTransportService mockTransportService = ((MockTransportService) internalCluster().getInstance(
            TransportService.class,
            dataNode
        ));
        StubbableTransport.SendRequestBehavior sendBehaviour = (connection, requestId, action, request, options) -> {
            if (action.startsWith(transportActionName)) {
                throw new ReceiveTimeoutTransportException(connection.getNode(), action, "simulate timeout");
            }
            connection.sendRequest(requestId, action, request, options);
        };
        mockTransportService.addSendBehavior(internalCluster().getInstance(TransportService.class, anotherDataNode), sendBehaviour);
        MockTransportService mockTransportServiceAnotherNode = ((MockTransportService) internalCluster().getInstance(
            TransportService.class,
            anotherDataNode
        ));
        mockTransportServiceAnotherNode.addSendBehavior(internalCluster().getInstance(TransportService.class, dataNode), sendBehaviour);

    }
}
