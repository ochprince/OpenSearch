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

package com.colasoft.opensearch.client;

import com.colasoft.opensearch.action.ActionFuture;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import com.colasoft.opensearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.alias.get.GetAliasesResponse;
import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeAction;
import com.colasoft.opensearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheResponse;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexRequest;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.close.CloseIndexResponse;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexRequest;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.create.CreateIndexResponse;
import com.colasoft.opensearch.action.admin.indices.datastream.CreateDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.DeleteDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.datastream.GetDataStreamAction;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import com.colasoft.opensearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import com.colasoft.opensearch.action.admin.indices.flush.FlushRequest;
import com.colasoft.opensearch.action.admin.indices.flush.FlushRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.flush.FlushResponse;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeRequest;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.forcemerge.ForceMergeResponse;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexRequest;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.get.GetIndexResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetMappingsResponse;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequest;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexRequest;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.open.OpenIndexResponse;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockRequest;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.readonly.AddIndexBlockResponse;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryRequest;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.recovery.RecoveryResponse;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshRequest;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.refresh.RefreshResponse;
import com.colasoft.opensearch.action.admin.indices.resolve.ResolveIndexAction;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverRequest;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.rollover.RolloverResponse;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsRequest;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.replication.SegmentReplicationStatsResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsRequest;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import com.colasoft.opensearch.action.admin.indices.settings.put.UpdateSettingsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoreRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresRequest;
import com.colasoft.opensearch.action.admin.indices.shards.IndicesShardStoresResponse;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeRequest;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.shrink.ResizeResponse;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsRequest;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.stats.IndicesStatsResponse;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import com.colasoft.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import com.colasoft.opensearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusRequest;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.get.UpgradeStatusResponse;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeRequest;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.upgrade.post.UpgradeResponse;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryRequest;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryRequestBuilder;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryResponse;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata.APIBlock;
import com.colasoft.opensearch.common.Nullable;

/**
 * Administrative actions/operations against indices.
 *
 * @see AdminClient#indices()
 *
 * @opensearch.internal
 */
public interface IndicesAdminClient extends OpenSearchClient {

    /**
     * Indices Exists.
     *
     * @param request The indices exists request
     * @return The result future
     * @see Requests#indicesExistsRequest(String...)
     */
    ActionFuture<IndicesExistsResponse> exists(IndicesExistsRequest request);

    /**
     * The status of one or more indices.
     *
     * @param request  The indices status request
     * @param listener A listener to be notified with a result
     * @see Requests#indicesExistsRequest(String...)
     */
    void exists(IndicesExistsRequest request, ActionListener<IndicesExistsResponse> listener);

    /**
     * Indices exists.
     */
    IndicesExistsRequestBuilder prepareExists(String... indices);

    /**
     * Indices stats.
     */
    ActionFuture<IndicesStatsResponse> stats(IndicesStatsRequest request);

    /**
     * Indices stats.
     */
    void stats(IndicesStatsRequest request, ActionListener<IndicesStatsResponse> listener);

    /**
     * Indices stats.
     */
    IndicesStatsRequestBuilder prepareStats(String... indices);

    /**
     * Indices recoveries
     */
    ActionFuture<RecoveryResponse> recoveries(RecoveryRequest request);

    /**
     *Indices recoveries
     */
    void recoveries(RecoveryRequest request, ActionListener<RecoveryResponse> listener);

    /**
     *Indices segment replication
     */
    ActionFuture<SegmentReplicationStatsResponse> segmentReplicationStats(SegmentReplicationStatsRequest request);

    /**
     *Indices segment replication
     */
    void segmentReplicationStats(SegmentReplicationStatsRequest request, ActionListener<SegmentReplicationStatsResponse> listener);

    /**
     * Indices recoveries
     */
    RecoveryRequestBuilder prepareRecoveries(String... indices);

    /**
     * Indices segment replication
     */
    SegmentReplicationStatsRequestBuilder prepareSegmentReplicationStats(String... indices);

    /**
     * The segments of one or more indices.
     *
     * @param request The indices segments request
     * @return The result future
     * @see Requests#indicesSegmentsRequest(String...)
     */
    ActionFuture<IndicesSegmentResponse> segments(IndicesSegmentsRequest request);

    /**
     * The segments of one or more indices.
     *
     * @param request  The indices segments request
     * @param listener A listener to be notified with a result
     * @see Requests#indicesSegmentsRequest(String...)
     */
    void segments(IndicesSegmentsRequest request, ActionListener<IndicesSegmentResponse> listener);

    /**
     * The segments of one or more indices.
     */
    IndicesSegmentsRequestBuilder prepareSegments(String... indices);

    /**
     * The shard stores info of one or more indices.
     *
     * @param request The indices shard stores request
     * @return The result future
     * @see Requests#indicesShardStoresRequest(String...)
     */
    ActionFuture<IndicesShardStoresResponse> shardStores(IndicesShardStoresRequest request);

    /**
     * The shard stores info of one or more indices.
     *
     * @param request The indices shard stores request
     * @param listener A listener to be notified with a result
     * @see Requests#indicesShardStoresRequest(String...)
     */
    void shardStores(IndicesShardStoresRequest request, ActionListener<IndicesShardStoresResponse> listener);

    /**
     * The shard stores info of one or more indices.
     */
    IndicesShardStoreRequestBuilder prepareShardStores(String... indices);

    /**
     * Creates an index using an explicit request allowing to specify the settings of the index.
     *
     * @param request The create index request
     * @return The result future
     * @see com.colasoft.opensearch.client.Requests#createIndexRequest(String)
     */
    ActionFuture<CreateIndexResponse> create(CreateIndexRequest request);

    /**
     * Creates an index using an explicit request allowing to specify the settings of the index.
     *
     * @param request  The create index request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#createIndexRequest(String)
     */
    void create(CreateIndexRequest request, ActionListener<CreateIndexResponse> listener);

    /**
     * Creates an index using an explicit request allowing to specify the settings of the index.
     *
     * @param index The index name to create
     */
    CreateIndexRequestBuilder prepareCreate(String index);

    /**
     * Deletes an index based on the index name.
     *
     * @param request The delete index request
     * @return The result future
     * @see com.colasoft.opensearch.client.Requests#deleteIndexRequest(String)
     */
    ActionFuture<AcknowledgedResponse> delete(DeleteIndexRequest request);

    /**
     * Deletes an index based on the index name.
     *
     * @param request  The delete index request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#deleteIndexRequest(String)
     */
    void delete(DeleteIndexRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Deletes an index based on the index name.
     *
     * @param indices The indices to delete. Use "_all" to delete all indices.
     */
    DeleteIndexRequestBuilder prepareDelete(String... indices);

    /**
     * Closes an index based on the index name.
     *
     * @param request The close index request
     * @return The result future
     * @see com.colasoft.opensearch.client.Requests#closeIndexRequest(String)
     */
    ActionFuture<CloseIndexResponse> close(CloseIndexRequest request);

    /**
     * Closes an index based on the index name.
     *
     * @param request  The close index request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#closeIndexRequest(String)
     */
    void close(CloseIndexRequest request, ActionListener<CloseIndexResponse> listener);

    /**
     * Closes one or more indices based on their index name.
     *
     * @param indices The name of the indices to close
     */
    CloseIndexRequestBuilder prepareClose(String... indices);

    /**
     * Open an index based on the index name.
     *
     * @param request The close index request
     * @return The result future
     * @see com.colasoft.opensearch.client.Requests#openIndexRequest(String)
     */
    ActionFuture<OpenIndexResponse> open(OpenIndexRequest request);

    /**
     * Open an index based on the index name.
     *
     * @param request  The close index request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#openIndexRequest(String)
     */
    void open(OpenIndexRequest request, ActionListener<OpenIndexResponse> listener);

    /**
     * Adds a block to an index
     *
     * @param block   The block to add
     * @param indices The name of the indices to add the block to
     */
    AddIndexBlockRequestBuilder prepareAddBlock(APIBlock block, String... indices);

    /**
     * Adds a block to an index
     *
     * @param request  The add index block request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#openIndexRequest(String)
     */
    void addBlock(AddIndexBlockRequest request, ActionListener<AddIndexBlockResponse> listener);

    /**
     * Opens one or more indices based on their index name.
     *
     * @param indices The name of the indices to close
     */
    OpenIndexRequestBuilder prepareOpen(String... indices);

    /**
     * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
     *
     * @param request The refresh request
     * @return The result future
     * @see com.colasoft.opensearch.client.Requests#refreshRequest(String...)
     */
    ActionFuture<RefreshResponse> refresh(RefreshRequest request);

    /**
     * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
     *
     * @param request  The refresh request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#refreshRequest(String...)
     */
    void refresh(RefreshRequest request, ActionListener<RefreshResponse> listener);

    /**
     * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
     */
    RefreshRequestBuilder prepareRefresh(String... indices);

    /**
     * Explicitly flush one or more indices (releasing memory from the node).
     *
     * @param request The flush request
     * @return A result future
     * @see com.colasoft.opensearch.client.Requests#flushRequest(String...)
     */
    ActionFuture<FlushResponse> flush(FlushRequest request);

    /**
     * Explicitly flush one or more indices (releasing memory from the node).
     *
     * @param request  The flush request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#flushRequest(String...)
     */
    void flush(FlushRequest request, ActionListener<FlushResponse> listener);

    /**
     * Explicitly flush one or more indices (releasing memory from the node).
     */
    FlushRequestBuilder prepareFlush(String... indices);

    /**
     * Explicitly force merge one or more indices into a the number of segments.
     *
     * @param request The optimize request
     * @return A result future
     * @see com.colasoft.opensearch.client.Requests#forceMergeRequest(String...)
     */
    ActionFuture<ForceMergeResponse> forceMerge(ForceMergeRequest request);

    /**
     * Explicitly force merge one or more indices into a the number of segments.
     *
     * @param request  The force merge request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#forceMergeRequest(String...)
     */
    void forceMerge(ForceMergeRequest request, ActionListener<ForceMergeResponse> listener);

    /**
     * Explicitly force merge one or more indices into a the number of segments.
     */
    ForceMergeRequestBuilder prepareForceMerge(String... indices);

    /**
     * Explicitly upgrade one or more indices
     *
     * @param request The upgrade request
     * @return A result future
     * @see com.colasoft.opensearch.client.Requests#upgradeRequest(String...)
     */
    ActionFuture<UpgradeResponse> upgrade(UpgradeRequest request);

    /**
     * Explicitly upgrade one or more indices
     *
     * @param request  The upgrade request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#upgradeRequest(String...)
     */
    void upgrade(UpgradeRequest request, ActionListener<UpgradeResponse> listener);

    /**
     *  Explicitly upgrade one or more indices
     */
    UpgradeStatusRequestBuilder prepareUpgradeStatus(String... indices);

    /**
     * Check upgrade status of one or more indices
     *
     * @param request The upgrade request
     * @return A result future
     * @see com.colasoft.opensearch.client.Requests#upgradeRequest(String...)
     */
    ActionFuture<UpgradeStatusResponse> upgradeStatus(UpgradeStatusRequest request);

    /**
     * Check upgrade status of one or more indices
     *
     * @param request  The upgrade request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#upgradeRequest(String...)
     */
    void upgradeStatus(UpgradeStatusRequest request, ActionListener<UpgradeStatusResponse> listener);

    /**
     * Check upgrade status of one or more indices
     */
    UpgradeRequestBuilder prepareUpgrade(String... indices);

    /**
     * Get the complete mappings of one or more types
     */
    void getMappings(GetMappingsRequest request, ActionListener<GetMappingsResponse> listener);

    /**
     * Get the complete mappings of one or more types
     */
    ActionFuture<GetMappingsResponse> getMappings(GetMappingsRequest request);

    /**
     * Get the complete mappings of one or more types
     */
    GetMappingsRequestBuilder prepareGetMappings(String... indices);

    /**
     * Get the mappings of specific fields
     */
    void getFieldMappings(GetFieldMappingsRequest request, ActionListener<GetFieldMappingsResponse> listener);

    /**
     * Get the mappings of specific fields
     */
    GetFieldMappingsRequestBuilder prepareGetFieldMappings(String... indices);

    /**
     * Get the mappings of specific fields
     */
    ActionFuture<GetFieldMappingsResponse> getFieldMappings(GetFieldMappingsRequest request);

    /**
     * Add mapping definition for a type into one or more indices.
     *
     * @param request The create mapping request
     * @return A result future
     * @see com.colasoft.opensearch.client.Requests#putMappingRequest(String...)
     */
    ActionFuture<AcknowledgedResponse> putMapping(PutMappingRequest request);

    /**
     * Add mapping definition for a type into one or more indices.
     *
     * @param request  The create mapping request
     * @param listener A listener to be notified with a result
     * @see com.colasoft.opensearch.client.Requests#putMappingRequest(String...)
     */
    void putMapping(PutMappingRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Add mapping definition for a type into one or more indices.
     */
    PutMappingRequestBuilder preparePutMapping(String... indices);

    /**
     * Allows to add/remove aliases from indices.
     *
     * @param request The index aliases request
     * @return The result future
     * @see Requests#indexAliasesRequest()
     */
    ActionFuture<AcknowledgedResponse> aliases(IndicesAliasesRequest request);

    /**
     * Allows to add/remove aliases from indices.
     *
     * @param request  The index aliases request
     * @param listener A listener to be notified with a result
     * @see Requests#indexAliasesRequest()
     */
    void aliases(IndicesAliasesRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Allows to add/remove aliases from indices.
     */
    IndicesAliasesRequestBuilder prepareAliases();

    /**
     * Get specific index aliases that exists in particular indices and / or by name.
     *
     * @param request The result future
     */
    ActionFuture<GetAliasesResponse> getAliases(GetAliasesRequest request);

    /**
     * Get specific index aliases that exists in particular indices and / or by name.
     *
     * @param request  The index aliases request
     * @param listener A listener to be notified with a result
     */
    void getAliases(GetAliasesRequest request, ActionListener<GetAliasesResponse> listener);

    /**
     * Get specific index aliases that exists in particular indices and / or by name.
     */
    GetAliasesRequestBuilder prepareGetAliases(String... aliases);

    /**
     * Get index metadata for particular indices.
     *
     * @param request The result future
     */
    ActionFuture<GetIndexResponse> getIndex(GetIndexRequest request);

    /**
     * Get index metadata for particular indices.
     *
     * @param request  The index aliases request
     * @param listener A listener to be notified with a result
     */
    void getIndex(GetIndexRequest request, ActionListener<GetIndexResponse> listener);

    /**
     * Get index metadata for particular indices.
     */
    GetIndexRequestBuilder prepareGetIndex();

    /**
     * Clear indices cache.
     *
     * @param request The clear indices cache request
     * @return The result future
     * @see Requests#clearIndicesCacheRequest(String...)
     */
    ActionFuture<ClearIndicesCacheResponse> clearCache(ClearIndicesCacheRequest request);

    /**
     * Clear indices cache.
     *
     * @param request  The clear indices cache request
     * @param listener A listener to be notified with a result
     * @see Requests#clearIndicesCacheRequest(String...)
     */
    void clearCache(ClearIndicesCacheRequest request, ActionListener<ClearIndicesCacheResponse> listener);

    /**
     * Clear indices cache.
     */
    ClearIndicesCacheRequestBuilder prepareClearCache(String... indices);

    /**
     * Updates settings of one or more indices.
     *
     * @param request the update settings request
     * @return The result future
     */
    ActionFuture<AcknowledgedResponse> updateSettings(UpdateSettingsRequest request);

    /**
     * Updates settings of one or more indices.
     *
     * @param request  the update settings request
     * @param listener A listener to be notified with the response
     */
    void updateSettings(UpdateSettingsRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Update indices settings.
     */
    UpdateSettingsRequestBuilder prepareUpdateSettings(String... indices);

    /**
     * Analyze text under the provided index.
     */
    ActionFuture<AnalyzeAction.Response> analyze(AnalyzeAction.Request request);

    /**
     * Analyze text under the provided index.
     */
    void analyze(AnalyzeAction.Request request, ActionListener<AnalyzeAction.Response> listener);

    /**
     * Analyze text under the provided index.
     *
     * @param index The index name
     * @param text  The text to analyze
     */
    AnalyzeRequestBuilder prepareAnalyze(@Nullable String index, String text);

    /**
     * Analyze text.
     *
     * @param text The text to analyze
     */
    AnalyzeRequestBuilder prepareAnalyze(String text);

    /**
     * Analyze text/texts.
     *
     */
    AnalyzeRequestBuilder prepareAnalyze();

    /**
     * Puts an index template.
     */
    ActionFuture<AcknowledgedResponse> putTemplate(PutIndexTemplateRequest request);

    /**
     * Puts an index template.
     */
    void putTemplate(PutIndexTemplateRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Puts an index template.
     *
     * @param name The name of the template.
     */
    PutIndexTemplateRequestBuilder preparePutTemplate(String name);

    /**
     * Deletes index template.
     */
    ActionFuture<AcknowledgedResponse> deleteTemplate(DeleteIndexTemplateRequest request);

    /**
     * Deletes an index template.
     */
    void deleteTemplate(DeleteIndexTemplateRequest request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Deletes an index template.
     *
     * @param name The name of the template.
     */
    DeleteIndexTemplateRequestBuilder prepareDeleteTemplate(String name);

    /**
     * Gets index template.
     */
    ActionFuture<GetIndexTemplatesResponse> getTemplates(GetIndexTemplatesRequest request);

    /**
     * Gets an index template.
     */
    void getTemplates(GetIndexTemplatesRequest request, ActionListener<GetIndexTemplatesResponse> listener);

    /**
     * Gets an index template (optional).
     */
    GetIndexTemplatesRequestBuilder prepareGetTemplates(String... name);

    /**
     * Validate a query for correctness.
     *
     * @param request The count request
     * @return The result future
     */
    ActionFuture<ValidateQueryResponse> validateQuery(ValidateQueryRequest request);

    /**
     * Validate a query for correctness.
     *
     * @param request  The count request
     * @param listener A listener to be notified of the result
     */
    void validateQuery(ValidateQueryRequest request, ActionListener<ValidateQueryResponse> listener);

    /**
     * Validate a query for correctness.
     */
    ValidateQueryRequestBuilder prepareValidateQuery(String... indices);

    /**
     * Executed a per index settings get request and returns the settings for the indices specified.
     * Note: this is a per index request and will not include settings that are set on the cluster
     * level. This request is not exhaustive, it will not return default values for setting.
     */
    void getSettings(GetSettingsRequest request, ActionListener<GetSettingsResponse> listener);

    /**
     * Executed a per index settings get request.
     * @see #getSettings(GetSettingsRequest)
     */
    ActionFuture<GetSettingsResponse> getSettings(GetSettingsRequest request);

    /**
     * Returns a builder for a per index settings get request.
     * @param indices the indices to fetch the setting for.
     * @see #getSettings(GetSettingsRequest)
     */
    GetSettingsRequestBuilder prepareGetSettings(String... indices);

    /**
     * Resize an index using an explicit request allowing to specify the settings, mappings and aliases of the target index of the index.
     */
    ResizeRequestBuilder prepareResizeIndex(String sourceIndex, String targetIndex);

    /**
     * Resize an index using an explicit request allowing to specify the settings, mappings and aliases of the target index of the index.
     */
    ActionFuture<ResizeResponse> resizeIndex(ResizeRequest request);

    /**
     * Shrinks an index using an explicit request allowing to specify the settings, mappings and aliases of the target index of the index.
     */
    void resizeIndex(ResizeRequest request, ActionListener<ResizeResponse> listener);

    /**
     * Swaps the index pointed to by an alias given all provided conditions are satisfied
     */
    RolloverRequestBuilder prepareRolloverIndex(String sourceAlias);

    /**
     * Swaps the index pointed to by an alias given all provided conditions are satisfied
     */
    ActionFuture<RolloverResponse> rolloverIndex(RolloverRequest request);

    /**
     * Swaps the index pointed to by an alias given all provided conditions are satisfied
     */
    void rolloverIndex(RolloverRequest request, ActionListener<RolloverResponse> listener);

    /**
     * Store a data stream
     */
    void createDataStream(CreateDataStreamAction.Request request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Store a data stream
     */
    ActionFuture<AcknowledgedResponse> createDataStream(CreateDataStreamAction.Request request);

    /**
     * Delete a data stream
     */
    void deleteDataStream(DeleteDataStreamAction.Request request, ActionListener<AcknowledgedResponse> listener);

    /**
     * Delete a data stream
     */
    ActionFuture<AcknowledgedResponse> deleteDataStream(DeleteDataStreamAction.Request request);

    /**
     * Get data streams
     */
    void getDataStreams(GetDataStreamAction.Request request, ActionListener<GetDataStreamAction.Response> listener);

    /**
     * Get data streams
     */
    ActionFuture<GetDataStreamAction.Response> getDataStreams(GetDataStreamAction.Request request);

    /**
     * Resolves names and wildcard expressions to indices, aliases, and data streams
     */
    void resolveIndex(ResolveIndexAction.Request request, ActionListener<ResolveIndexAction.Response> listener);

    /**
     * Resolves names and wildcard expressions to indices, aliases, and data streams
     */
    ActionFuture<ResolveIndexAction.Response> resolveIndex(ResolveIndexAction.Request request);
}
