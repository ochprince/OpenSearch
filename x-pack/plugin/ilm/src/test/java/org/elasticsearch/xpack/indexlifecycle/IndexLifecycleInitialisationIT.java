/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.indexlifecycle;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.Index;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.LocalStateCompositeXPackPlugin;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.indexlifecycle.ClusterStateWaitStep;
import org.elasticsearch.xpack.core.indexlifecycle.LifecycleAction;
import org.elasticsearch.xpack.core.indexlifecycle.LifecyclePolicy;
import org.elasticsearch.xpack.core.indexlifecycle.LifecycleSettings;
import org.elasticsearch.xpack.core.indexlifecycle.LifecycleType;
import org.elasticsearch.xpack.core.indexlifecycle.MockAction;
import org.elasticsearch.xpack.core.indexlifecycle.Phase;
import org.elasticsearch.xpack.core.indexlifecycle.Step;
import org.elasticsearch.xpack.core.indexlifecycle.TerminalPolicyStep;
import org.elasticsearch.xpack.core.indexlifecycle.action.PutLifecycleAction;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.client.Requests.clusterHealthRequest;
import static org.elasticsearch.client.Requests.createIndexRequest;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.cluster.routing.ShardRoutingState.STARTED;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.xpack.core.indexlifecycle.LifecyclePolicyTestsUtils.newLockableLifecyclePolicy;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

@ESIntegTestCase.ClusterScope(scope = Scope.TEST, numDataNodes = 0)
public class IndexLifecycleInitialisationIT extends ESIntegTestCase {
    private Settings settings;
    private LifecyclePolicy lifecyclePolicy;

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder settings = Settings.builder().put(super.nodeSettings(nodeOrdinal));
        settings.put(XPackSettings.INDEX_LIFECYCLE_ENABLED.getKey(), true);
        settings.put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), false);
        settings.put(XPackSettings.SECURITY_ENABLED.getKey(), false);
        settings.put(XPackSettings.WATCHER_ENABLED.getKey(), false);
        settings.put(XPackSettings.MONITORING_ENABLED.getKey(), false);
        settings.put(XPackSettings.GRAPH_ENABLED.getKey(), false);
        settings.put(XPackSettings.LOGSTASH_ENABLED.getKey(), false);
        settings.put(LifecycleSettings.LIFECYCLE_POLL_INTERVAL, "1s");
        return settings.build();
    }

    @Override
    protected boolean ignoreExternalCluster() {
        return true;
    }

    @Override
    protected Settings transportClientSettings() {
        Settings.Builder settings = Settings.builder().put(super.transportClientSettings());
        settings.put(XPackSettings.INDEX_LIFECYCLE_ENABLED.getKey(), true);
        settings.put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), false);
        settings.put(XPackSettings.SECURITY_ENABLED.getKey(), false);
        settings.put(XPackSettings.WATCHER_ENABLED.getKey(), false);
        settings.put(XPackSettings.MONITORING_ENABLED.getKey(), false);
        settings.put(XPackSettings.GRAPH_ENABLED.getKey(), false);
        settings.put(XPackSettings.LOGSTASH_ENABLED.getKey(), false);
        return settings.build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(LocalStateCompositeXPackPlugin.class, IndexLifecycle.class, TestILMPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    @Before
    public void init() {
        settings = Settings.builder().put(indexSettings()).put(SETTING_NUMBER_OF_SHARDS, 1)
            .put(SETTING_NUMBER_OF_REPLICAS, 0).put(LifecycleSettings.LIFECYCLE_NAME, "test").build();
        List<Step> steps = new ArrayList<>();
        Step.StepKey key = new Step.StepKey("mock", ObservableAction.NAME, ObservableClusterStateWaitStep.NAME);
        steps.add(new ObservableClusterStateWaitStep(key, TerminalPolicyStep.KEY));
        Map<String, LifecycleAction> actions = Collections.singletonMap(ObservableAction.NAME, new ObservableAction(steps, true));
        Map<String, Phase> phases = Collections.singletonMap("mock", new Phase("mock", TimeValue.timeValueSeconds(0), actions));
        lifecyclePolicy = newLockableLifecyclePolicy("test", phases);
    }

    public void testSingleNodeCluster() throws Exception {
        settings = Settings.builder().put(settings).put("index.lifecycle.test.complete", true).build();
        // start master node
        logger.info("Starting server1");
        final String server_1 = internalCluster().startNode();
        final String node1 = getLocalNodeId(server_1);
        logger.info("Creating lifecycle [test_lifecycle]");
        PutLifecycleAction.Request putLifecycleRequest = new PutLifecycleAction.Request(lifecyclePolicy);
        PutLifecycleAction.Response putLifecycleResponse = client().execute(PutLifecycleAction.INSTANCE, putLifecycleRequest).get();
        assertAcked(putLifecycleResponse);
        logger.info("Creating index [test]");
        CreateIndexResponse createIndexResponse = client().admin().indices().create(createIndexRequest("test").settings(settings))
                .actionGet();
        assertAcked(createIndexResponse);
        ClusterState clusterState = client().admin().cluster().prepareState().get().getState();
        RoutingNode routingNodeEntry1 = clusterState.getRoutingNodes().node(node1);
        assertThat(routingNodeEntry1.numberOfShardsWithState(STARTED), equalTo(1));
        assertBusy(() -> {
            assertEquals(true, client().admin().indices().prepareExists("test").get().isExists());
        });
        assertBusy(() -> {
            GetSettingsResponse settingsResponse = client().admin().indices().prepareGetSettings("test").get();
            String step = settingsResponse.getSetting("test", "index.lifecycle.step");
            assertThat(step, equalTo(TerminalPolicyStep.KEY.getName()));
        });
    }

    public void testMasterDedicatedDataDedicated() throws Exception {
        settings = Settings.builder().put(settings).put("index.lifecycle.test.complete", true).build();
        // start master node
        logger.info("Starting sever1");
        internalCluster().startMasterOnlyNode();
        // start data node
        logger.info("Starting sever1");
        final String server_2 = internalCluster().startDataOnlyNode();
        final String node2 = getLocalNodeId(server_2);

        logger.info("Creating lifecycle [test_lifecycle]");
        PutLifecycleAction.Request putLifecycleRequest = new PutLifecycleAction.Request(lifecyclePolicy);
        PutLifecycleAction.Response putLifecycleResponse = client().execute(PutLifecycleAction.INSTANCE, putLifecycleRequest).get();
        assertAcked(putLifecycleResponse);
        logger.info("Creating index [test]");
        CreateIndexResponse createIndexResponse = client().admin().indices().create(createIndexRequest("test").settings(settings))
                .actionGet();
        assertAcked(createIndexResponse);

        ClusterState clusterState = client().admin().cluster().prepareState().get().getState();
        RoutingNode routingNodeEntry1 = clusterState.getRoutingNodes().node(node2);
        assertThat(routingNodeEntry1.numberOfShardsWithState(STARTED), equalTo(1));

        assertBusy(() -> {
            assertEquals(true, client().admin().indices().prepareExists("test").get().isExists());
        });
        assertBusy(() -> {
            GetSettingsResponse settingsResponse = client().admin().indices().prepareGetSettings("test").get();
            String step = settingsResponse.getSetting("test", "index.lifecycle.step");
            assertThat(step, equalTo(TerminalPolicyStep.KEY.getName()));
        });
    }

    public void testMasterFailover() throws Exception {
        // start one server
        logger.info("Starting sever1");
        final String server_1 = internalCluster().startNode();
        final String node1 = getLocalNodeId(server_1);

        logger.info("Creating lifecycle [test_lifecycle]");
        PutLifecycleAction.Request putLifecycleRequest = new PutLifecycleAction.Request(lifecyclePolicy);
        PutLifecycleAction.Response putLifecycleResponse = client().execute(PutLifecycleAction.INSTANCE, putLifecycleRequest).get();
        assertAcked(putLifecycleResponse);

        logger.info("Creating index [test]");
        CreateIndexResponse createIndexResponse = client().admin().indices().create(createIndexRequest("test").settings(settings))
                .actionGet();
        assertAcked(createIndexResponse);

        ClusterState clusterState = client().admin().cluster().prepareState().get().getState();
        RoutingNode routingNodeEntry1 = clusterState.getRoutingNodes().node(node1);
        assertThat(routingNodeEntry1.numberOfShardsWithState(STARTED), equalTo(1));

        logger.info("Starting server2");
        // start another server
        internalCluster().startNode();

        // first wait for 2 nodes in the cluster
        logger.info("Waiting for replicas to be assigned");
        ClusterHealthResponse clusterHealth = client().admin().cluster()
                .health(clusterHealthRequest().waitForGreenStatus().waitForNodes("2")).actionGet();
        logger.info("Done Cluster Health, status {}", clusterHealth.getStatus());
        assertThat(clusterHealth.isTimedOut(), equalTo(false));
        assertThat(clusterHealth.getStatus(), equalTo(ClusterHealthStatus.GREEN));

        // check step in progress in lifecycle
        assertBusy(() -> {
            GetSettingsResponse settingsResponse = client().admin().indices().prepareGetSettings("test").get();
            String step = settingsResponse.getSetting("test", "index.lifecycle.step");
            assertThat(step, equalTo(ObservableClusterStateWaitStep.NAME));
        });


        logger.info("Closing server1");
        // kill the first server
        internalCluster().stopCurrentMasterNode();

        // check that index lifecycle picked back up where it
        assertBusy(() -> {
            GetSettingsResponse settingsResponse = client().admin().indices().prepareGetSettings("test").get();
            String step = settingsResponse.getSetting("test", "index.lifecycle.step");
            assertThat(step, equalTo(ObservableClusterStateWaitStep.NAME));
        });

        // complete the step
        client().admin().indices().prepareUpdateSettings("test")
            .setSettings(Collections.singletonMap("index.lifecycle.test.complete", true)).get();

        assertBusy(() -> {
            GetSettingsResponse settingsResponse = client().admin().indices().prepareGetSettings("test").get();
            String step = settingsResponse.getSetting("test", "index.lifecycle.step");
            assertThat(step, equalTo(TerminalPolicyStep.KEY.getName()));
        });
    }

    private String getLocalNodeId(String name) {
        TransportService transportService = internalCluster().getInstance(TransportService.class, name);
        String nodeId = transportService.getLocalNode().getId();
        assertThat(nodeId, not(nullValue()));
        return nodeId;
    }

    public static class TestILMPlugin extends Plugin {
        public TestILMPlugin() {
        }

        public List<Setting<?>> getSettings() {
            final Setting<Boolean> COMPLETE_SETTING = Setting.boolSetting("index.lifecycle.test.complete", false,
                Setting.Property.Dynamic, Setting.Property.IndexScope);
            return Collections.singletonList(COMPLETE_SETTING);
        }
        public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
            return Arrays.asList(new NamedWriteableRegistry.Entry(LifecycleType.class, LockableLifecycleType.TYPE,
                    (in) -> LockableLifecycleType.INSTANCE),
                new NamedWriteableRegistry.Entry(LifecycleAction.class, ObservableAction.NAME, ObservableAction::readObservableAction),
                new NamedWriteableRegistry.Entry(ObservableClusterStateWaitStep.class, ObservableClusterStateWaitStep.NAME,
                    ObservableClusterStateWaitStep::new));
        }
    }

    public static class ObservableClusterStateWaitStep extends ClusterStateWaitStep implements NamedWriteable {
        public static final String NAME = "observable_cluster_state_action";

        public ObservableClusterStateWaitStep(StepKey current, StepKey next) {
            super(current, next);
        }

        public ObservableClusterStateWaitStep(StreamInput in) throws IOException {
            this(new StepKey(in.readString(), in.readString(), in.readString()), readOptionalNextStepKey(in));
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeString(getKey().getPhase());
            out.writeString(getKey().getAction());
            out.writeString(getKey().getName());
            boolean hasNextStep = getNextStepKey() != null;
            out.writeBoolean(hasNextStep);
            if (hasNextStep) {
                out.writeString(getNextStepKey().getPhase());
                out.writeString(getNextStepKey().getAction());
                out.writeString(getNextStepKey().getName());
            }
        }

        private static StepKey readOptionalNextStepKey(StreamInput in) throws IOException {
            if (in.readBoolean()) {
                return new StepKey(in.readString(), in.readString(), in.readString());
            }
            return null;
        }

        @Override
        public String getWriteableName() {
            return NAME;
        }

        @Override
        public Result isConditionMet(Index index, ClusterState clusterState) {
            boolean complete = clusterState.metaData().index("test").getSettings()
                .getAsBoolean("index.lifecycle.test.complete", false);
            return new Result(complete, null);
        }
    }

    public static class ObservableAction extends MockAction {

        ObservableAction(List<Step> steps, boolean safe) {
            super(steps, safe);
        }

        public static ObservableAction readObservableAction(StreamInput in) throws IOException {
            List<Step> steps = in.readList(ObservableClusterStateWaitStep::new);
            boolean safe = in.readBoolean();
            return new ObservableAction(steps, safe);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeList(getSteps().stream().map(s -> (ObservableClusterStateWaitStep) s).collect(Collectors.toList()));
            out.writeBoolean(isSafeAction());
        }
    }
}
