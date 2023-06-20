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

package com.colasoft.opensearch.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainAction;
import com.colasoft.opensearch.action.admin.cluster.allocation.TransportClusterAllocationExplainAction;
import com.colasoft.opensearch.action.admin.cluster.configuration.AddVotingConfigExclusionsAction;
import com.colasoft.opensearch.action.admin.cluster.configuration.ClearVotingConfigExclusionsAction;
import com.colasoft.opensearch.action.admin.cluster.configuration.TransportAddVotingConfigExclusionsAction;
import com.colasoft.opensearch.action.admin.cluster.configuration.TransportClearVotingConfigExclusionsAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.TransportGetDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.TransportDeleteDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.TransportDecommissionAction;
import com.colasoft.opensearch.action.admin.cluster.health.ClusterHealthAction;
import com.colasoft.opensearch.action.admin.cluster.health.TransportClusterHealthAction;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.NodesHotThreadsAction;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.TransportNodesHotThreadsAction;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoAction;
import com.colasoft.opensearch.action.admin.cluster.node.info.TransportNodesInfoAction;
import com.colasoft.opensearch.action.admin.cluster.node.liveness.TransportLivenessAction;
import com.colasoft.opensearch.action.admin.cluster.node.reload.NodesReloadSecureSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.node.reload.TransportNodesReloadSecureSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsAction;
import com.colasoft.opensearch.action.admin.cluster.node.stats.TransportNodesStatsAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.CancelTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.TransportCancelTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.GetTaskAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.TransportGetTaskAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.TransportListTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.usage.NodesUsageAction;
import com.colasoft.opensearch.action.admin.cluster.node.usage.TransportNodesUsageAction;
import com.colasoft.opensearch.action.admin.cluster.remote.RemoteInfoAction;
import com.colasoft.opensearch.action.admin.cluster.remote.TransportRemoteInfoAction;
import com.colasoft.opensearch.action.admin.cluster.remotestore.restore.RestoreRemoteStoreAction;
import com.colasoft.opensearch.action.admin.cluster.remotestore.restore.TransportRestoreRemoteStoreAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.TransportCleanupRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.delete.DeleteRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.delete.TransportDeleteRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.GetRepositoriesAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.TransportGetRepositoriesAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.put.PutRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.put.TransportPutRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.TransportVerifyRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.VerifyRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.reroute.ClusterRerouteAction;
import com.colasoft.opensearch.action.admin.cluster.reroute.TransportClusterRerouteAction;
import com.colasoft.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.settings.TransportClusterUpdateSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.shards.ClusterSearchShardsAction;
import com.colasoft.opensearch.action.admin.cluster.shards.TransportClusterSearchShardsAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.ClusterDeleteWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.TransportDeleteWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.ClusterGetWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.TransportGetWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.ClusterAddWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.TransportAddWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.clone.CloneSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.clone.TransportCloneSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.TransportCreateSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.delete.DeleteSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.delete.TransportDeleteSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.TransportGetSnapshotsAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.TransportRestoreSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.TransportSnapshotsStatusAction;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateAction;
import com.colasoft.opensearch.action.admin.cluster.state.TransportClusterStateAction;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsAction;
import com.colasoft.opensearch.action.admin.cluster.stats.TransportClusterStatsAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.DeleteStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetScriptContextAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetScriptLanguageAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.PutStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.TransportDeleteStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.TransportGetScriptContextAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.TransportGetScriptLanguageAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.TransportGetStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.TransportPutStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.tasks.PendingClusterTasksAction;
import com.colasoft.opensearch.action.admin.cluster.tasks.TransportPendingClusterTasksAction;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesAction;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import com.colasoft.opensearch.action.admin.indices.alias.TransportIndicesAliasesAction;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesAction;
import com.colasoft.opensearch.action.admin.indices.alias.get.TransportGetAliasesAction;
import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeAction;
import com.colasoft.opensearch.action.admin.indices.analyze.TransportAnalyzeAction;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheAction;
import com.colasoft.opensearch.action.admin.indices.cache.clear.TransportClearIndicesCacheAction;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexAction;
import com.colasoft.opensearch.action.admin.indices.close.TransportCloseIndexAction;
import com.colasoft.opensearch.action.admin.indices.create.AutoCreateAction;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexAction;
import com.colasoft.opensearch.action.admin.indices.create.TransportCreateIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.delete.DeleteDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.delete.TransportDeleteDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.find.FindDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.find.TransportFindDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.import_index.ImportDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.import_index.TransportImportDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.list.ListDanglingIndicesAction;
import com.colasoft.opensearch.action.admin.indices.dangling.list.TransportListDanglingIndicesAction;
import com.colasoft.opensearch.action.admin.indices.datastream.CreateDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.DataStreamsStatsAction;
import com.colasoft.opensearch.action.admin.indices.datastream.DeleteDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.GetDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexAction;
import com.colasoft.opensearch.action.admin.indices.delete.TransportDeleteIndexAction;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsAction;
import com.colasoft.opensearch.action.admin.indices.exists.indices.TransportIndicesExistsAction;
import com.colasoft.opensearch.action.admin.indices.flush.FlushAction;
import com.colasoft.opensearch.action.admin.indices.flush.TransportFlushAction;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeAction;
import com.colasoft.opensearch.action.admin.indices.forcemerge.TransportForceMergeAction;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexAction;
import com.colasoft.opensearch.action.admin.indices.get.TransportGetIndexAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.TransportGetFieldMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.TransportGetFieldMappingsIndexAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.TransportGetMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.AutoPutMappingAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.put.TransportAutoPutMappingAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.TransportPutMappingAction;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexAction;
import com.colasoft.opensearch.action.admin.indices.open.TransportOpenIndexAction;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockAction;
import com.colasoft.opensearch.action.admin.indices.readonly.TransportAddIndexBlockAction;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryAction;
import com.colasoft.opensearch.action.admin.indices.recovery.TransportRecoveryAction;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshAction;
import com.colasoft.opensearch.action.admin.indices.refresh.TransportRefreshAction;
import com.colasoft.opensearch.action.admin.indices.resolve.ResolveIndexAction;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverAction;
import com.colasoft.opensearch.action.admin.indices.rollover.TransportRolloverAction;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsAction;
import com.colasoft.opensearch.action.admin.indices.replication.TransportSegmentReplicationStatsAction;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.PitSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.TransportIndicesSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.TransportPitSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsAction;
import com.colasoft.opensearch.action.admin.indices.settings.get.TransportGetSettingsAction;
import com.colasoft.opensearch.action.admin.indices.settings.put.TransportUpdateSettingsAction;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsAction;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresAction;
import com.colasoft.opensearch.action.admin.indices.shards.TransportIndicesShardStoresAction;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeAction;
import com.colasoft.opensearch.action.admin.indices.shrink.TransportResizeAction;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsAction;
import com.colasoft.opensearch.action.admin.indices.stats.TransportIndicesStatsAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.TransportDeleteComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.TransportDeleteComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.TransportDeleteIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.get.GetComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.get.GetComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import com.colasoft.opensearch.action.admin.indices.template.get.TransportGetComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.get.TransportGetComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.get.TransportGetIndexTemplatesAction;
import com.colasoft.opensearch.action.admin.indices.template.post.SimulateIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.post.SimulateTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.post.TransportSimulateIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.post.TransportSimulateTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.PutComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.PutComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.TransportPutComponentTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.TransportPutComposableIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.TransportPutIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.TransportUpgradeStatusAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.TransportUpgradeAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.TransportUpgradeSettingsAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeSettingsAction;
import com.colasoft.opensearch.action.admin.indices.validate.query.TransportValidateQueryAction;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryAction;
import com.colasoft.opensearch.action.bulk.BulkAction;
import com.colasoft.opensearch.action.bulk.TransportBulkAction;
import com.colasoft.opensearch.action.bulk.TransportShardBulkAction;
import com.colasoft.opensearch.action.delete.DeleteAction;
import com.colasoft.opensearch.action.delete.TransportDeleteAction;
import com.colasoft.opensearch.action.explain.ExplainAction;
import com.colasoft.opensearch.action.explain.TransportExplainAction;
import com.colasoft.opensearch.action.fieldcaps.FieldCapabilitiesAction;
import com.colasoft.opensearch.action.fieldcaps.TransportFieldCapabilitiesAction;
import com.colasoft.opensearch.action.fieldcaps.TransportFieldCapabilitiesIndexAction;
import com.colasoft.opensearch.action.get.GetAction;
import com.colasoft.opensearch.action.get.MultiGetAction;
import com.colasoft.opensearch.action.get.TransportGetAction;
import com.colasoft.opensearch.action.get.TransportMultiGetAction;
import com.colasoft.opensearch.action.get.TransportShardMultiGetAction;
import com.colasoft.opensearch.action.index.IndexAction;
import com.colasoft.opensearch.action.index.TransportIndexAction;
import com.colasoft.opensearch.action.ingest.DeletePipelineAction;
import com.colasoft.opensearch.action.ingest.DeletePipelineTransportAction;
import com.colasoft.opensearch.action.ingest.GetPipelineAction;
import com.colasoft.opensearch.action.ingest.GetPipelineTransportAction;
import com.colasoft.opensearch.action.ingest.PutPipelineAction;
import com.colasoft.opensearch.action.ingest.PutPipelineTransportAction;
import com.colasoft.opensearch.action.ingest.SimulatePipelineAction;
import com.colasoft.opensearch.action.ingest.SimulatePipelineTransportAction;
import com.colasoft.opensearch.action.main.MainAction;
import com.colasoft.opensearch.action.main.TransportMainAction;
import com.colasoft.opensearch.action.search.ClearScrollAction;
import com.colasoft.opensearch.action.search.CreatePitAction;
import com.colasoft.opensearch.action.search.DeletePitAction;
import com.colasoft.opensearch.action.search.DeleteSearchPipelineAction;
import com.colasoft.opensearch.action.search.DeleteSearchPipelineTransportAction;
import com.colasoft.opensearch.action.search.GetSearchPipelineAction;
import com.colasoft.opensearch.action.search.GetSearchPipelineTransportAction;
import com.colasoft.opensearch.action.search.MultiSearchAction;
import com.colasoft.opensearch.action.search.GetAllPitsAction;
import com.colasoft.opensearch.action.search.PutSearchPipelineAction;
import com.colasoft.opensearch.action.search.PutSearchPipelineTransportAction;
import com.colasoft.opensearch.action.search.SearchAction;
import com.colasoft.opensearch.action.search.SearchScrollAction;
import com.colasoft.opensearch.action.search.TransportClearScrollAction;
import com.colasoft.opensearch.action.search.TransportCreatePitAction;
import com.colasoft.opensearch.action.search.TransportDeletePitAction;
import com.colasoft.opensearch.action.search.TransportGetAllPitsAction;
import com.colasoft.opensearch.action.search.TransportMultiSearchAction;
import com.colasoft.opensearch.action.search.TransportSearchAction;
import com.colasoft.opensearch.action.search.TransportSearchScrollAction;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.AutoCreateIndex;
import com.colasoft.opensearch.action.support.DestructiveOperations;
import com.colasoft.opensearch.action.support.TransportAction;
import com.colasoft.opensearch.action.termvectors.MultiTermVectorsAction;
import com.colasoft.opensearch.action.termvectors.TermVectorsAction;
import com.colasoft.opensearch.action.termvectors.TransportMultiTermVectorsAction;
import com.colasoft.opensearch.action.termvectors.TransportShardMultiTermsVectorAction;
import com.colasoft.opensearch.action.termvectors.TransportTermVectorsAction;
import com.colasoft.opensearch.action.update.TransportUpdateAction;
import com.colasoft.opensearch.action.update.UpdateAction;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.NamedRegistry;
import com.colasoft.opensearch.common.inject.AbstractModule;
import com.colasoft.opensearch.common.inject.TypeLiteral;
import com.colasoft.opensearch.common.inject.multibindings.MapBinder;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.common.util.FeatureFlags;
import com.colasoft.opensearch.extensions.action.ExtensionProxyAction;
import com.colasoft.opensearch.extensions.action.ExtensionProxyTransportAction;
import com.colasoft.opensearch.index.seqno.RetentionLeaseActions;
import com.colasoft.opensearch.indices.SystemIndices;
import com.colasoft.opensearch.indices.breaker.CircuitBreakerService;
import com.colasoft.opensearch.persistent.CompletionPersistentTaskAction;
import com.colasoft.opensearch.persistent.RemovePersistentTaskAction;
import com.colasoft.opensearch.persistent.StartPersistentTaskAction;
import com.colasoft.opensearch.persistent.UpdatePersistentTaskStatusAction;
import com.colasoft.opensearch.plugins.ActionPlugin;
import com.colasoft.opensearch.plugins.ActionPlugin.ActionHandler;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.rest.RestHeaderDefinition;
import com.colasoft.opensearch.rest.action.RestFieldCapabilitiesAction;
import com.colasoft.opensearch.rest.action.RestMainAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestAddVotingConfigExclusionAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCancelTasksAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCleanupRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClearVotingConfigExclusionsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCloneSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterAllocationExplainAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterDeleteWeightedRoutingAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterGetSettingsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterGetWeightedRoutingAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterHealthAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterPutWeightedRoutingAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterRerouteAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterSearchShardsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterStateAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterStatsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestClusterUpdateSettingsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestCreateSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteDecommissionStateAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDeleteStoredScriptAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetDecommissionStateAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetRepositoriesAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetScriptContextAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetScriptLanguageAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetSnapshotsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetStoredScriptAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestGetTaskAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestListTasksAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestNodesHotThreadsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestNodesInfoAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestNodesStatsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestNodesUsageAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestPendingClusterTasksAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestDecommissionAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestPutRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestPutStoredScriptAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestReloadSecureSettingsAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestRemoteClusterInfoAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestRestoreRemoteStoreAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestRestoreSnapshotAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestSnapshotsStatusAction;
import com.colasoft.opensearch.rest.action.admin.cluster.RestVerifyRepositoryAction;
import com.colasoft.opensearch.rest.action.admin.cluster.dangling.RestDeleteDanglingIndexAction;
import com.colasoft.opensearch.rest.action.admin.cluster.dangling.RestImportDanglingIndexAction;
import com.colasoft.opensearch.rest.action.admin.cluster.dangling.RestListDanglingIndicesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestAddIndexBlockAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestAnalyzeAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestClearIndicesCacheAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestCloseIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestCreateDataStreamAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestCreateIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDataStreamsStatsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteDataStreamAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestDeleteIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestFlushAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestForceMergeAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetDataStreamsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetFieldMappingAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetIndicesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetMappingAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestGetSettingsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndexDeleteAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndexPutAliasAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndicesAliasesAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndicesSegmentsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndicesShardStoresAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestIndicesStatsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestOpenIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutComponentTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutComposableIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestPutMappingAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestRecoveryAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestRefreshAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestResizeHandler;
import com.colasoft.opensearch.rest.action.admin.indices.RestResolveIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestRolloverIndexAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestSimulateIndexTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestSimulateTemplateAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestSyncedFlushAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestUpdateSettingsAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestUpgradeAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestUpgradeStatusAction;
import com.colasoft.opensearch.rest.action.admin.indices.RestValidateQueryAction;
import com.colasoft.opensearch.rest.action.cat.AbstractCatAction;
import com.colasoft.opensearch.rest.action.cat.RestAliasAction;
import com.colasoft.opensearch.rest.action.cat.RestAllocationAction;
import com.colasoft.opensearch.rest.action.cat.RestCatAction;
import com.colasoft.opensearch.rest.action.cat.RestCatRecoveryAction;
import com.colasoft.opensearch.rest.action.cat.RestCatSegmentReplicationAction;
import com.colasoft.opensearch.rest.action.cat.RestFielddataAction;
import com.colasoft.opensearch.rest.action.cat.RestHealthAction;
import com.colasoft.opensearch.rest.action.cat.RestIndicesAction;
import com.colasoft.opensearch.rest.action.cat.RestClusterManagerAction;
import com.colasoft.opensearch.rest.action.cat.RestNodeAttrsAction;
import com.colasoft.opensearch.rest.action.cat.RestNodesAction;
import com.colasoft.opensearch.rest.action.cat.RestPitSegmentsAction;
import com.colasoft.opensearch.rest.action.cat.RestPluginsAction;
import com.colasoft.opensearch.rest.action.cat.RestRepositoriesAction;
import com.colasoft.opensearch.rest.action.cat.RestSegmentsAction;
import com.colasoft.opensearch.rest.action.cat.RestShardsAction;
import com.colasoft.opensearch.rest.action.cat.RestSnapshotAction;
import com.colasoft.opensearch.rest.action.cat.RestTasksAction;
import com.colasoft.opensearch.rest.action.cat.RestTemplatesAction;
import com.colasoft.opensearch.rest.action.cat.RestThreadPoolAction;
import com.colasoft.opensearch.rest.action.document.RestBulkAction;
import com.colasoft.opensearch.rest.action.document.RestDeleteAction;
import com.colasoft.opensearch.rest.action.document.RestGetAction;
import com.colasoft.opensearch.rest.action.document.RestGetSourceAction;
import com.colasoft.opensearch.rest.action.document.RestIndexAction;
import com.colasoft.opensearch.rest.action.document.RestIndexAction.AutoIdHandler;
import com.colasoft.opensearch.rest.action.document.RestIndexAction.CreateHandler;
import com.colasoft.opensearch.rest.action.document.RestMultiGetAction;
import com.colasoft.opensearch.rest.action.document.RestMultiTermVectorsAction;
import com.colasoft.opensearch.rest.action.document.RestTermVectorsAction;
import com.colasoft.opensearch.rest.action.document.RestUpdateAction;
import com.colasoft.opensearch.rest.action.ingest.RestDeletePipelineAction;
import com.colasoft.opensearch.rest.action.ingest.RestGetPipelineAction;
import com.colasoft.opensearch.rest.action.ingest.RestPutPipelineAction;
import com.colasoft.opensearch.rest.action.ingest.RestSimulatePipelineAction;
import com.colasoft.opensearch.rest.action.search.RestClearScrollAction;
import com.colasoft.opensearch.rest.action.search.RestCountAction;
import com.colasoft.opensearch.rest.action.search.RestCreatePitAction;
import com.colasoft.opensearch.rest.action.search.RestDeletePitAction;
import com.colasoft.opensearch.rest.action.search.RestDeleteSearchPipelineAction;
import com.colasoft.opensearch.rest.action.search.RestExplainAction;
import com.colasoft.opensearch.rest.action.search.RestGetAllPitsAction;
import com.colasoft.opensearch.rest.action.search.RestGetSearchPipelineAction;
import com.colasoft.opensearch.rest.action.search.RestMultiSearchAction;
import com.colasoft.opensearch.rest.action.search.RestPutSearchPipelineAction;
import com.colasoft.opensearch.rest.action.search.RestSearchAction;
import com.colasoft.opensearch.rest.action.search.RestSearchScrollAction;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.usage.UsageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Builds and binds the generic action map, all {@link TransportAction}s, and {@link ActionFilters}.
 *
 * @opensearch.internal
 */
public class ActionModule extends AbstractModule {

    private static final Logger logger = LogManager.getLogger(ActionModule.class);

    private final Settings settings;
    private final IndexNameExpressionResolver indexNameExpressionResolver;
    private final IndexScopedSettings indexScopedSettings;
    private final ClusterSettings clusterSettings;
    private final SettingsFilter settingsFilter;
    private final List<ActionPlugin> actionPlugins;
    // The unmodifiable map containing OpenSearch and Plugin actions
    // This is initialized at node bootstrap and contains same-JVM actions
    // It will be wrapped in the Dynamic Action Registry but otherwise
    // remains unchanged from its prior purpose, and registered actions
    // will remain accessible.
    private final Map<String, ActionHandler<?, ?>> actions;
    // A dynamic action registry which includes the above immutable actions
    // and also registers dynamic actions which may be unregistered. Usually
    // associated with remote action execution on extensions, possibly in
    // a different JVM and possibly on a different server.
    private final DynamicActionRegistry dynamicActionRegistry;
    private final ActionFilters actionFilters;
    private final AutoCreateIndex autoCreateIndex;
    private final DestructiveOperations destructiveOperations;
    private final RestController restController;
    private final RequestValidators<PutMappingRequest> mappingRequestValidators;
    private final RequestValidators<IndicesAliasesRequest> indicesAliasesRequestRequestValidators;
    private final ThreadPool threadPool;

    public ActionModule(
        Settings settings,
        IndexNameExpressionResolver indexNameExpressionResolver,
        IndexScopedSettings indexScopedSettings,
        ClusterSettings clusterSettings,
        SettingsFilter settingsFilter,
        ThreadPool threadPool,
        List<ActionPlugin> actionPlugins,
        NodeClient nodeClient,
        CircuitBreakerService circuitBreakerService,
        UsageService usageService,
        SystemIndices systemIndices
    ) {
        this.settings = settings;
        this.indexNameExpressionResolver = indexNameExpressionResolver;
        this.indexScopedSettings = indexScopedSettings;
        this.clusterSettings = clusterSettings;
        this.settingsFilter = settingsFilter;
        this.actionPlugins = actionPlugins;
        this.threadPool = threadPool;
        actions = setupActions(actionPlugins);
        actionFilters = setupActionFilters(actionPlugins);
        dynamicActionRegistry = new DynamicActionRegistry();
        autoCreateIndex = new AutoCreateIndex(settings, clusterSettings, indexNameExpressionResolver, systemIndices);
        destructiveOperations = new DestructiveOperations(settings, clusterSettings);
        Set<RestHeaderDefinition> headers = Stream.concat(
            actionPlugins.stream().flatMap(p -> p.getRestHeaders().stream()),
            Stream.of(new RestHeaderDefinition(Task.X_OPAQUE_ID, false))
        ).collect(Collectors.toSet());
        UnaryOperator<RestHandler> restWrapper = null;
        for (ActionPlugin plugin : actionPlugins) {
            UnaryOperator<RestHandler> newRestWrapper = plugin.getRestHandlerWrapper(threadPool.getThreadContext());
            if (newRestWrapper != null) {
                logger.debug("Using REST wrapper from plugin " + plugin.getClass().getName());
                if (restWrapper != null) {
                    throw new IllegalArgumentException("Cannot have more than one plugin implementing a REST wrapper");
                }
                restWrapper = newRestWrapper;
            }
        }
        mappingRequestValidators = new RequestValidators<>(
            actionPlugins.stream().flatMap(p -> p.mappingRequestValidators().stream()).collect(Collectors.toList())
        );
        indicesAliasesRequestRequestValidators = new RequestValidators<>(
            actionPlugins.stream().flatMap(p -> p.indicesAliasesRequestValidators().stream()).collect(Collectors.toList())
        );

        restController = new RestController(headers, restWrapper, nodeClient, circuitBreakerService, usageService);
    }

    public Map<String, ActionHandler<?, ?>> getActions() {
        return actions;
    }

    static Map<String, ActionHandler<?, ?>> setupActions(List<ActionPlugin> actionPlugins) {
        // Subclass NamedRegistry for easy registration
        class ActionRegistry extends NamedRegistry<ActionHandler<?, ?>> {
            ActionRegistry() {
                super("action");
            }

            public void register(ActionHandler<?, ?> handler) {
                register(handler.getAction().name(), handler);
            }

            public <Request extends ActionRequest, Response extends ActionResponse> void register(
                ActionType<Response> action,
                Class<? extends TransportAction<Request, Response>> transportAction,
                Class<?>... supportTransportActions
            ) {
                register(new ActionHandler<>(action, transportAction, supportTransportActions));
            }
        }
        ActionRegistry actions = new ActionRegistry();

        actions.register(MainAction.INSTANCE, TransportMainAction.class);
        actions.register(NodesInfoAction.INSTANCE, TransportNodesInfoAction.class);
        actions.register(RemoteInfoAction.INSTANCE, TransportRemoteInfoAction.class);
        actions.register(NodesStatsAction.INSTANCE, TransportNodesStatsAction.class);
        actions.register(NodesUsageAction.INSTANCE, TransportNodesUsageAction.class);
        actions.register(NodesHotThreadsAction.INSTANCE, TransportNodesHotThreadsAction.class);
        actions.register(ListTasksAction.INSTANCE, TransportListTasksAction.class);
        actions.register(GetTaskAction.INSTANCE, TransportGetTaskAction.class);
        actions.register(CancelTasksAction.INSTANCE, TransportCancelTasksAction.class);

        actions.register(AddVotingConfigExclusionsAction.INSTANCE, TransportAddVotingConfigExclusionsAction.class);
        actions.register(ClearVotingConfigExclusionsAction.INSTANCE, TransportClearVotingConfigExclusionsAction.class);
        actions.register(ClusterAllocationExplainAction.INSTANCE, TransportClusterAllocationExplainAction.class);
        actions.register(ClusterStatsAction.INSTANCE, TransportClusterStatsAction.class);
        actions.register(ClusterStateAction.INSTANCE, TransportClusterStateAction.class);
        actions.register(ClusterHealthAction.INSTANCE, TransportClusterHealthAction.class);
        actions.register(ClusterUpdateSettingsAction.INSTANCE, TransportClusterUpdateSettingsAction.class);
        actions.register(ClusterRerouteAction.INSTANCE, TransportClusterRerouteAction.class);
        actions.register(ClusterSearchShardsAction.INSTANCE, TransportClusterSearchShardsAction.class);
        actions.register(PendingClusterTasksAction.INSTANCE, TransportPendingClusterTasksAction.class);
        actions.register(PutRepositoryAction.INSTANCE, TransportPutRepositoryAction.class);
        actions.register(GetRepositoriesAction.INSTANCE, TransportGetRepositoriesAction.class);
        actions.register(DeleteRepositoryAction.INSTANCE, TransportDeleteRepositoryAction.class);
        actions.register(VerifyRepositoryAction.INSTANCE, TransportVerifyRepositoryAction.class);
        actions.register(CleanupRepositoryAction.INSTANCE, TransportCleanupRepositoryAction.class);
        actions.register(GetSnapshotsAction.INSTANCE, TransportGetSnapshotsAction.class);
        actions.register(DeleteSnapshotAction.INSTANCE, TransportDeleteSnapshotAction.class);
        actions.register(CreateSnapshotAction.INSTANCE, TransportCreateSnapshotAction.class);
        actions.register(CloneSnapshotAction.INSTANCE, TransportCloneSnapshotAction.class);
        actions.register(RestoreSnapshotAction.INSTANCE, TransportRestoreSnapshotAction.class);
        actions.register(SnapshotsStatusAction.INSTANCE, TransportSnapshotsStatusAction.class);

        actions.register(ClusterAddWeightedRoutingAction.INSTANCE, TransportAddWeightedRoutingAction.class);
        actions.register(ClusterGetWeightedRoutingAction.INSTANCE, TransportGetWeightedRoutingAction.class);
        actions.register(ClusterDeleteWeightedRoutingAction.INSTANCE, TransportDeleteWeightedRoutingAction.class);
        actions.register(IndicesStatsAction.INSTANCE, TransportIndicesStatsAction.class);
        actions.register(IndicesSegmentsAction.INSTANCE, TransportIndicesSegmentsAction.class);
        actions.register(IndicesShardStoresAction.INSTANCE, TransportIndicesShardStoresAction.class);
        actions.register(CreateIndexAction.INSTANCE, TransportCreateIndexAction.class);
        actions.register(ResizeAction.INSTANCE, TransportResizeAction.class);
        actions.register(RolloverAction.INSTANCE, TransportRolloverAction.class);
        actions.register(DeleteIndexAction.INSTANCE, TransportDeleteIndexAction.class);
        actions.register(GetIndexAction.INSTANCE, TransportGetIndexAction.class);
        actions.register(OpenIndexAction.INSTANCE, TransportOpenIndexAction.class);
        actions.register(CloseIndexAction.INSTANCE, TransportCloseIndexAction.class);
        actions.register(IndicesExistsAction.INSTANCE, TransportIndicesExistsAction.class);
        actions.register(AddIndexBlockAction.INSTANCE, TransportAddIndexBlockAction.class);
        actions.register(GetMappingsAction.INSTANCE, TransportGetMappingsAction.class);
        actions.register(
            GetFieldMappingsAction.INSTANCE,
            TransportGetFieldMappingsAction.class,
            TransportGetFieldMappingsIndexAction.class
        );
        actions.register(PutMappingAction.INSTANCE, TransportPutMappingAction.class);
        actions.register(AutoPutMappingAction.INSTANCE, TransportAutoPutMappingAction.class);
        actions.register(IndicesAliasesAction.INSTANCE, TransportIndicesAliasesAction.class);
        actions.register(UpdateSettingsAction.INSTANCE, TransportUpdateSettingsAction.class);
        actions.register(AnalyzeAction.INSTANCE, TransportAnalyzeAction.class);
        actions.register(PutIndexTemplateAction.INSTANCE, TransportPutIndexTemplateAction.class);
        actions.register(GetIndexTemplatesAction.INSTANCE, TransportGetIndexTemplatesAction.class);
        actions.register(DeleteIndexTemplateAction.INSTANCE, TransportDeleteIndexTemplateAction.class);
        actions.register(PutComponentTemplateAction.INSTANCE, TransportPutComponentTemplateAction.class);
        actions.register(GetComponentTemplateAction.INSTANCE, TransportGetComponentTemplateAction.class);
        actions.register(DeleteComponentTemplateAction.INSTANCE, TransportDeleteComponentTemplateAction.class);
        actions.register(PutComposableIndexTemplateAction.INSTANCE, TransportPutComposableIndexTemplateAction.class);
        actions.register(GetComposableIndexTemplateAction.INSTANCE, TransportGetComposableIndexTemplateAction.class);
        actions.register(DeleteComposableIndexTemplateAction.INSTANCE, TransportDeleteComposableIndexTemplateAction.class);
        actions.register(SimulateIndexTemplateAction.INSTANCE, TransportSimulateIndexTemplateAction.class);
        actions.register(SimulateTemplateAction.INSTANCE, TransportSimulateTemplateAction.class);
        actions.register(ValidateQueryAction.INSTANCE, TransportValidateQueryAction.class);
        actions.register(RefreshAction.INSTANCE, TransportRefreshAction.class);
        actions.register(FlushAction.INSTANCE, TransportFlushAction.class);
        actions.register(ForceMergeAction.INSTANCE, TransportForceMergeAction.class);
        actions.register(UpgradeAction.INSTANCE, TransportUpgradeAction.class);
        actions.register(UpgradeStatusAction.INSTANCE, TransportUpgradeStatusAction.class);
        actions.register(UpgradeSettingsAction.INSTANCE, TransportUpgradeSettingsAction.class);
        actions.register(ClearIndicesCacheAction.INSTANCE, TransportClearIndicesCacheAction.class);
        actions.register(GetAliasesAction.INSTANCE, TransportGetAliasesAction.class);
        actions.register(GetSettingsAction.INSTANCE, TransportGetSettingsAction.class);

        actions.register(IndexAction.INSTANCE, TransportIndexAction.class);
        actions.register(GetAction.INSTANCE, TransportGetAction.class);
        actions.register(TermVectorsAction.INSTANCE, TransportTermVectorsAction.class);
        actions.register(
            MultiTermVectorsAction.INSTANCE,
            TransportMultiTermVectorsAction.class,
            TransportShardMultiTermsVectorAction.class
        );
        actions.register(DeleteAction.INSTANCE, TransportDeleteAction.class);
        actions.register(UpdateAction.INSTANCE, TransportUpdateAction.class);
        actions.register(MultiGetAction.INSTANCE, TransportMultiGetAction.class, TransportShardMultiGetAction.class);
        actions.register(BulkAction.INSTANCE, TransportBulkAction.class, TransportShardBulkAction.class);
        actions.register(SearchAction.INSTANCE, TransportSearchAction.class);
        actions.register(SearchScrollAction.INSTANCE, TransportSearchScrollAction.class);
        actions.register(MultiSearchAction.INSTANCE, TransportMultiSearchAction.class);
        actions.register(ExplainAction.INSTANCE, TransportExplainAction.class);
        actions.register(ClearScrollAction.INSTANCE, TransportClearScrollAction.class);
        actions.register(RecoveryAction.INSTANCE, TransportRecoveryAction.class);
        actions.register(SegmentReplicationStatsAction.INSTANCE, TransportSegmentReplicationStatsAction.class);
        actions.register(NodesReloadSecureSettingsAction.INSTANCE, TransportNodesReloadSecureSettingsAction.class);
        actions.register(AutoCreateAction.INSTANCE, AutoCreateAction.TransportAction.class);

        // Indexed scripts
        actions.register(PutStoredScriptAction.INSTANCE, TransportPutStoredScriptAction.class);
        actions.register(GetStoredScriptAction.INSTANCE, TransportGetStoredScriptAction.class);
        actions.register(DeleteStoredScriptAction.INSTANCE, TransportDeleteStoredScriptAction.class);
        actions.register(GetScriptContextAction.INSTANCE, TransportGetScriptContextAction.class);
        actions.register(GetScriptLanguageAction.INSTANCE, TransportGetScriptLanguageAction.class);

        actions.register(
            FieldCapabilitiesAction.INSTANCE,
            TransportFieldCapabilitiesAction.class,
            TransportFieldCapabilitiesIndexAction.class
        );

        actions.register(PutPipelineAction.INSTANCE, PutPipelineTransportAction.class);
        actions.register(GetPipelineAction.INSTANCE, GetPipelineTransportAction.class);
        actions.register(DeletePipelineAction.INSTANCE, DeletePipelineTransportAction.class);
        actions.register(SimulatePipelineAction.INSTANCE, SimulatePipelineTransportAction.class);

        actionPlugins.stream().flatMap(p -> p.getActions().stream()).forEach(actions::register);

        // Data streams:
        actions.register(CreateDataStreamAction.INSTANCE, CreateDataStreamAction.TransportAction.class);
        actions.register(DeleteDataStreamAction.INSTANCE, DeleteDataStreamAction.TransportAction.class);
        actions.register(GetDataStreamAction.INSTANCE, GetDataStreamAction.TransportAction.class);
        actions.register(ResolveIndexAction.INSTANCE, ResolveIndexAction.TransportAction.class);
        actions.register(DataStreamsStatsAction.INSTANCE, DataStreamsStatsAction.TransportAction.class);

        // Persistent tasks:
        actions.register(StartPersistentTaskAction.INSTANCE, StartPersistentTaskAction.TransportAction.class);
        actions.register(UpdatePersistentTaskStatusAction.INSTANCE, UpdatePersistentTaskStatusAction.TransportAction.class);
        actions.register(CompletionPersistentTaskAction.INSTANCE, CompletionPersistentTaskAction.TransportAction.class);
        actions.register(RemovePersistentTaskAction.INSTANCE, RemovePersistentTaskAction.TransportAction.class);

        // retention leases
        actions.register(RetentionLeaseActions.Add.INSTANCE, RetentionLeaseActions.Add.TransportAction.class);
        actions.register(RetentionLeaseActions.Renew.INSTANCE, RetentionLeaseActions.Renew.TransportAction.class);
        actions.register(RetentionLeaseActions.Remove.INSTANCE, RetentionLeaseActions.Remove.TransportAction.class);

        // Dangling indices
        actions.register(ListDanglingIndicesAction.INSTANCE, TransportListDanglingIndicesAction.class);
        actions.register(ImportDanglingIndexAction.INSTANCE, TransportImportDanglingIndexAction.class);
        actions.register(DeleteDanglingIndexAction.INSTANCE, TransportDeleteDanglingIndexAction.class);
        actions.register(FindDanglingIndexAction.INSTANCE, TransportFindDanglingIndexAction.class);

        // Remote Store
        actions.register(RestoreRemoteStoreAction.INSTANCE, TransportRestoreRemoteStoreAction.class);

        // point in time actions
        actions.register(CreatePitAction.INSTANCE, TransportCreatePitAction.class);
        actions.register(DeletePitAction.INSTANCE, TransportDeletePitAction.class);
        actions.register(PitSegmentsAction.INSTANCE, TransportPitSegmentsAction.class);
        actions.register(GetAllPitsAction.INSTANCE, TransportGetAllPitsAction.class);

        if (FeatureFlags.isEnabled(FeatureFlags.EXTENSIONS)) {
            // ExtensionProxyAction
            actions.register(ExtensionProxyAction.INSTANCE, ExtensionProxyTransportAction.class);
        }

        // Decommission actions
        actions.register(DecommissionAction.INSTANCE, TransportDecommissionAction.class);
        actions.register(GetDecommissionStateAction.INSTANCE, TransportGetDecommissionStateAction.class);
        actions.register(DeleteDecommissionStateAction.INSTANCE, TransportDeleteDecommissionStateAction.class);

        // Search Pipelines
        actions.register(PutSearchPipelineAction.INSTANCE, PutSearchPipelineTransportAction.class);
        actions.register(GetSearchPipelineAction.INSTANCE, GetSearchPipelineTransportAction.class);
        actions.register(DeleteSearchPipelineAction.INSTANCE, DeleteSearchPipelineTransportAction.class);

        return unmodifiableMap(actions.getRegistry());
    }

    private ActionFilters setupActionFilters(List<ActionPlugin> actionPlugins) {
        return new ActionFilters(
            Collections.unmodifiableSet(actionPlugins.stream().flatMap(p -> p.getActionFilters().stream()).collect(Collectors.toSet()))
        );
    }

    public void initRestHandlers(Supplier<DiscoveryNodes> nodesInCluster) {
        List<AbstractCatAction> catActions = new ArrayList<>();
        Consumer<RestHandler> registerHandler = handler -> {
            if (handler instanceof AbstractCatAction) {
                catActions.add((AbstractCatAction) handler);
            }
            restController.registerHandler(handler);
        };
        registerHandler.accept(new RestAddVotingConfigExclusionAction());
        registerHandler.accept(new RestClearVotingConfigExclusionsAction());
        registerHandler.accept(new RestMainAction());
        registerHandler.accept(new RestNodesInfoAction(settingsFilter));
        registerHandler.accept(new RestRemoteClusterInfoAction());
        registerHandler.accept(new RestNodesStatsAction());
        registerHandler.accept(new RestNodesUsageAction());
        registerHandler.accept(new RestNodesHotThreadsAction());
        registerHandler.accept(new RestClusterAllocationExplainAction());
        registerHandler.accept(new RestClusterStatsAction());
        registerHandler.accept(new RestClusterStateAction(settingsFilter));
        registerHandler.accept(new RestClusterHealthAction());
        registerHandler.accept(new RestClusterUpdateSettingsAction());
        registerHandler.accept(new RestClusterGetSettingsAction(settings, clusterSettings, settingsFilter));
        registerHandler.accept(new RestClusterRerouteAction(settingsFilter));
        registerHandler.accept(new RestClusterSearchShardsAction());
        registerHandler.accept(new RestPendingClusterTasksAction());
        registerHandler.accept(new RestPutRepositoryAction());
        registerHandler.accept(new RestGetRepositoriesAction(settingsFilter));
        registerHandler.accept(new RestDeleteRepositoryAction());
        registerHandler.accept(new RestVerifyRepositoryAction());
        registerHandler.accept(new RestCleanupRepositoryAction());
        registerHandler.accept(new RestGetSnapshotsAction());
        registerHandler.accept(new RestCreateSnapshotAction());
        registerHandler.accept(new RestCloneSnapshotAction());
        registerHandler.accept(new RestRestoreSnapshotAction());
        registerHandler.accept(new RestDeleteSnapshotAction());
        registerHandler.accept(new RestSnapshotsStatusAction());
        registerHandler.accept(new RestGetIndicesAction());
        registerHandler.accept(new RestIndicesStatsAction());
        registerHandler.accept(new RestIndicesSegmentsAction());
        registerHandler.accept(new RestIndicesShardStoresAction());
        registerHandler.accept(new RestGetAliasesAction());
        registerHandler.accept(new RestIndexDeleteAliasesAction());
        registerHandler.accept(new RestIndexPutAliasAction());
        registerHandler.accept(new RestIndicesAliasesAction());
        registerHandler.accept(new RestCreateIndexAction());
        registerHandler.accept(new RestResizeHandler.RestShrinkIndexAction());
        registerHandler.accept(new RestResizeHandler.RestSplitIndexAction());
        registerHandler.accept(new RestResizeHandler.RestCloneIndexAction());
        registerHandler.accept(new RestRolloverIndexAction());
        registerHandler.accept(new RestDeleteIndexAction());
        registerHandler.accept(new RestCloseIndexAction());
        registerHandler.accept(new RestOpenIndexAction());
        registerHandler.accept(new RestAddIndexBlockAction());

        registerHandler.accept(new RestClusterPutWeightedRoutingAction());
        registerHandler.accept(new RestClusterGetWeightedRoutingAction());
        registerHandler.accept(new RestClusterDeleteWeightedRoutingAction());

        registerHandler.accept(new RestUpdateSettingsAction());
        registerHandler.accept(new RestGetSettingsAction());

        registerHandler.accept(new RestAnalyzeAction());
        registerHandler.accept(new RestGetIndexTemplateAction());
        registerHandler.accept(new RestPutIndexTemplateAction());
        registerHandler.accept(new RestDeleteIndexTemplateAction());
        registerHandler.accept(new RestPutComponentTemplateAction());
        registerHandler.accept(new RestGetComponentTemplateAction());
        registerHandler.accept(new RestDeleteComponentTemplateAction());
        registerHandler.accept(new RestPutComposableIndexTemplateAction());
        registerHandler.accept(new RestGetComposableIndexTemplateAction());
        registerHandler.accept(new RestDeleteComposableIndexTemplateAction());
        registerHandler.accept(new RestSimulateIndexTemplateAction());
        registerHandler.accept(new RestSimulateTemplateAction());

        registerHandler.accept(new RestPutMappingAction());
        registerHandler.accept(new RestGetMappingAction(threadPool));
        registerHandler.accept(new RestGetFieldMappingAction());

        registerHandler.accept(new RestRefreshAction());
        registerHandler.accept(new RestFlushAction());
        registerHandler.accept(new RestSyncedFlushAction());
        registerHandler.accept(new RestForceMergeAction());
        registerHandler.accept(new RestUpgradeAction());
        registerHandler.accept(new RestUpgradeStatusAction());
        registerHandler.accept(new RestClearIndicesCacheAction());

        registerHandler.accept(new RestIndexAction());
        registerHandler.accept(new CreateHandler());
        registerHandler.accept(new AutoIdHandler(nodesInCluster));
        registerHandler.accept(new RestGetAction());
        registerHandler.accept(new RestGetSourceAction());
        registerHandler.accept(new RestMultiGetAction(settings));
        registerHandler.accept(new RestDeleteAction());
        registerHandler.accept(new RestCountAction());
        registerHandler.accept(new RestTermVectorsAction());
        registerHandler.accept(new RestMultiTermVectorsAction());
        registerHandler.accept(new RestBulkAction(settings));
        registerHandler.accept(new RestUpdateAction());

        registerHandler.accept(new RestSearchAction());
        registerHandler.accept(new RestSearchScrollAction());
        registerHandler.accept(new RestClearScrollAction());
        registerHandler.accept(new RestMultiSearchAction(settings));

        registerHandler.accept(new RestValidateQueryAction());

        registerHandler.accept(new RestExplainAction());

        registerHandler.accept(new RestRecoveryAction());

        registerHandler.accept(new RestReloadSecureSettingsAction());

        // Scripts API
        registerHandler.accept(new RestGetStoredScriptAction());
        registerHandler.accept(new RestPutStoredScriptAction());
        registerHandler.accept(new RestDeleteStoredScriptAction());
        registerHandler.accept(new RestGetScriptContextAction());
        registerHandler.accept(new RestGetScriptLanguageAction());

        registerHandler.accept(new RestFieldCapabilitiesAction());

        // Tasks API
        registerHandler.accept(new RestListTasksAction(nodesInCluster));
        registerHandler.accept(new RestGetTaskAction());
        registerHandler.accept(new RestCancelTasksAction(nodesInCluster));

        // Ingest API
        registerHandler.accept(new RestPutPipelineAction());
        registerHandler.accept(new RestGetPipelineAction());
        registerHandler.accept(new RestDeletePipelineAction());
        registerHandler.accept(new RestSimulatePipelineAction());

        // Dangling indices API
        registerHandler.accept(new RestListDanglingIndicesAction());
        registerHandler.accept(new RestImportDanglingIndexAction());
        registerHandler.accept(new RestDeleteDanglingIndexAction());

        // Data Stream API
        registerHandler.accept(new RestCreateDataStreamAction());
        registerHandler.accept(new RestDeleteDataStreamAction());
        registerHandler.accept(new RestGetDataStreamsAction());
        registerHandler.accept(new RestResolveIndexAction());
        registerHandler.accept(new RestDataStreamsStatsAction());

        // CAT API
        registerHandler.accept(new RestAllocationAction());
        registerHandler.accept(new RestCatSegmentReplicationAction());
        registerHandler.accept(new RestShardsAction());
        registerHandler.accept(new RestClusterManagerAction());
        registerHandler.accept(new RestNodesAction());
        registerHandler.accept(new RestTasksAction(nodesInCluster));
        registerHandler.accept(new RestIndicesAction());
        registerHandler.accept(new RestSegmentsAction());
        // Fully qualified to prevent interference with rest.action.count.RestCountAction
        registerHandler.accept(new com.colasoft.opensearch.rest.action.cat.RestCountAction());
        // Fully qualified to prevent interference with rest.action.indices.RestRecoveryAction
        registerHandler.accept(new RestCatRecoveryAction());
        registerHandler.accept(new RestHealthAction());
        registerHandler.accept(new com.colasoft.opensearch.rest.action.cat.RestPendingClusterTasksAction());
        registerHandler.accept(new RestAliasAction());
        registerHandler.accept(new RestThreadPoolAction());
        registerHandler.accept(new RestPluginsAction());
        registerHandler.accept(new RestFielddataAction());
        registerHandler.accept(new RestNodeAttrsAction());
        registerHandler.accept(new RestRepositoriesAction());
        registerHandler.accept(new RestSnapshotAction());
        registerHandler.accept(new RestTemplatesAction());

        // Point in time API
        registerHandler.accept(new RestCreatePitAction());
        registerHandler.accept(new RestDeletePitAction());
        registerHandler.accept(new RestGetAllPitsAction(nodesInCluster));
        registerHandler.accept(new RestPitSegmentsAction(nodesInCluster));
        registerHandler.accept(new RestDeleteDecommissionStateAction());

        // Search pipelines API
        if (FeatureFlags.isEnabled(FeatureFlags.SEARCH_PIPELINE)) {
            registerHandler.accept(new RestPutSearchPipelineAction());
            registerHandler.accept(new RestGetSearchPipelineAction());
            registerHandler.accept(new RestDeleteSearchPipelineAction());
        }

        for (ActionPlugin plugin : actionPlugins) {
            for (RestHandler handler : plugin.getRestHandlers(
                settings,
                restController,
                clusterSettings,
                indexScopedSettings,
                settingsFilter,
                indexNameExpressionResolver,
                nodesInCluster
            )) {
                registerHandler.accept(handler);
            }
        }
        registerHandler.accept(new RestCatAction(catActions));
        registerHandler.accept(new RestDecommissionAction());
        registerHandler.accept(new RestGetDecommissionStateAction());

        // Remote Store APIs
        if (FeatureFlags.isEnabled(FeatureFlags.REMOTE_STORE)) {
            registerHandler.accept(new RestRestoreRemoteStoreAction());
        }
    }

    @Override
    protected void configure() {
        bind(ActionFilters.class).toInstance(actionFilters);
        bind(DestructiveOperations.class).toInstance(destructiveOperations);
        bind(new TypeLiteral<RequestValidators<PutMappingRequest>>() {
        }).toInstance(mappingRequestValidators);
        bind(new TypeLiteral<RequestValidators<IndicesAliasesRequest>>() {
        }).toInstance(indicesAliasesRequestRequestValidators);

        // Supporting classes
        bind(AutoCreateIndex.class).toInstance(autoCreateIndex);
        bind(TransportLivenessAction.class).asEagerSingleton();

        // register ActionType -> transportAction Map used by NodeClient
        @SuppressWarnings("rawtypes")
        MapBinder<ActionType, TransportAction> transportActionsBinder = MapBinder.newMapBinder(
            binder(),
            ActionType.class,
            TransportAction.class
        );
        for (ActionHandler<?, ?> action : actions.values()) {
            // bind the action as eager singleton, so the map binder one will reuse it
            bind(action.getTransportAction()).asEagerSingleton();
            transportActionsBinder.addBinding(action.getAction()).to(action.getTransportAction()).asEagerSingleton();
            for (Class<?> supportAction : action.getSupportTransportActions()) {
                bind(supportAction).asEagerSingleton();
            }
        }

        // register dynamic ActionType -> transportAction Map used by NodeClient
        bind(DynamicActionRegistry.class).toInstance(dynamicActionRegistry);
    }

    public ActionFilters getActionFilters() {
        return actionFilters;
    }

    public DynamicActionRegistry getDynamicActionRegistry() {
        return dynamicActionRegistry;
    }

    public RestController getRestController() {
        return restController;
    }

    /**
     * The DynamicActionRegistry maintains a registry mapping {@link ActionType} instances to {@link TransportAction} instances.
     * <p>
     * This class is modeled after {@link NamedRegistry} but provides both register and unregister capabilities.
     *
     * @opensearch.internal
     */
    public static class DynamicActionRegistry {
        // This is the unmodifiable actions map created during node bootstrap, which
        // will continue to link ActionType and TransportAction pairs from core and plugin
        // action handler registration.
        private Map<ActionType, TransportAction> actions = Collections.emptyMap();
        // A dynamic registry to add or remove ActionType / TransportAction pairs
        // at times other than node bootstrap.
        private final Map<ActionType<?>, TransportAction<?, ?>> registry = new ConcurrentHashMap<>();

        /**
         * Register the immutable actions in the registry.
         *
         * @param actions The injected map of {@link ActionType} to {@link TransportAction}
         */
        public void registerUnmodifiableActionMap(Map<ActionType, TransportAction> actions) {
            this.actions = actions;
        }

        /**
         * Add a dynamic action to the registry.
         *
         * @param action The action instance to add
         * @param transportAction The corresponding instance of transportAction to execute
         */
        public void registerDynamicAction(ActionType<?> action, TransportAction<?, ?> transportAction) {
            requireNonNull(action, "action is required");
            requireNonNull(transportAction, "transportAction is required");
            if (actions.containsKey(action) || registry.putIfAbsent(action, transportAction) != null) {
                throw new IllegalArgumentException("action [" + action.name() + "] already registered");
            }
        }

        /**
         * Remove a dynamic action from the registry.
         *
         * @param action The action to remove
         */
        public void unregisterDynamicAction(ActionType<?> action) {
            requireNonNull(action, "action is required");
            if (registry.remove(action) == null) {
                throw new IllegalArgumentException("action [" + action.name() + "] was not registered");
            }
        }

        /**
         * Gets the {@link TransportAction} instance corresponding to the {@link ActionType} instance.
         *
         * @param action The {@link ActionType}.
         * @return the corresponding {@link TransportAction} if it is registered, null otherwise.
         */
        @SuppressWarnings("unchecked")
        public TransportAction<? extends ActionRequest, ? extends ActionResponse> get(ActionType<?> action) {
            if (actions.containsKey(action)) {
                return actions.get(action);
            }
            return registry.get(action);
        }
    }
}
