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

package com.colasoft.opensearch.action;

import org.junit.After;
import com.colasoft.opensearch.OpenSearchParseException;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterGetSettingsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterHealthAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterRerouteAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterStateAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterUpdateSettingsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.dangling.RestDeleteDanglingIndexAction;
import com.colasoft.opensearch.rest.action.admin.cluster.dangling.RestImportDanglingIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestAddIndexBlockAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestCloseIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestCreateIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetIndicesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetMappingAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetSettingsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndexDeleteAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndexPutAliasAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndicesAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestOpenIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutMappingAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestResizeHandler;
import com.colasoft.opensearch.rest.action.admin.indices.RestRolloverIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestUpdateSettingsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestSimulateIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestSimulateTemplateAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCleanupRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCloneSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCreateSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetRepositoriesAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetSnapshotsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestPutRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestRestoreSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestSnapshotsStatusAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestVerifyRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteStoredScriptAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetStoredScriptAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestPutStoredScriptAction;
import com.colasoft.opensearch.rest.action.cat.RestAllocationAction;
import com.colasoft.opensearch.rest.action.cat.RestMasterAction;
import com.colasoft.opensearch.rest.action.cat.RestRepositoriesAction;
import com.colasoft.opensearch.rest.action.cat.RestThreadPoolAction;
import com.colasoft.opensearch.rest.action.cat.RestClusterManagerAction;
import com.colasoft.opensearch.rest.action.cat.RestShardsAction;
import com.colasoft.opensearch.rest.action.cat.RestPluginsAction;
import com.colasoft.opensearch.rest.action.cat.RestNodeAttrsAction;
import com.colasoft.opensearch.rest.action.cat.RestNodesAction;
import com.colasoft.opensearch.rest.action.cat.RestIndicesAction;
import com.colasoft.opensearch.rest.action.cat.RestTemplatesAction;
import com.colasoft.opensearch.rest.action.cat.RestPendingClusterTasksAction;
import com.colasoft.opensearch.rest.action.cat.RestSegmentsAction;
import com.colasoft.opensearch.rest.action.cat.RestSnapshotAction;
import com.colasoft.opensearch.rest.action.ingest.RestDeletePipelineAction;
import com.colasoft.opensearch.rest.action.ingest.RestGetPipelineAction;
import com.colasoft.opensearch.rest.action.ingest.RestPutPipelineAction;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.rest.FakeRestRequest;
import com.colasoft.opensearch.threadpool.TestThreadPool;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static com.colasoft.opensearch.cluster.metadata.IndexMetadata.INDEX_READ_ONLY_SETTING;

/**
 * As of 2.0, the request parameter 'master_timeout' in all applicable REST APIs is deprecated,
 * and alternative parameter 'cluster_manager_timeout' is added.
 * The tests are used to validate the behavior about the renamed request parameter.
 * Remove the test after removing MASTER_ROLE and 'master_timeout'.
 */
public class RenamedTimeoutRequestParameterTests extends OpenSearchTestCase {
    private final TestThreadPool threadPool = new TestThreadPool(RenamedTimeoutRequestParameterTests.class.getName());
    private final NodeClient client = new NodeClient(Settings.EMPTY, threadPool);

    private static final String DUPLICATE_PARAMETER_ERROR_MESSAGE =
        "Please only use one of the request parameters [master_timeout, cluster_manager_timeout].";

    @After
    public void terminateThreadPool() {
        terminate(threadPool);
    }

    public void testNoWarningsForNewParam() {
        BaseRestHandler.parseDeprecatedMasterTimeoutParameter(getMasterNodeRequest(), getRestRequestWithNewParam());
    }

    public void testBothParamsNotValid() {
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> BaseRestHandler.parseDeprecatedMasterTimeoutParameter(getMasterNodeRequest(), getRestRequestWithBothParams())
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatAllocation() {
        RestAllocationAction action = new RestAllocationAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatIndices() {
        RestIndicesAction action = new RestIndicesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatClusterManager() {
        RestClusterManagerAction action = new RestClusterManagerAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatMaster() {
        RestMasterAction action = new RestMasterAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatNodeattrs() {
        RestNodeAttrsAction action = new RestNodeAttrsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatNodes() {
        RestNodesAction action = new RestNodesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatPendingTasks() {
        RestPendingClusterTasksAction action = new RestPendingClusterTasksAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatPlugins() {
        RestPluginsAction action = new RestPluginsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatRepositories() {
        RestRepositoriesAction action = new RestRepositoriesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatShards() {
        RestShardsAction action = new RestShardsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatSnapshots() {
        RestSnapshotAction action = new RestSnapshotAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatTemplates() {
        RestTemplatesAction action = new RestTemplatesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatThreadPool() {
        RestThreadPoolAction action = new RestThreadPoolAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCatSegments() {
        RestSegmentsAction action = new RestSegmentsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.doCatRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterHealth() {
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> RestClusterHealthAction.fromRequest(getRestRequestWithBodyWithBothParams())
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterReroute() throws IOException {
        final SettingsFilter filter = new SettingsFilter(Collections.singleton("foo.filtered"));
        RestClusterRerouteAction action = new RestClusterRerouteAction(filter);
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterState() throws IOException {
        final SettingsFilter filter = new SettingsFilter(Collections.singleton("foo.filtered"));
        RestClusterStateAction action = new RestClusterStateAction(filter);
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterGetSettings() throws IOException {
        final SettingsFilter filter = new SettingsFilter(Collections.singleton("foo.filtered"));
        RestClusterGetSettingsAction action = new RestClusterGetSettingsAction(null, null, filter);
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterUpdateSettings() throws IOException {
        RestClusterUpdateSettingsAction action = new RestClusterUpdateSettingsAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testClusterPendingTasks() {
        RestPendingClusterTasksAction action = new RestPendingClusterTasksAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testAddIndexBlock() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        request.params().put("block", "metadata");
        NodeClient client = new NodeClient(Settings.builder().put(INDEX_READ_ONLY_SETTING.getKey(), Boolean.FALSE).build(), threadPool);
        RestAddIndexBlockAction action = new RestAddIndexBlockAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCloseIndex() {
        RestCloseIndexAction action = new RestCloseIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCreateIndex() {
        RestCreateIndexAction action = new RestCreateIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteIndex() {
        RestDeleteIndexAction action = new RestDeleteIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetIndices() {
        RestGetIndicesAction action = new RestGetIndicesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetMapping() {
        RestGetMappingAction action = new RestGetMappingAction(threadPool);
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetSettings() {
        RestGetSettingsAction action = new RestGetSettingsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testIndexDeleteAliases() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        request.params().put("name", "*");
        request.params().put("index", "test");
        RestIndexDeleteAliasesAction action = new RestIndexDeleteAliasesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testIndexPutAlias() {
        RestIndexPutAliasAction action = new RestIndexPutAliasAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testIndicesAliases() {
        RestIndicesAliasesAction action = new RestIndicesAliasesAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testOpenIndex() {
        RestOpenIndexAction action = new RestOpenIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutMapping() {
        RestPutMappingAction action = new RestPutMappingAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testShrinkIndex() {
        RestResizeHandler.RestShrinkIndexAction action = new RestResizeHandler.RestShrinkIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testSplitIndex() {
        RestResizeHandler.RestSplitIndexAction action = new RestResizeHandler.RestSplitIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCloneIndex() {
        RestResizeHandler.RestCloneIndexAction action = new RestResizeHandler.RestCloneIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testRolloverIndex() {
        RestRolloverIndexAction action = new RestRolloverIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testUpdateSettings() {
        RestUpdateSettingsAction action = new RestUpdateSettingsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteDanglingIndex() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        request.params().put("index_uuid", "test");
        RestDeleteDanglingIndexAction action = new RestDeleteDanglingIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testImportDanglingIndex() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        request.params().put("index_uuid", "test");
        RestImportDanglingIndexAction action = new RestImportDanglingIndexAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteComponentTemplate() {
        RestDeleteComponentTemplateAction action = new RestDeleteComponentTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteComposableIndexTemplate() {
        RestDeleteComposableIndexTemplateAction action = new RestDeleteComposableIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteIndexTemplate() {
        RestDeleteIndexTemplateAction action = new RestDeleteIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetComponentTemplate() {
        RestGetComponentTemplateAction action = new RestGetComponentTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetComposableIndexTemplate() {
        RestGetComposableIndexTemplateAction action = new RestGetComposableIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetIndexTemplate() {
        RestGetIndexTemplateAction action = new RestGetIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutComponentTemplate() {
        RestPutComponentTemplateAction action = new RestPutComponentTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutComposableIndexTemplate() {
        RestPutComposableIndexTemplateAction action = new RestPutComposableIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutIndexTemplate() {
        RestPutIndexTemplateAction action = new RestPutIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testSimulateIndexTemplate() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", randomFrom("1h", "2m"));
        request.params().put("master_timeout", "3s");
        request.params().put("name", "test");
        RestSimulateIndexTemplateAction action = new RestSimulateIndexTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testSimulateTemplate() {
        RestSimulateTemplateAction action = new RestSimulateTemplateAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCleanupRepository() {
        RestCleanupRepositoryAction action = new RestCleanupRepositoryAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCloneSnapshot() {
        RestCloneSnapshotAction action = new RestCloneSnapshotAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testCreateSnapshot() {
        RestCreateSnapshotAction action = new RestCreateSnapshotAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteRepository() {
        RestDeleteRepositoryAction action = new RestDeleteRepositoryAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteSnapshot() {
        RestDeleteSnapshotAction action = new RestDeleteSnapshotAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetRepositories() {
        final SettingsFilter filter = new SettingsFilter(Collections.singleton("foo.filtered"));
        RestGetRepositoriesAction action = new RestGetRepositoriesAction(filter);
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetSnapshots() {
        RestGetSnapshotsAction action = new RestGetSnapshotsAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutRepository() {
        RestPutRepositoryAction action = new RestPutRepositoryAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testRestoreSnapshot() {
        RestRestoreSnapshotAction action = new RestRestoreSnapshotAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testSnapshotsStatus() {
        RestSnapshotsStatusAction action = new RestSnapshotsStatusAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testVerifyRepository() {
        RestVerifyRepositoryAction action = new RestVerifyRepositoryAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeletePipeline() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        request.params().put("id", "test");
        RestDeletePipelineAction action = new RestDeletePipelineAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetPipeline() {
        RestGetPipelineAction action = new RestGetPipelineAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutPipeline() {
        FakeRestRequest request = getFakeRestRequestWithBody();
        request.params().put("cluster_manager_timeout", "2m");
        request.params().put("master_timeout", "3s");
        request.params().put("id", "test");
        RestPutPipelineAction action = new RestPutPipelineAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(request, client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testDeleteStoredScript() {
        RestDeleteStoredScriptAction action = new RestDeleteStoredScriptAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testGetStoredScript() {
        RestGetStoredScriptAction action = new RestGetStoredScriptAction();
        Exception e = assertThrows(OpenSearchParseException.class, () -> action.prepareRequest(getRestRequestWithBothParams(), client));
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
    }

    public void testPutStoredScript() {
        RestPutStoredScriptAction action = new RestPutStoredScriptAction();
        Exception e = assertThrows(
            OpenSearchParseException.class,
            () -> action.prepareRequest(getRestRequestWithBodyWithBothParams(), client)
        );
        assertThat(e.getMessage(), containsString(DUPLICATE_PARAMETER_ERROR_MESSAGE));
        assertWarnings("empty templates should no longer be used");
    }

    private ClusterManagerNodeRequest getMasterNodeRequest() {
        return new ClusterManagerNodeRequest() {
            @Override
            public ActionRequestValidationException validate() {
                return null;
            }
        };
    }

    private FakeRestRequest getRestRequestWithBothParams() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "1h");
        request.params().put("master_timeout", "3s");
        return request;
    }

    private FakeRestRequest getRestRequestWithDeprecatedParam() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("master_timeout", "3s");
        return request;
    }

    private FakeRestRequest getRestRequestWithNewParam() {
        FakeRestRequest request = new FakeRestRequest();
        request.params().put("cluster_manager_timeout", "2m");
        return request;
    }

    private FakeRestRequest getRestRequestWithBodyWithBothParams() {
        FakeRestRequest request = getFakeRestRequestWithBody();
        request.params().put("cluster_manager_timeout", "2m");
        request.params().put("master_timeout", "3s");
        return request;
    }

    private FakeRestRequest getFakeRestRequestWithBody() {
        return new FakeRestRequest.Builder(NamedXContentRegistry.EMPTY).withContent(new BytesArray("{}"), XContentType.JSON).build();
    }
}
