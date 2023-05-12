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

package com.colasoft.opensearch.client.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.ActionFuture;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainAction;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainRequest;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainResponse;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateRequest;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateResponse;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateRequest;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get.GetDecommissionStateResponse;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionAction;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionRequest;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put.DecommissionResponse;
import com.colasoft.opensearch.action.admin.cluster.health.ClusterHealthAction;
import com.colasoft.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import com.colasoft.opensearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.NodesHotThreadsAction;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.NodesHotThreadsRequest;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.NodesHotThreadsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.hotthreads.NodesHotThreadsResponse;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoAction;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoRequest;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoResponse;
import com.colasoft.opensearch.action.admin.cluster.node.reload.NodesReloadSecureSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.node.reload.NodesReloadSecureSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsAction;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsRequest;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.stats.NodesStatsResponse;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.CancelTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.CancelTasksRequest;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.CancelTasksRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.cancel.CancelTasksResponse;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.GetTaskAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.GetTaskRequest;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.GetTaskRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.get.GetTaskResponse;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksAction;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksRequest;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import com.colasoft.opensearch.action.admin.cluster.node.usage.NodesUsageAction;
import com.colasoft.opensearch.action.admin.cluster.node.usage.NodesUsageRequest;
import com.colasoft.opensearch.action.admin.cluster.node.usage.NodesUsageRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.node.usage.NodesUsageResponse;
import com.colasoft.opensearch.action.admin.cluster.remotestore.restore.RestoreRemoteStoreAction;
import com.colasoft.opensearch.action.admin.cluster.remotestore.restore.RestoreRemoteStoreRequest;
import com.colasoft.opensearch.action.admin.cluster.remotestore.restore.RestoreRemoteStoreResponse;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryRequest;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.repositories.cleanup.CleanupRepositoryResponse;
import com.colasoft.opensearch.action.admin.cluster.repositories.delete.DeleteRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequest;
import com.colasoft.opensearch.action.admin.cluster.repositories.delete.DeleteRepositoryRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.GetRepositoriesAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.GetRepositoriesRequest;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.GetRepositoriesRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.repositories.get.GetRepositoriesResponse;
import com.colasoft.opensearch.action.admin.cluster.repositories.put.PutRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import com.colasoft.opensearch.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.VerifyRepositoryAction;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequest;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.VerifyRepositoryRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.repositories.verify.VerifyRepositoryResponse;
import com.colasoft.opensearch.action.admin.cluster.reroute.ClusterRerouteAction;
import com.colasoft.opensearch.action.admin.cluster.reroute.ClusterRerouteRequest;
import com.colasoft.opensearch.action.admin.cluster.reroute.ClusterRerouteRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.reroute.ClusterRerouteResponse;
import com.colasoft.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsAction;
import com.colasoft.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import com.colasoft.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import com.colasoft.opensearch.action.admin.cluster.shards.ClusterSearchShardsAction;
import com.colasoft.opensearch.action.admin.cluster.shards.ClusterSearchShardsRequest;
import com.colasoft.opensearch.action.admin.cluster.shards.ClusterSearchShardsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.shards.ClusterSearchShardsResponse;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.ClusterDeleteWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.ClusterDeleteWeightedRoutingRequest;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.ClusterDeleteWeightedRoutingRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete.ClusterDeleteWeightedRoutingResponse;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.ClusterGetWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.ClusterGetWeightedRoutingRequest;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.ClusterGetWeightedRoutingRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.get.ClusterGetWeightedRoutingResponse;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.ClusterAddWeightedRoutingAction;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.ClusterPutWeightedRoutingRequest;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.ClusterPutWeightedRoutingRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put.ClusterPutWeightedRoutingResponse;
import com.colasoft.opensearch.action.admin.cluster.snapshots.clone.CloneSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.clone.CloneSnapshotRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.clone.CloneSnapshotRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.create.CreateSnapshotResponse;
import com.colasoft.opensearch.action.admin.cluster.snapshots.delete.DeleteSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.restore.RestoreSnapshotResponse;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusAction;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.snapshots.status.SnapshotsStatusResponse;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateAction;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateRequest;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateResponse;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsAction;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsRequest;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsResponse;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.DeleteStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequest;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.DeleteStoredScriptRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetStoredScriptRequest;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetStoredScriptRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.PutStoredScriptAction;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.PutStoredScriptRequest;
import com.colasoft.opensearch.action.admin.cluster.storedscripts.PutStoredScriptRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.tasks.PendingClusterTasksAction;
import com.colasoft.opensearch.action.admin.cluster.tasks.PendingClusterTasksRequest;
import com.colasoft.opensearch.action.admin.cluster.tasks.PendingClusterTasksRequestBuilder;
import com.colasoft.opensearch.action.admin.cluster.tasks.PendingClusterTasksResponse;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesAction;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesAction;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesResponse;
import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeAction;
import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheAction;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexAction;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexRequest;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexResponse;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexAction;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexRequest;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexResponse;
import com.colasoft.opensearch.action.admin.indices.dangling.delete.DeleteDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.delete.DeleteDanglingIndexRequest;
import com.colasoft.opensearch.action.admin.indices.dangling.import_index.ImportDanglingIndexAction;
import com.colasoft.opensearch.action.admin.indices.dangling.import_index.ImportDanglingIndexRequest;
import com.colasoft.opensearch.action.admin.indices.dangling.list.ListDanglingIndicesAction;
import com.colasoft.opensearch.action.admin.indices.dangling.list.ListDanglingIndicesRequest;
import com.colasoft.opensearch.action.admin.indices.dangling.list.ListDanglingIndicesResponse;
import com.colasoft.opensearch.action.admin.indices.datastream.CreateDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.DeleteDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.GetDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexAction;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsAction;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import com.colasoft.opensearch.action.admin.indices.flush.FlushAction;
import com.colasoft.opensearch.action.admin.indices.flush.FlushRequest;
import com.colasoft.opensearch.action.admin.indices.flush.FlushRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.flush.FlushResponse;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeAction;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeRequest;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeResponse;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexAction;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexRequest;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsAction;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexAction;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexRequest;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexResponse;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockAction;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockRequest;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockResponse;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryAction;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryRequest;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryResponse;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshAction;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshRequest;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshResponse;
import com.colasoft.opensearch.action.admin.indices.resolve.ResolveIndexAction;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverAction;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverRequest;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverResponse;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsAction;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsRequest;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsRequest;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.segments.PitSegmentsAction;
import com.colasoft.opensearch.action.admin.indices.segments.PitSegmentsRequest;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsAction;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsAction;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoreRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresAction;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresRequest;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresResponse;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeAction;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeRequest;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeResponse;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsAction;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsRequest;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsResponse;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesAction;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateAction;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusRequest;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusResponse;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeAction;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeRequest;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeResponse;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryAction;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryRequest;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryResponse;
import com.colasoft.opensearch.action.bulk.BulkAction;
import com.colasoft.opensearch.action.bulk.BulkRequest;
import com.colasoft.opensearch.action.bulk.BulkRequestBuilder;
import com.colasoft.opensearch.action.bulk.BulkResponse;
import com.colasoft.opensearch.action.delete.DeleteAction;
import com.colasoft.opensearch.action.delete.DeleteRequest;
import com.colasoft.opensearch.action.delete.DeleteRequestBuilder;
import com.colasoft.opensearch.action.delete.DeleteResponse;
import com.colasoft.opensearch.action.explain.ExplainAction;
import com.colasoft.opensearch.action.explain.ExplainRequest;
import com.colasoft.opensearch.action.explain.ExplainRequestBuilder;
import com.colasoft.opensearch.action.explain.ExplainResponse;
import com.colasoft.opensearch.action.fieldcaps.FieldCapabilitiesAction;
import com.colasoft.opensearch.action.fieldcaps.FieldCapabilitiesRequest;
import com.colasoft.opensearch.action.fieldcaps.FieldCapabilitiesRequestBuilder;
import com.colasoft.opensearch.action.fieldcaps.FieldCapabilitiesResponse;
import com.colasoft.opensearch.action.get.GetAction;
import com.colasoft.opensearch.action.get.GetRequest;
import com.colasoft.opensearch.action.get.GetRequestBuilder;
import com.colasoft.opensearch.action.get.GetResponse;
import com.colasoft.opensearch.action.get.MultiGetAction;
import com.colasoft.opensearch.action.get.MultiGetRequest;
import com.colasoft.opensearch.action.get.MultiGetRequestBuilder;
import com.colasoft.opensearch.action.get.MultiGetResponse;
import com.colasoft.opensearch.action.index.IndexAction;
import com.colasoft.opensearch.action.index.IndexRequest;
import com.colasoft.opensearch.action.index.IndexRequestBuilder;
import com.colasoft.opensearch.action.index.IndexResponse;
import com.colasoft.opensearch.action.ingest.DeletePipelineAction;
import com.colasoft.opensearch.action.ingest.DeletePipelineRequest;
import com.colasoft.opensearch.action.ingest.DeletePipelineRequestBuilder;
import com.colasoft.opensearch.action.ingest.GetPipelineAction;
import com.colasoft.opensearch.action.ingest.GetPipelineRequest;
import com.colasoft.opensearch.action.ingest.GetPipelineRequestBuilder;
import com.colasoft.opensearch.action.ingest.GetPipelineResponse;
import com.colasoft.opensearch.action.ingest.PutPipelineAction;
import com.colasoft.opensearch.action.ingest.PutPipelineRequest;
import com.colasoft.opensearch.action.ingest.PutPipelineRequestBuilder;
import com.colasoft.opensearch.action.ingest.SimulatePipelineAction;
import com.colasoft.opensearch.action.ingest.SimulatePipelineRequest;
import com.colasoft.opensearch.action.ingest.SimulatePipelineRequestBuilder;
import com.colasoft.opensearch.action.ingest.SimulatePipelineResponse;
import com.colasoft.opensearch.action.search.ClearScrollAction;
import com.colasoft.opensearch.action.search.ClearScrollRequest;
import com.colasoft.opensearch.action.search.ClearScrollRequestBuilder;
import com.colasoft.opensearch.action.search.ClearScrollResponse;
import com.colasoft.opensearch.action.search.CreatePitAction;
import com.colasoft.opensearch.action.search.CreatePitRequest;
import com.colasoft.opensearch.action.search.CreatePitResponse;
import com.colasoft.opensearch.action.search.DeletePitAction;
import com.colasoft.opensearch.action.search.DeletePitRequest;
import com.colasoft.opensearch.action.search.DeletePitResponse;
import com.colasoft.opensearch.action.search.DeleteSearchPipelineAction;
import com.colasoft.opensearch.action.search.DeleteSearchPipelineRequest;
import com.colasoft.opensearch.action.search.GetAllPitNodesRequest;
import com.colasoft.opensearch.action.search.GetAllPitNodesResponse;
import com.colasoft.opensearch.action.search.GetSearchPipelineAction;
import com.colasoft.opensearch.action.search.GetSearchPipelineRequest;
import com.colasoft.opensearch.action.search.GetSearchPipelineResponse;
import com.colasoft.opensearch.action.search.MultiSearchAction;
import com.colasoft.opensearch.action.search.MultiSearchRequest;
import com.colasoft.opensearch.action.search.MultiSearchRequestBuilder;
import com.colasoft.opensearch.action.search.MultiSearchResponse;
import com.colasoft.opensearch.action.search.GetAllPitsAction;
import com.colasoft.opensearch.action.search.PutSearchPipelineAction;
import com.colasoft.opensearch.action.search.PutSearchPipelineRequest;
import com.colasoft.opensearch.action.search.SearchAction;
import com.colasoft.opensearch.action.search.SearchRequest;
import com.colasoft.opensearch.action.search.SearchRequestBuilder;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.action.search.SearchScrollAction;
import com.colasoft.opensearch.action.search.SearchScrollRequest;
import com.colasoft.opensearch.action.search.SearchScrollRequestBuilder;
import com.colasoft.opensearch.action.support.PlainActionFuture;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.action.termvectors.MultiTermVectorsAction;
import com.colasoft.opensearch.action.termvectors.MultiTermVectorsRequest;
import com.colasoft.opensearch.action.termvectors.MultiTermVectorsRequestBuilder;
import com.colasoft.opensearch.action.termvectors.MultiTermVectorsResponse;
import com.colasoft.opensearch.action.termvectors.TermVectorsAction;
import com.colasoft.opensearch.action.termvectors.TermVectorsRequest;
import com.colasoft.opensearch.action.termvectors.TermVectorsRequestBuilder;
import com.colasoft.opensearch.action.termvectors.TermVectorsResponse;
import com.colasoft.opensearch.action.update.UpdateAction;
import com.colasoft.opensearch.action.update.UpdateRequest;
import com.colasoft.opensearch.action.update.UpdateRequestBuilder;
import com.colasoft.opensearch.action.update.UpdateResponse;
import com.colasoft.opensearch.client.AdminClient;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.client.ClusterAdminClient;
import com.colasoft.opensearch.client.FilterClient;
import com.colasoft.opensearch.client.IndicesAdminClient;
import com.colasoft.opensearch.client.OpenSearchClient;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata.APIBlock;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.tasks.TaskId;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.util.Map;

/**
 * Base client used to create concrete client implementations
 *
 * @opensearch.internal
 */
public abstract class AbstractClient implements Client {

    protected final Logger logger;

    protected final Settings settings;
    private final ThreadPool threadPool;
    private final Admin admin;

    public AbstractClient(Settings settings, ThreadPool threadPool) {
        this.settings = settings;
        this.threadPool = threadPool;
        this.admin = new Admin(this);
        this.logger = LogManager.getLogger(this.getClass());
    }

    @Override
    public final Settings settings() {
        return this.settings;
    }

    @Override
    public final ThreadPool threadPool() {
        return this.threadPool;
    }

    @Override
    public final AdminClient admin() {
        return admin;
    }

    @Override
    public final <Request extends ActionRequest, Response extends ActionResponse> ActionFuture<Response> execute(
        ActionType<Response> action,
        Request request
    ) {
        PlainActionFuture<Response> actionFuture = PlainActionFuture.newFuture();
        execute(action, request, actionFuture);
        return actionFuture;
    }

    /**
     * This is the single execution point of *all* clients.
     */
    @Override
    public final <Request extends ActionRequest, Response extends ActionResponse> void execute(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    ) {
        doExecute(action, request, listener);
    }

    protected abstract <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
        ActionType<Response> action,
        Request request,
        ActionListener<Response> listener
    );

    @Override
    public ActionFuture<IndexResponse> index(final IndexRequest request) {
        return execute(IndexAction.INSTANCE, request);
    }

    @Override
    public void index(final IndexRequest request, final ActionListener<IndexResponse> listener) {
        execute(IndexAction.INSTANCE, request, listener);
    }

    @Override
    public IndexRequestBuilder prepareIndex() {
        return new IndexRequestBuilder(this, IndexAction.INSTANCE, null);
    }

    @Override
    public IndexRequestBuilder prepareIndex(String index) {
        return new IndexRequestBuilder(this, IndexAction.INSTANCE, index);
    }

    @Override
    public ActionFuture<UpdateResponse> update(final UpdateRequest request) {
        return execute(UpdateAction.INSTANCE, request);
    }

    @Override
    public void update(final UpdateRequest request, final ActionListener<UpdateResponse> listener) {
        execute(UpdateAction.INSTANCE, request, listener);
    }

    @Override
    public UpdateRequestBuilder prepareUpdate() {
        return new UpdateRequestBuilder(this, UpdateAction.INSTANCE, null, null);
    }

    @Override
    public UpdateRequestBuilder prepareUpdate(String index, String id) {
        return new UpdateRequestBuilder(this, UpdateAction.INSTANCE, index, id);
    }

    @Override
    public ActionFuture<DeleteResponse> delete(final DeleteRequest request) {
        return execute(DeleteAction.INSTANCE, request);
    }

    @Override
    public void delete(final DeleteRequest request, final ActionListener<DeleteResponse> listener) {
        execute(DeleteAction.INSTANCE, request, listener);
    }

    @Override
    public DeleteRequestBuilder prepareDelete() {
        return new DeleteRequestBuilder(this, DeleteAction.INSTANCE, null);
    }

    @Override
    public DeleteRequestBuilder prepareDelete(String index, String id) {
        return prepareDelete().setIndex(index).setId(id);
    }

    @Override
    public ActionFuture<BulkResponse> bulk(final BulkRequest request) {
        return execute(BulkAction.INSTANCE, request);
    }

    @Override
    public void bulk(final BulkRequest request, final ActionListener<BulkResponse> listener) {
        execute(BulkAction.INSTANCE, request, listener);
    }

    @Override
    public BulkRequestBuilder prepareBulk() {
        return new BulkRequestBuilder(this, BulkAction.INSTANCE);
    }

    @Override
    public BulkRequestBuilder prepareBulk(@Nullable String globalIndex) {
        return new BulkRequestBuilder(this, BulkAction.INSTANCE, globalIndex);
    }

    @Override
    public ActionFuture<GetResponse> get(final GetRequest request) {
        return execute(GetAction.INSTANCE, request);
    }

    @Override
    public void get(final GetRequest request, final ActionListener<GetResponse> listener) {
        execute(GetAction.INSTANCE, request, listener);
    }

    @Override
    public GetRequestBuilder prepareGet() {
        return new GetRequestBuilder(this, GetAction.INSTANCE, null);
    }

    @Override
    public GetRequestBuilder prepareGet(String index, String id) {
        return prepareGet().setIndex(index).setId(id);
    }

    @Override
    public ActionFuture<MultiGetResponse> multiGet(final MultiGetRequest request) {
        return execute(MultiGetAction.INSTANCE, request);
    }

    @Override
    public void multiGet(final MultiGetRequest request, final ActionListener<MultiGetResponse> listener) {
        execute(MultiGetAction.INSTANCE, request, listener);
    }

    @Override
    public MultiGetRequestBuilder prepareMultiGet() {
        return new MultiGetRequestBuilder(this, MultiGetAction.INSTANCE);
    }

    @Override
    public ActionFuture<SearchResponse> search(final SearchRequest request) {
        return execute(SearchAction.INSTANCE, request);
    }

    @Override
    public void search(final SearchRequest request, final ActionListener<SearchResponse> listener) {
        execute(SearchAction.INSTANCE, request, listener);
    }

    @Override
    public SearchRequestBuilder prepareSearch(String... indices) {
        return new SearchRequestBuilder(this, SearchAction.INSTANCE).setIndices(indices);
    }

    @Override
    public ActionFuture<SearchResponse> searchScroll(final SearchScrollRequest request) {
        return execute(SearchScrollAction.INSTANCE, request);
    }

    @Override
    public void searchScroll(final SearchScrollRequest request, final ActionListener<SearchResponse> listener) {
        execute(SearchScrollAction.INSTANCE, request, listener);
    }

    @Override
    public SearchScrollRequestBuilder prepareSearchScroll(String scrollId) {
        return new SearchScrollRequestBuilder(this, SearchScrollAction.INSTANCE, scrollId);
    }

    @Override
    public void createPit(final CreatePitRequest createPITRequest, final ActionListener<CreatePitResponse> listener) {
        execute(CreatePitAction.INSTANCE, createPITRequest, listener);
    }

    @Override
    public void deletePits(final DeletePitRequest deletePITRequest, final ActionListener<DeletePitResponse> listener) {
        execute(DeletePitAction.INSTANCE, deletePITRequest, listener);
    }

    @Override
    public void getAllPits(final GetAllPitNodesRequest getAllPitNodesRequest, final ActionListener<GetAllPitNodesResponse> listener) {
        execute(GetAllPitsAction.INSTANCE, getAllPitNodesRequest, listener);
    }

    @Override
    public void pitSegments(final PitSegmentsRequest request, final ActionListener<IndicesSegmentResponse> listener) {
        execute(PitSegmentsAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<MultiSearchResponse> multiSearch(MultiSearchRequest request) {
        return execute(MultiSearchAction.INSTANCE, request);
    }

    @Override
    public void multiSearch(MultiSearchRequest request, ActionListener<MultiSearchResponse> listener) {
        execute(MultiSearchAction.INSTANCE, request, listener);
    }

    @Override
    public MultiSearchRequestBuilder prepareMultiSearch() {
        return new MultiSearchRequestBuilder(this, MultiSearchAction.INSTANCE);
    }

    @Override
    public ActionFuture<TermVectorsResponse> termVectors(final TermVectorsRequest request) {
        return execute(TermVectorsAction.INSTANCE, request);
    }

    @Override
    public void termVectors(final TermVectorsRequest request, final ActionListener<TermVectorsResponse> listener) {
        execute(TermVectorsAction.INSTANCE, request, listener);
    }

    @Override
    public TermVectorsRequestBuilder prepareTermVectors() {
        return new TermVectorsRequestBuilder(this, TermVectorsAction.INSTANCE);
    }

    @Override
    public TermVectorsRequestBuilder prepareTermVectors(String index, String id) {
        return new TermVectorsRequestBuilder(this, TermVectorsAction.INSTANCE, index, id);
    }

    @Override
    public ActionFuture<MultiTermVectorsResponse> multiTermVectors(final MultiTermVectorsRequest request) {
        return execute(MultiTermVectorsAction.INSTANCE, request);
    }

    @Override
    public void multiTermVectors(final MultiTermVectorsRequest request, final ActionListener<MultiTermVectorsResponse> listener) {
        execute(MultiTermVectorsAction.INSTANCE, request, listener);
    }

    @Override
    public MultiTermVectorsRequestBuilder prepareMultiTermVectors() {
        return new MultiTermVectorsRequestBuilder(this, MultiTermVectorsAction.INSTANCE);
    }

    @Override
    public ExplainRequestBuilder prepareExplain(String index, String id) {
        return new ExplainRequestBuilder(this, ExplainAction.INSTANCE, index, id);
    }

    @Override
    public ActionFuture<ExplainResponse> explain(ExplainRequest request) {
        return execute(ExplainAction.INSTANCE, request);
    }

    @Override
    public void explain(ExplainRequest request, ActionListener<ExplainResponse> listener) {
        execute(ExplainAction.INSTANCE, request, listener);
    }

    @Override
    public void clearScroll(ClearScrollRequest request, ActionListener<ClearScrollResponse> listener) {
        execute(ClearScrollAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<ClearScrollResponse> clearScroll(ClearScrollRequest request) {
        return execute(ClearScrollAction.INSTANCE, request);
    }

    @Override
    public ClearScrollRequestBuilder prepareClearScroll() {
        return new ClearScrollRequestBuilder(this, ClearScrollAction.INSTANCE);
    }

    @Override
    public void fieldCaps(FieldCapabilitiesRequest request, ActionListener<FieldCapabilitiesResponse> listener) {
        execute(FieldCapabilitiesAction.INSTANCE, request, listener);
    }

    @Override
    public ActionFuture<FieldCapabilitiesResponse> fieldCaps(FieldCapabilitiesRequest request) {
        return execute(FieldCapabilitiesAction.INSTANCE, request);
    }

    @Override
    public FieldCapabilitiesRequestBuilder prepareFieldCaps(String... indices) {
        return new FieldCapabilitiesRequestBuilder(this, FieldCapabilitiesAction.INSTANCE, indices);
    }

    static class Admin implements AdminClient {

        private final ClusterAdmin clusterAdmin;
        private final IndicesAdmin indicesAdmin;

        Admin(OpenSearchClient client) {
            this.clusterAdmin = new ClusterAdmin(client);
            this.indicesAdmin = new IndicesAdmin(client);
        }

        @Override
        public ClusterAdminClient cluster() {
            return clusterAdmin;
        }

        @Override
        public IndicesAdminClient indices() {
            return indicesAdmin;
        }
    }

    static class ClusterAdmin implements ClusterAdminClient {

        private final OpenSearchClient client;

        ClusterAdmin(OpenSearchClient client) {
            this.client = client;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse> ActionFuture<Response> execute(
            ActionType<Response> action,
            Request request
        ) {
            return client.execute(action, request);
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse> void execute(
            ActionType<Response> action,
            Request request,
            ActionListener<Response> listener
        ) {
            client.execute(action, request, listener);
        }

        @Override
        public ThreadPool threadPool() {
            return client.threadPool();
        }

        @Override
        public ActionFuture<ClusterHealthResponse> health(final ClusterHealthRequest request) {
            return execute(ClusterHealthAction.INSTANCE, request);
        }

        @Override
        public void health(final ClusterHealthRequest request, final ActionListener<ClusterHealthResponse> listener) {
            execute(ClusterHealthAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterHealthRequestBuilder prepareHealth(String... indices) {
            return new ClusterHealthRequestBuilder(this, ClusterHealthAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<ClusterStateResponse> state(final ClusterStateRequest request) {
            return execute(ClusterStateAction.INSTANCE, request);
        }

        @Override
        public void state(final ClusterStateRequest request, final ActionListener<ClusterStateResponse> listener) {
            execute(ClusterStateAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterStateRequestBuilder prepareState() {
            return new ClusterStateRequestBuilder(this, ClusterStateAction.INSTANCE);
        }

        @Override
        public ActionFuture<ClusterRerouteResponse> reroute(final ClusterRerouteRequest request) {
            return execute(ClusterRerouteAction.INSTANCE, request);
        }

        @Override
        public void reroute(final ClusterRerouteRequest request, final ActionListener<ClusterRerouteResponse> listener) {
            execute(ClusterRerouteAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterRerouteRequestBuilder prepareReroute() {
            return new ClusterRerouteRequestBuilder(this, ClusterRerouteAction.INSTANCE);
        }

        @Override
        public ActionFuture<ClusterUpdateSettingsResponse> updateSettings(final ClusterUpdateSettingsRequest request) {
            return execute(ClusterUpdateSettingsAction.INSTANCE, request);
        }

        @Override
        public void updateSettings(
            final ClusterUpdateSettingsRequest request,
            final ActionListener<ClusterUpdateSettingsResponse> listener
        ) {
            execute(ClusterUpdateSettingsAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterUpdateSettingsRequestBuilder prepareUpdateSettings() {
            return new ClusterUpdateSettingsRequestBuilder(this, ClusterUpdateSettingsAction.INSTANCE);
        }

        @Override
        public NodesReloadSecureSettingsRequestBuilder prepareReloadSecureSettings() {
            return new NodesReloadSecureSettingsRequestBuilder(this, NodesReloadSecureSettingsAction.INSTANCE);
        }

        @Override
        public ActionFuture<NodesInfoResponse> nodesInfo(final NodesInfoRequest request) {
            return execute(NodesInfoAction.INSTANCE, request);
        }

        @Override
        public void nodesInfo(final NodesInfoRequest request, final ActionListener<NodesInfoResponse> listener) {
            execute(NodesInfoAction.INSTANCE, request, listener);
        }

        @Override
        public NodesInfoRequestBuilder prepareNodesInfo(String... nodesIds) {
            return new NodesInfoRequestBuilder(this, NodesInfoAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<NodesStatsResponse> nodesStats(final NodesStatsRequest request) {
            return execute(NodesStatsAction.INSTANCE, request);
        }

        @Override
        public void nodesStats(final NodesStatsRequest request, final ActionListener<NodesStatsResponse> listener) {
            execute(NodesStatsAction.INSTANCE, request, listener);
        }

        @Override
        public NodesStatsRequestBuilder prepareNodesStats(String... nodesIds) {
            return new NodesStatsRequestBuilder(this, NodesStatsAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<NodesUsageResponse> nodesUsage(final NodesUsageRequest request) {
            return execute(NodesUsageAction.INSTANCE, request);
        }

        @Override
        public void nodesUsage(final NodesUsageRequest request, final ActionListener<NodesUsageResponse> listener) {
            execute(NodesUsageAction.INSTANCE, request, listener);
        }

        @Override
        public NodesUsageRequestBuilder prepareNodesUsage(String... nodesIds) {
            return new NodesUsageRequestBuilder(this, NodesUsageAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<ClusterStatsResponse> clusterStats(ClusterStatsRequest request) {
            return execute(ClusterStatsAction.INSTANCE, request);
        }

        @Override
        public void clusterStats(ClusterStatsRequest request, ActionListener<ClusterStatsResponse> listener) {
            execute(ClusterStatsAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterStatsRequestBuilder prepareClusterStats() {
            return new ClusterStatsRequestBuilder(this, ClusterStatsAction.INSTANCE);
        }

        @Override
        public ActionFuture<NodesHotThreadsResponse> nodesHotThreads(NodesHotThreadsRequest request) {
            return execute(NodesHotThreadsAction.INSTANCE, request);
        }

        @Override
        public void nodesHotThreads(NodesHotThreadsRequest request, ActionListener<NodesHotThreadsResponse> listener) {
            execute(NodesHotThreadsAction.INSTANCE, request, listener);
        }

        @Override
        public NodesHotThreadsRequestBuilder prepareNodesHotThreads(String... nodesIds) {
            return new NodesHotThreadsRequestBuilder(this, NodesHotThreadsAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<ListTasksResponse> listTasks(final ListTasksRequest request) {
            return execute(ListTasksAction.INSTANCE, request);
        }

        @Override
        public void listTasks(final ListTasksRequest request, final ActionListener<ListTasksResponse> listener) {
            execute(ListTasksAction.INSTANCE, request, listener);
        }

        @Override
        public ListTasksRequestBuilder prepareListTasks(String... nodesIds) {
            return new ListTasksRequestBuilder(this, ListTasksAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<GetTaskResponse> getTask(final GetTaskRequest request) {
            return execute(GetTaskAction.INSTANCE, request);
        }

        @Override
        public void getTask(final GetTaskRequest request, final ActionListener<GetTaskResponse> listener) {
            execute(GetTaskAction.INSTANCE, request, listener);
        }

        @Override
        public GetTaskRequestBuilder prepareGetTask(String taskId) {
            return prepareGetTask(new TaskId(taskId));
        }

        @Override
        public GetTaskRequestBuilder prepareGetTask(TaskId taskId) {
            return new GetTaskRequestBuilder(this, GetTaskAction.INSTANCE).setTaskId(taskId);
        }

        @Override
        public ActionFuture<CancelTasksResponse> cancelTasks(CancelTasksRequest request) {
            return execute(CancelTasksAction.INSTANCE, request);
        }

        @Override
        public void cancelTasks(CancelTasksRequest request, ActionListener<CancelTasksResponse> listener) {
            execute(CancelTasksAction.INSTANCE, request, listener);
        }

        @Override
        public CancelTasksRequestBuilder prepareCancelTasks(String... nodesIds) {
            return new CancelTasksRequestBuilder(this, CancelTasksAction.INSTANCE).setNodesIds(nodesIds);
        }

        @Override
        public ActionFuture<ClusterSearchShardsResponse> searchShards(final ClusterSearchShardsRequest request) {
            return execute(ClusterSearchShardsAction.INSTANCE, request);
        }

        @Override
        public void searchShards(final ClusterSearchShardsRequest request, final ActionListener<ClusterSearchShardsResponse> listener) {
            execute(ClusterSearchShardsAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterSearchShardsRequestBuilder prepareSearchShards() {
            return new ClusterSearchShardsRequestBuilder(this, ClusterSearchShardsAction.INSTANCE);
        }

        @Override
        public ClusterSearchShardsRequestBuilder prepareSearchShards(String... indices) {
            return new ClusterSearchShardsRequestBuilder(this, ClusterSearchShardsAction.INSTANCE).setIndices(indices);
        }

        @Override
        public PendingClusterTasksRequestBuilder preparePendingClusterTasks() {
            return new PendingClusterTasksRequestBuilder(this, PendingClusterTasksAction.INSTANCE);
        }

        @Override
        public ActionFuture<PendingClusterTasksResponse> pendingClusterTasks(PendingClusterTasksRequest request) {
            return execute(PendingClusterTasksAction.INSTANCE, request);
        }

        @Override
        public void pendingClusterTasks(PendingClusterTasksRequest request, ActionListener<PendingClusterTasksResponse> listener) {
            execute(PendingClusterTasksAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> putRepository(PutRepositoryRequest request) {
            return execute(PutRepositoryAction.INSTANCE, request);
        }

        @Override
        public void putRepository(PutRepositoryRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(PutRepositoryAction.INSTANCE, request, listener);
        }

        @Override
        public PutRepositoryRequestBuilder preparePutRepository(String name) {
            return new PutRepositoryRequestBuilder(this, PutRepositoryAction.INSTANCE, name);
        }

        @Override
        public ActionFuture<CreateSnapshotResponse> createSnapshot(CreateSnapshotRequest request) {
            return execute(CreateSnapshotAction.INSTANCE, request);
        }

        @Override
        public void createSnapshot(CreateSnapshotRequest request, ActionListener<CreateSnapshotResponse> listener) {
            execute(CreateSnapshotAction.INSTANCE, request, listener);
        }

        @Override
        public CreateSnapshotRequestBuilder prepareCreateSnapshot(String repository, String name) {
            return new CreateSnapshotRequestBuilder(this, CreateSnapshotAction.INSTANCE, repository, name);
        }

        @Override
        public CloneSnapshotRequestBuilder prepareCloneSnapshot(String repository, String source, String target) {
            return new CloneSnapshotRequestBuilder(this, CloneSnapshotAction.INSTANCE, repository, source, target);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> cloneSnapshot(CloneSnapshotRequest request) {
            return execute(CloneSnapshotAction.INSTANCE, request);
        }

        @Override
        public void cloneSnapshot(CloneSnapshotRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(CloneSnapshotAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<GetSnapshotsResponse> getSnapshots(GetSnapshotsRequest request) {
            return execute(GetSnapshotsAction.INSTANCE, request);
        }

        @Override
        public void getSnapshots(GetSnapshotsRequest request, ActionListener<GetSnapshotsResponse> listener) {
            execute(GetSnapshotsAction.INSTANCE, request, listener);
        }

        @Override
        public GetSnapshotsRequestBuilder prepareGetSnapshots(String repository) {
            return new GetSnapshotsRequestBuilder(this, GetSnapshotsAction.INSTANCE, repository);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteSnapshot(DeleteSnapshotRequest request) {
            return execute(DeleteSnapshotAction.INSTANCE, request);
        }

        @Override
        public void deleteSnapshot(DeleteSnapshotRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteSnapshotAction.INSTANCE, request, listener);
        }

        @Override
        public DeleteSnapshotRequestBuilder prepareDeleteSnapshot(String repository, String... names) {
            return new DeleteSnapshotRequestBuilder(this, DeleteSnapshotAction.INSTANCE, repository, names);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteRepository(DeleteRepositoryRequest request) {
            return execute(DeleteRepositoryAction.INSTANCE, request);
        }

        @Override
        public void deleteRepository(DeleteRepositoryRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteRepositoryAction.INSTANCE, request, listener);
        }

        @Override
        public DeleteRepositoryRequestBuilder prepareDeleteRepository(String name) {
            return new DeleteRepositoryRequestBuilder(this, DeleteRepositoryAction.INSTANCE, name);
        }

        @Override
        public ActionFuture<VerifyRepositoryResponse> verifyRepository(VerifyRepositoryRequest request) {
            return execute(VerifyRepositoryAction.INSTANCE, request);
        }

        @Override
        public void verifyRepository(VerifyRepositoryRequest request, ActionListener<VerifyRepositoryResponse> listener) {
            execute(VerifyRepositoryAction.INSTANCE, request, listener);
        }

        @Override
        public VerifyRepositoryRequestBuilder prepareVerifyRepository(String name) {
            return new VerifyRepositoryRequestBuilder(this, VerifyRepositoryAction.INSTANCE, name);
        }

        @Override
        public ActionFuture<GetRepositoriesResponse> getRepositories(GetRepositoriesRequest request) {
            return execute(GetRepositoriesAction.INSTANCE, request);
        }

        @Override
        public void getRepositories(GetRepositoriesRequest request, ActionListener<GetRepositoriesResponse> listener) {
            execute(GetRepositoriesAction.INSTANCE, request, listener);
        }

        @Override
        public GetRepositoriesRequestBuilder prepareGetRepositories(String... name) {
            return new GetRepositoriesRequestBuilder(this, GetRepositoriesAction.INSTANCE, name);
        }

        @Override
        public CleanupRepositoryRequestBuilder prepareCleanupRepository(String repository) {
            return new CleanupRepositoryRequestBuilder(this, CleanupRepositoryAction.INSTANCE, repository);
        }

        @Override
        public ActionFuture<CleanupRepositoryResponse> cleanupRepository(CleanupRepositoryRequest request) {
            return execute(CleanupRepositoryAction.INSTANCE, request);
        }

        @Override
        public void cleanupRepository(CleanupRepositoryRequest request, ActionListener<CleanupRepositoryResponse> listener) {
            execute(CleanupRepositoryAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<RestoreSnapshotResponse> restoreSnapshot(RestoreSnapshotRequest request) {
            return execute(RestoreSnapshotAction.INSTANCE, request);
        }

        @Override
        public void restoreSnapshot(RestoreSnapshotRequest request, ActionListener<RestoreSnapshotResponse> listener) {
            execute(RestoreSnapshotAction.INSTANCE, request, listener);
        }

        @Override
        public void restoreRemoteStore(RestoreRemoteStoreRequest request, ActionListener<RestoreRemoteStoreResponse> listener) {
            execute(RestoreRemoteStoreAction.INSTANCE, request, listener);
        }

        @Override
        public RestoreSnapshotRequestBuilder prepareRestoreSnapshot(String repository, String snapshot) {
            return new RestoreSnapshotRequestBuilder(this, RestoreSnapshotAction.INSTANCE, repository, snapshot);
        }

        @Override
        public ActionFuture<SnapshotsStatusResponse> snapshotsStatus(SnapshotsStatusRequest request) {
            return execute(SnapshotsStatusAction.INSTANCE, request);
        }

        @Override
        public void snapshotsStatus(SnapshotsStatusRequest request, ActionListener<SnapshotsStatusResponse> listener) {
            execute(SnapshotsStatusAction.INSTANCE, request, listener);
        }

        @Override
        public SnapshotsStatusRequestBuilder prepareSnapshotStatus(String repository) {
            return new SnapshotsStatusRequestBuilder(this, SnapshotsStatusAction.INSTANCE, repository);
        }

        @Override
        public SnapshotsStatusRequestBuilder prepareSnapshotStatus() {
            return new SnapshotsStatusRequestBuilder(this, SnapshotsStatusAction.INSTANCE);
        }

        @Override
        public void putPipeline(PutPipelineRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(PutPipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> putPipeline(PutPipelineRequest request) {
            return execute(PutPipelineAction.INSTANCE, request);
        }

        @Override
        public PutPipelineRequestBuilder preparePutPipeline(String id, BytesReference source, XContentType xContentType) {
            return new PutPipelineRequestBuilder(this, PutPipelineAction.INSTANCE, id, source, xContentType);
        }

        @Override
        public void deletePipeline(DeletePipelineRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeletePipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deletePipeline(DeletePipelineRequest request) {
            return execute(DeletePipelineAction.INSTANCE, request);
        }

        @Override
        public DeletePipelineRequestBuilder prepareDeletePipeline() {
            return new DeletePipelineRequestBuilder(this, DeletePipelineAction.INSTANCE);
        }

        @Override
        public DeletePipelineRequestBuilder prepareDeletePipeline(String id) {
            return new DeletePipelineRequestBuilder(this, DeletePipelineAction.INSTANCE, id);
        }

        @Override
        public void getPipeline(GetPipelineRequest request, ActionListener<GetPipelineResponse> listener) {
            execute(GetPipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<GetPipelineResponse> getPipeline(GetPipelineRequest request) {
            return execute(GetPipelineAction.INSTANCE, request);
        }

        @Override
        public GetPipelineRequestBuilder prepareGetPipeline(String... ids) {
            return new GetPipelineRequestBuilder(this, GetPipelineAction.INSTANCE, ids);
        }

        @Override
        public void simulatePipeline(SimulatePipelineRequest request, ActionListener<SimulatePipelineResponse> listener) {
            execute(SimulatePipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<SimulatePipelineResponse> simulatePipeline(SimulatePipelineRequest request) {
            return execute(SimulatePipelineAction.INSTANCE, request);
        }

        @Override
        public SimulatePipelineRequestBuilder prepareSimulatePipeline(BytesReference source, XContentType xContentType) {
            return new SimulatePipelineRequestBuilder(this, SimulatePipelineAction.INSTANCE, source, xContentType);
        }

        @Override
        public void allocationExplain(ClusterAllocationExplainRequest request, ActionListener<ClusterAllocationExplainResponse> listener) {
            execute(ClusterAllocationExplainAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<ClusterAllocationExplainResponse> allocationExplain(ClusterAllocationExplainRequest request) {
            return execute(ClusterAllocationExplainAction.INSTANCE, request);
        }

        @Override
        public ClusterAllocationExplainRequestBuilder prepareAllocationExplain() {
            return new ClusterAllocationExplainRequestBuilder(this, ClusterAllocationExplainAction.INSTANCE);
        }

        @Override
        public ActionFuture<GetStoredScriptResponse> getStoredScript(final GetStoredScriptRequest request) {
            return execute(GetStoredScriptAction.INSTANCE, request);
        }

        @Override
        public void getStoredScript(final GetStoredScriptRequest request, final ActionListener<GetStoredScriptResponse> listener) {
            execute(GetStoredScriptAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<ListDanglingIndicesResponse> listDanglingIndices(ListDanglingIndicesRequest request) {
            return execute(ListDanglingIndicesAction.INSTANCE, request);
        }

        @Override
        public void listDanglingIndices(ListDanglingIndicesRequest request, ActionListener<ListDanglingIndicesResponse> listener) {
            execute(ListDanglingIndicesAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> importDanglingIndex(ImportDanglingIndexRequest request) {
            return execute(ImportDanglingIndexAction.INSTANCE, request);
        }

        @Override
        public void importDanglingIndex(ImportDanglingIndexRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(ImportDanglingIndexAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteDanglingIndex(DeleteDanglingIndexRequest request) {
            return execute(DeleteDanglingIndexAction.INSTANCE, request);
        }

        @Override
        public ActionFuture<ClusterPutWeightedRoutingResponse> putWeightedRouting(ClusterPutWeightedRoutingRequest request) {
            return execute(ClusterAddWeightedRoutingAction.INSTANCE, request);
        }

        @Override
        public void putWeightedRouting(
            ClusterPutWeightedRoutingRequest request,
            ActionListener<ClusterPutWeightedRoutingResponse> listener
        ) {
            execute(ClusterAddWeightedRoutingAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterPutWeightedRoutingRequestBuilder prepareWeightedRouting() {
            return new ClusterPutWeightedRoutingRequestBuilder(this, ClusterAddWeightedRoutingAction.INSTANCE);
        }

        @Override
        public ActionFuture<ClusterGetWeightedRoutingResponse> getWeightedRouting(ClusterGetWeightedRoutingRequest request) {
            return execute(ClusterGetWeightedRoutingAction.INSTANCE, request);
        }

        @Override
        public void getWeightedRouting(
            ClusterGetWeightedRoutingRequest request,
            ActionListener<ClusterGetWeightedRoutingResponse> listener
        ) {
            execute(ClusterGetWeightedRoutingAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterGetWeightedRoutingRequestBuilder prepareGetWeightedRouting() {
            return new ClusterGetWeightedRoutingRequestBuilder(this, ClusterGetWeightedRoutingAction.INSTANCE);
        }

        @Override
        public ActionFuture<ClusterDeleteWeightedRoutingResponse> deleteWeightedRouting(ClusterDeleteWeightedRoutingRequest request) {
            return execute(ClusterDeleteWeightedRoutingAction.INSTANCE, request);
        }

        @Override
        public void deleteWeightedRouting(
            ClusterDeleteWeightedRoutingRequest request,
            ActionListener<ClusterDeleteWeightedRoutingResponse> listener
        ) {
            execute(ClusterDeleteWeightedRoutingAction.INSTANCE, request, listener);
        }

        @Override
        public ClusterDeleteWeightedRoutingRequestBuilder prepareDeleteWeightedRouting() {
            return new ClusterDeleteWeightedRoutingRequestBuilder(this, ClusterDeleteWeightedRoutingAction.INSTANCE);
        }

        @Override
        public void deleteDanglingIndex(DeleteDanglingIndexRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteDanglingIndexAction.INSTANCE, request, listener);
        }

        @Override
        public GetStoredScriptRequestBuilder prepareGetStoredScript() {
            return new GetStoredScriptRequestBuilder(this, GetStoredScriptAction.INSTANCE);
        }

        @Override
        public GetStoredScriptRequestBuilder prepareGetStoredScript(String id) {
            return prepareGetStoredScript().setId(id);
        }

        @Override
        public PutStoredScriptRequestBuilder preparePutStoredScript() {
            return new PutStoredScriptRequestBuilder(this, PutStoredScriptAction.INSTANCE);
        }

        @Override
        public void putStoredScript(final PutStoredScriptRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(PutStoredScriptAction.INSTANCE, request, listener);

        }

        @Override
        public ActionFuture<AcknowledgedResponse> putStoredScript(final PutStoredScriptRequest request) {
            return execute(PutStoredScriptAction.INSTANCE, request);
        }

        @Override
        public void deleteStoredScript(DeleteStoredScriptRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteStoredScriptAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteStoredScript(DeleteStoredScriptRequest request) {
            return execute(DeleteStoredScriptAction.INSTANCE, request);
        }

        @Override
        public DeleteStoredScriptRequestBuilder prepareDeleteStoredScript() {
            return new DeleteStoredScriptRequestBuilder(client, DeleteStoredScriptAction.INSTANCE);
        }

        @Override
        public DeleteStoredScriptRequestBuilder prepareDeleteStoredScript(String id) {
            return prepareDeleteStoredScript().setId(id);
        }

        @Override
        public ActionFuture<DecommissionResponse> decommission(DecommissionRequest request) {
            return execute(DecommissionAction.INSTANCE, request);
        }

        @Override
        public void decommission(DecommissionRequest request, ActionListener<DecommissionResponse> listener) {
            execute(DecommissionAction.INSTANCE, request, listener);
        }

        @Override
        public DecommissionRequestBuilder prepareDecommission(DecommissionRequest request) {
            return new DecommissionRequestBuilder(this, DecommissionAction.INSTANCE, request);
        }

        @Override
        public ActionFuture<GetDecommissionStateResponse> getDecommissionState(GetDecommissionStateRequest request) {
            return execute(GetDecommissionStateAction.INSTANCE, request);
        }

        @Override
        public void getDecommissionState(GetDecommissionStateRequest request, ActionListener<GetDecommissionStateResponse> listener) {
            execute(GetDecommissionStateAction.INSTANCE, request, listener);
        }

        @Override
        public GetDecommissionStateRequestBuilder prepareGetDecommissionState() {
            return new GetDecommissionStateRequestBuilder(this, GetDecommissionStateAction.INSTANCE);
        }

        @Override
        public ActionFuture<DeleteDecommissionStateResponse> deleteDecommissionState(DeleteDecommissionStateRequest request) {
            return execute(DeleteDecommissionStateAction.INSTANCE, request);
        }

        @Override
        public void deleteDecommissionState(
            DeleteDecommissionStateRequest request,
            ActionListener<DeleteDecommissionStateResponse> listener
        ) {
            execute(DeleteDecommissionStateAction.INSTANCE, request, listener);
        }

        @Override
        public DeleteDecommissionStateRequestBuilder prepareDeleteDecommissionRequest() {
            return new DeleteDecommissionStateRequestBuilder(this, DeleteDecommissionStateAction.INSTANCE);
        }

        @Override
        public void putSearchPipeline(PutSearchPipelineRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(PutSearchPipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> putSearchPipeline(PutSearchPipelineRequest request) {
            return execute(PutSearchPipelineAction.INSTANCE, request);
        }

        @Override
        public void getSearchPipeline(GetSearchPipelineRequest request, ActionListener<GetSearchPipelineResponse> listener) {
            execute(GetSearchPipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<GetSearchPipelineResponse> getSearchPipeline(GetSearchPipelineRequest request) {
            return execute(GetSearchPipelineAction.INSTANCE, request);
        }

        @Override
        public void deleteSearchPipeline(DeleteSearchPipelineRequest request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteSearchPipelineAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteSearchPipeline(DeleteSearchPipelineRequest request) {
            return execute(DeleteSearchPipelineAction.INSTANCE, request);
        }
    }

    static class IndicesAdmin implements IndicesAdminClient {

        private final OpenSearchClient client;

        IndicesAdmin(OpenSearchClient client) {
            this.client = client;
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse> ActionFuture<Response> execute(
            ActionType<Response> action,
            Request request
        ) {
            return client.execute(action, request);
        }

        @Override
        public <Request extends ActionRequest, Response extends ActionResponse> void execute(
            ActionType<Response> action,
            Request request,
            ActionListener<Response> listener
        ) {
            client.execute(action, request, listener);
        }

        @Override
        public ThreadPool threadPool() {
            return client.threadPool();
        }

        @Override
        public ActionFuture<IndicesExistsResponse> exists(final IndicesExistsRequest request) {
            return execute(IndicesExistsAction.INSTANCE, request);
        }

        @Override
        public void exists(final IndicesExistsRequest request, final ActionListener<IndicesExistsResponse> listener) {
            execute(IndicesExistsAction.INSTANCE, request, listener);
        }

        @Override
        public IndicesExistsRequestBuilder prepareExists(String... indices) {
            return new IndicesExistsRequestBuilder(this, IndicesExistsAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> aliases(final IndicesAliasesRequest request) {
            return execute(IndicesAliasesAction.INSTANCE, request);
        }

        @Override
        public void aliases(final IndicesAliasesRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(IndicesAliasesAction.INSTANCE, request, listener);
        }

        @Override
        public IndicesAliasesRequestBuilder prepareAliases() {
            return new IndicesAliasesRequestBuilder(this, IndicesAliasesAction.INSTANCE);
        }

        @Override
        public ActionFuture<GetAliasesResponse> getAliases(GetAliasesRequest request) {
            return execute(GetAliasesAction.INSTANCE, request);
        }

        @Override
        public void getAliases(GetAliasesRequest request, ActionListener<GetAliasesResponse> listener) {
            execute(GetAliasesAction.INSTANCE, request, listener);
        }

        @Override
        public GetAliasesRequestBuilder prepareGetAliases(String... aliases) {
            return new GetAliasesRequestBuilder(this, GetAliasesAction.INSTANCE, aliases);
        }

        @Override
        public ActionFuture<ClearIndicesCacheResponse> clearCache(final ClearIndicesCacheRequest request) {
            return execute(ClearIndicesCacheAction.INSTANCE, request);
        }

        @Override
        public ActionFuture<GetIndexResponse> getIndex(GetIndexRequest request) {
            return execute(GetIndexAction.INSTANCE, request);
        }

        @Override
        public void getIndex(GetIndexRequest request, ActionListener<GetIndexResponse> listener) {
            execute(GetIndexAction.INSTANCE, request, listener);
        }

        @Override
        public GetIndexRequestBuilder prepareGetIndex() {
            return new GetIndexRequestBuilder(this, GetIndexAction.INSTANCE);
        }

        @Override
        public void clearCache(final ClearIndicesCacheRequest request, final ActionListener<ClearIndicesCacheResponse> listener) {
            execute(ClearIndicesCacheAction.INSTANCE, request, listener);
        }

        @Override
        public ClearIndicesCacheRequestBuilder prepareClearCache(String... indices) {
            return new ClearIndicesCacheRequestBuilder(this, ClearIndicesCacheAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<CreateIndexResponse> create(final CreateIndexRequest request) {
            return execute(CreateIndexAction.INSTANCE, request);
        }

        @Override
        public void create(final CreateIndexRequest request, final ActionListener<CreateIndexResponse> listener) {
            execute(CreateIndexAction.INSTANCE, request, listener);
        }

        @Override
        public CreateIndexRequestBuilder prepareCreate(String index) {
            return new CreateIndexRequestBuilder(this, CreateIndexAction.INSTANCE, index);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> delete(final DeleteIndexRequest request) {
            return execute(DeleteIndexAction.INSTANCE, request);
        }

        @Override
        public void delete(final DeleteIndexRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteIndexAction.INSTANCE, request, listener);
        }

        @Override
        public DeleteIndexRequestBuilder prepareDelete(String... indices) {
            return new DeleteIndexRequestBuilder(this, DeleteIndexAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<CloseIndexResponse> close(final CloseIndexRequest request) {
            return execute(CloseIndexAction.INSTANCE, request);
        }

        @Override
        public void close(final CloseIndexRequest request, final ActionListener<CloseIndexResponse> listener) {
            execute(CloseIndexAction.INSTANCE, request, listener);
        }

        @Override
        public CloseIndexRequestBuilder prepareClose(String... indices) {
            return new CloseIndexRequestBuilder(this, CloseIndexAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<OpenIndexResponse> open(final OpenIndexRequest request) {
            return execute(OpenIndexAction.INSTANCE, request);
        }

        @Override
        public void open(final OpenIndexRequest request, final ActionListener<OpenIndexResponse> listener) {
            execute(OpenIndexAction.INSTANCE, request, listener);
        }

        @Override
        public AddIndexBlockRequestBuilder prepareAddBlock(APIBlock block, String... indices) {
            return new AddIndexBlockRequestBuilder(this, AddIndexBlockAction.INSTANCE, block, indices);
        }

        @Override
        public void addBlock(AddIndexBlockRequest request, ActionListener<AddIndexBlockResponse> listener) {
            execute(AddIndexBlockAction.INSTANCE, request, listener);
        }

        @Override
        public OpenIndexRequestBuilder prepareOpen(String... indices) {
            return new OpenIndexRequestBuilder(this, OpenIndexAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<FlushResponse> flush(final FlushRequest request) {
            return execute(FlushAction.INSTANCE, request);
        }

        @Override
        public void flush(final FlushRequest request, final ActionListener<FlushResponse> listener) {
            execute(FlushAction.INSTANCE, request, listener);
        }

        @Override
        public FlushRequestBuilder prepareFlush(String... indices) {
            return new FlushRequestBuilder(this, FlushAction.INSTANCE).setIndices(indices);
        }

        @Override
        public void getMappings(GetMappingsRequest request, ActionListener<GetMappingsResponse> listener) {
            execute(GetMappingsAction.INSTANCE, request, listener);
        }

        @Override
        public void getFieldMappings(GetFieldMappingsRequest request, ActionListener<GetFieldMappingsResponse> listener) {
            execute(GetFieldMappingsAction.INSTANCE, request, listener);
        }

        @Override
        public GetMappingsRequestBuilder prepareGetMappings(String... indices) {
            return new GetMappingsRequestBuilder(this, GetMappingsAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<GetMappingsResponse> getMappings(GetMappingsRequest request) {
            return execute(GetMappingsAction.INSTANCE, request);
        }

        @Override
        public GetFieldMappingsRequestBuilder prepareGetFieldMappings(String... indices) {
            return new GetFieldMappingsRequestBuilder(this, GetFieldMappingsAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<GetFieldMappingsResponse> getFieldMappings(GetFieldMappingsRequest request) {
            return execute(GetFieldMappingsAction.INSTANCE, request);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> putMapping(final PutMappingRequest request) {
            return execute(PutMappingAction.INSTANCE, request);
        }

        @Override
        public void putMapping(final PutMappingRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(PutMappingAction.INSTANCE, request, listener);
        }

        @Override
        public PutMappingRequestBuilder preparePutMapping(String... indices) {
            return new PutMappingRequestBuilder(this, PutMappingAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<ForceMergeResponse> forceMerge(final ForceMergeRequest request) {
            return execute(ForceMergeAction.INSTANCE, request);
        }

        @Override
        public void forceMerge(final ForceMergeRequest request, final ActionListener<ForceMergeResponse> listener) {
            execute(ForceMergeAction.INSTANCE, request, listener);
        }

        @Override
        public ForceMergeRequestBuilder prepareForceMerge(String... indices) {
            return new ForceMergeRequestBuilder(this, ForceMergeAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<UpgradeResponse> upgrade(final UpgradeRequest request) {
            return execute(UpgradeAction.INSTANCE, request);
        }

        @Override
        public void upgrade(final UpgradeRequest request, final ActionListener<UpgradeResponse> listener) {
            execute(UpgradeAction.INSTANCE, request, listener);
        }

        @Override
        public UpgradeRequestBuilder prepareUpgrade(String... indices) {
            return new UpgradeRequestBuilder(this, UpgradeAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<UpgradeStatusResponse> upgradeStatus(final UpgradeStatusRequest request) {
            return execute(UpgradeStatusAction.INSTANCE, request);
        }

        @Override
        public void upgradeStatus(final UpgradeStatusRequest request, final ActionListener<UpgradeStatusResponse> listener) {
            execute(UpgradeStatusAction.INSTANCE, request, listener);
        }

        @Override
        public UpgradeStatusRequestBuilder prepareUpgradeStatus(String... indices) {
            return new UpgradeStatusRequestBuilder(this, UpgradeStatusAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<RefreshResponse> refresh(final RefreshRequest request) {
            return execute(RefreshAction.INSTANCE, request);
        }

        @Override
        public void refresh(final RefreshRequest request, final ActionListener<RefreshResponse> listener) {
            execute(RefreshAction.INSTANCE, request, listener);
        }

        @Override
        public RefreshRequestBuilder prepareRefresh(String... indices) {
            return new RefreshRequestBuilder(this, RefreshAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<IndicesStatsResponse> stats(final IndicesStatsRequest request) {
            return execute(IndicesStatsAction.INSTANCE, request);
        }

        @Override
        public void stats(final IndicesStatsRequest request, final ActionListener<IndicesStatsResponse> listener) {
            execute(IndicesStatsAction.INSTANCE, request, listener);
        }

        @Override
        public IndicesStatsRequestBuilder prepareStats(String... indices) {
            return new IndicesStatsRequestBuilder(this, IndicesStatsAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<RecoveryResponse> recoveries(final RecoveryRequest request) {
            return execute(RecoveryAction.INSTANCE, request);
        }

        @Override
        public void recoveries(final RecoveryRequest request, final ActionListener<RecoveryResponse> listener) {
            execute(RecoveryAction.INSTANCE, request, listener);
        }

        @Override
        public RecoveryRequestBuilder prepareRecoveries(String... indices) {
            return new RecoveryRequestBuilder(this, RecoveryAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<SegmentReplicationStatsResponse> segmentReplicationStats(final SegmentReplicationStatsRequest request) {
            return execute(SegmentReplicationStatsAction.INSTANCE, request);
        }

        @Override
        public void segmentReplicationStats(
            final SegmentReplicationStatsRequest request,
            final ActionListener<SegmentReplicationStatsResponse> listener
        ) {
            execute(SegmentReplicationStatsAction.INSTANCE, request, listener);
        }

        @Override
        public SegmentReplicationStatsRequestBuilder prepareSegmentReplicationStats(String... indices) {
            return new SegmentReplicationStatsRequestBuilder(this, SegmentReplicationStatsAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<IndicesSegmentResponse> segments(final IndicesSegmentsRequest request) {
            return execute(IndicesSegmentsAction.INSTANCE, request);
        }

        @Override
        public void segments(final IndicesSegmentsRequest request, final ActionListener<IndicesSegmentResponse> listener) {
            execute(IndicesSegmentsAction.INSTANCE, request, listener);
        }

        @Override
        public IndicesSegmentsRequestBuilder prepareSegments(String... indices) {
            return new IndicesSegmentsRequestBuilder(this, IndicesSegmentsAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<IndicesShardStoresResponse> shardStores(IndicesShardStoresRequest request) {
            return execute(IndicesShardStoresAction.INSTANCE, request);
        }

        @Override
        public void shardStores(IndicesShardStoresRequest request, ActionListener<IndicesShardStoresResponse> listener) {
            execute(IndicesShardStoresAction.INSTANCE, request, listener);
        }

        @Override
        public IndicesShardStoreRequestBuilder prepareShardStores(String... indices) {
            return new IndicesShardStoreRequestBuilder(this, IndicesShardStoresAction.INSTANCE, indices);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> updateSettings(final UpdateSettingsRequest request) {
            return execute(UpdateSettingsAction.INSTANCE, request);
        }

        @Override
        public void updateSettings(final UpdateSettingsRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(UpdateSettingsAction.INSTANCE, request, listener);
        }

        @Override
        public UpdateSettingsRequestBuilder prepareUpdateSettings(String... indices) {
            return new UpdateSettingsRequestBuilder(this, UpdateSettingsAction.INSTANCE).setIndices(indices);
        }

        @Override
        public ActionFuture<AnalyzeAction.Response> analyze(final AnalyzeAction.Request request) {
            return execute(AnalyzeAction.INSTANCE, request);
        }

        @Override
        public void analyze(final AnalyzeAction.Request request, final ActionListener<AnalyzeAction.Response> listener) {
            execute(AnalyzeAction.INSTANCE, request, listener);
        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze(@Nullable String index, String text) {
            return new AnalyzeRequestBuilder(this, AnalyzeAction.INSTANCE, index, text);
        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze(String text) {
            return new AnalyzeRequestBuilder(this, AnalyzeAction.INSTANCE, null, text);
        }

        @Override
        public AnalyzeRequestBuilder prepareAnalyze() {
            return new AnalyzeRequestBuilder(this, AnalyzeAction.INSTANCE);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> putTemplate(final PutIndexTemplateRequest request) {
            return execute(PutIndexTemplateAction.INSTANCE, request);
        }

        @Override
        public void putTemplate(final PutIndexTemplateRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(PutIndexTemplateAction.INSTANCE, request, listener);
        }

        @Override
        public PutIndexTemplateRequestBuilder preparePutTemplate(String name) {
            return new PutIndexTemplateRequestBuilder(this, PutIndexTemplateAction.INSTANCE, name);
        }

        @Override
        public ActionFuture<GetIndexTemplatesResponse> getTemplates(final GetIndexTemplatesRequest request) {
            return execute(GetIndexTemplatesAction.INSTANCE, request);
        }

        @Override
        public void getTemplates(final GetIndexTemplatesRequest request, final ActionListener<GetIndexTemplatesResponse> listener) {
            execute(GetIndexTemplatesAction.INSTANCE, request, listener);
        }

        @Override
        public GetIndexTemplatesRequestBuilder prepareGetTemplates(String... names) {
            return new GetIndexTemplatesRequestBuilder(this, GetIndexTemplatesAction.INSTANCE, names);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteTemplate(final DeleteIndexTemplateRequest request) {
            return execute(DeleteIndexTemplateAction.INSTANCE, request);
        }

        @Override
        public void deleteTemplate(final DeleteIndexTemplateRequest request, final ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteIndexTemplateAction.INSTANCE, request, listener);
        }

        @Override
        public DeleteIndexTemplateRequestBuilder prepareDeleteTemplate(String name) {
            return new DeleteIndexTemplateRequestBuilder(this, DeleteIndexTemplateAction.INSTANCE, name);
        }

        @Override
        public ActionFuture<ValidateQueryResponse> validateQuery(final ValidateQueryRequest request) {
            return execute(ValidateQueryAction.INSTANCE, request);
        }

        @Override
        public void validateQuery(final ValidateQueryRequest request, final ActionListener<ValidateQueryResponse> listener) {
            execute(ValidateQueryAction.INSTANCE, request, listener);
        }

        @Override
        public ValidateQueryRequestBuilder prepareValidateQuery(String... indices) {
            return new ValidateQueryRequestBuilder(this, ValidateQueryAction.INSTANCE).setIndices(indices);
        }

        @Override
        public GetSettingsRequestBuilder prepareGetSettings(String... indices) {
            return new GetSettingsRequestBuilder(this, GetSettingsAction.INSTANCE, indices);
        }

        @Override
        public ResizeRequestBuilder prepareResizeIndex(String sourceIndex, String targetIndex) {
            return new ResizeRequestBuilder(this, ResizeAction.INSTANCE).setSourceIndex(sourceIndex)
                .setTargetIndex(new CreateIndexRequest(targetIndex));
        }

        @Override
        public ActionFuture<ResizeResponse> resizeIndex(ResizeRequest request) {
            return execute(ResizeAction.INSTANCE, request);
        }

        @Override
        public void resizeIndex(ResizeRequest request, ActionListener<ResizeResponse> listener) {
            execute(ResizeAction.INSTANCE, request, listener);
        }

        @Override
        public RolloverRequestBuilder prepareRolloverIndex(String alias) {
            return new RolloverRequestBuilder(this, RolloverAction.INSTANCE).setRolloverTarget(alias);
        }

        @Override
        public ActionFuture<RolloverResponse> rolloverIndex(RolloverRequest request) {
            return execute(RolloverAction.INSTANCE, request);
        }

        @Override
        public void rolloverIndex(RolloverRequest request, ActionListener<RolloverResponse> listener) {
            execute(RolloverAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<GetSettingsResponse> getSettings(GetSettingsRequest request) {
            return execute(GetSettingsAction.INSTANCE, request);
        }

        @Override
        public void getSettings(GetSettingsRequest request, ActionListener<GetSettingsResponse> listener) {
            execute(GetSettingsAction.INSTANCE, request, listener);
        }

        @Override
        public void createDataStream(CreateDataStreamAction.Request request, ActionListener<AcknowledgedResponse> listener) {
            execute(CreateDataStreamAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> createDataStream(CreateDataStreamAction.Request request) {
            return execute(CreateDataStreamAction.INSTANCE, request);
        }

        @Override
        public void deleteDataStream(DeleteDataStreamAction.Request request, ActionListener<AcknowledgedResponse> listener) {
            execute(DeleteDataStreamAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<AcknowledgedResponse> deleteDataStream(DeleteDataStreamAction.Request request) {
            return execute(DeleteDataStreamAction.INSTANCE, request);
        }

        @Override
        public void getDataStreams(GetDataStreamAction.Request request, ActionListener<GetDataStreamAction.Response> listener) {
            execute(GetDataStreamAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<GetDataStreamAction.Response> getDataStreams(GetDataStreamAction.Request request) {
            return execute(GetDataStreamAction.INSTANCE, request);
        }

        @Override
        public void resolveIndex(ResolveIndexAction.Request request, ActionListener<ResolveIndexAction.Response> listener) {
            execute(ResolveIndexAction.INSTANCE, request, listener);
        }

        @Override
        public ActionFuture<ResolveIndexAction.Response> resolveIndex(ResolveIndexAction.Request request) {
            return execute(ResolveIndexAction.INSTANCE, request);
        }
    }

    @Override
    public Client filterWithHeader(Map<String, String> headers) {
        return new FilterClient(this) {
            @Override
            protected <Request extends ActionRequest, Response extends ActionResponse> void doExecute(
                ActionType<Response> action,
                Request request,
                ActionListener<Response> listener
            ) {
                ThreadContext threadContext = threadPool().getThreadContext();
                try (ThreadContext.StoredContext ctx = threadContext.stashAndMergeHeaders(headers)) {
                    super.doExecute(action, request, listener);
                }
            }
        };
    }
}
