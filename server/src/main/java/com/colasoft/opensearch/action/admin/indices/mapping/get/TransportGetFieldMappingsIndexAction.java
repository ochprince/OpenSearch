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

package com.colasoft.opensearch.action.admin.indices.mapping.get;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.admin.indices.mapping.get.GetFieldMappingsResponse.FieldMappingMetadata;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.single.shard.TransportSingleShardAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.routing.ShardsIterator;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.regex.Regex;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.mapper.MappingLookup;
import com.colasoft.opensearch.index.mapper.DocumentMapper;
import com.colasoft.opensearch.index.mapper.Mapper;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.singletonMap;

/**
 * Transport action used to retrieve the mappings related to fields that belong to a specific index
 *
 * @opensearch.internal
 */
public class TransportGetFieldMappingsIndexAction extends TransportSingleShardAction<
    GetFieldMappingsIndexRequest,
    GetFieldMappingsResponse> {

    private static final String ACTION_NAME = GetFieldMappingsAction.NAME + "[index]";

    protected final ClusterService clusterService;
    private final IndicesService indicesService;

    @Inject
    public TransportGetFieldMappingsIndexAction(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver
    ) {
        super(
            ACTION_NAME,
            threadPool,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            GetFieldMappingsIndexRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.clusterService = clusterService;
        this.indicesService = indicesService;
    }

    @Override
    protected boolean resolveIndex(GetFieldMappingsIndexRequest request) {
        // internal action, index already resolved
        return false;
    }

    @Override
    protected ShardsIterator shards(ClusterState state, InternalRequest request) {
        // Will balance requests between shards
        return state.routingTable().index(request.concreteIndex()).randomAllActiveShardsIt();
    }

    @Override
    protected GetFieldMappingsResponse shardOperation(final GetFieldMappingsIndexRequest request, ShardId shardId) {
        assert shardId != null;
        IndexService indexService = indicesService.indexServiceSafe(shardId.getIndex());
        Version indexCreatedVersion = indexService.mapperService().getIndexSettings().getIndexVersionCreated();
        Predicate<String> metadataFieldPredicate = (f) -> indicesService.isMetadataField(indexCreatedVersion, f);
        Predicate<String> fieldPredicate = metadataFieldPredicate.or(indicesService.getFieldFilter().apply(shardId.getIndexName()));

        DocumentMapper documentMapper = indexService.mapperService().documentMapper();
        Map<String, FieldMappingMetadata> fieldMapping = findFieldMappings(fieldPredicate, documentMapper, request);
        return new GetFieldMappingsResponse(singletonMap(shardId.getIndexName(), fieldMapping));
    }

    @Override
    protected Writeable.Reader<GetFieldMappingsResponse> getResponseReader() {
        return GetFieldMappingsResponse::new;
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, InternalRequest request) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.METADATA_READ, request.concreteIndex());
    }

    private static final ToXContent.Params includeDefaultsParams = new ToXContent.Params() {

        static final String INCLUDE_DEFAULTS = "include_defaults";

        @Override
        public String param(String key) {
            if (INCLUDE_DEFAULTS.equals(key)) {
                return "true";
            }
            return null;
        }

        @Override
        public String param(String key, String defaultValue) {
            if (INCLUDE_DEFAULTS.equals(key)) {
                return "true";
            }
            return defaultValue;
        }

        @Override
        public boolean paramAsBoolean(String key, boolean defaultValue) {
            if (INCLUDE_DEFAULTS.equals(key)) {
                return true;
            }
            return defaultValue;
        }

        @Override
        public Boolean paramAsBoolean(String key, Boolean defaultValue) {
            if (INCLUDE_DEFAULTS.equals(key)) {
                return true;
            }
            return defaultValue;
        }
    };

    private static Map<String, FieldMappingMetadata> findFieldMappings(
        Predicate<String> fieldPredicate,
        DocumentMapper documentMapper,
        GetFieldMappingsIndexRequest request
    ) {
        if (documentMapper == null) {
            return Collections.emptyMap();
        }
        Map<String, FieldMappingMetadata> fieldMappings = new HashMap<>();
        final MappingLookup allFieldMappers = documentMapper.mappers();
        for (String field : request.fields()) {
            if (Regex.isMatchAllPattern(field)) {
                for (Mapper fieldMapper : allFieldMappers) {
                    addFieldMapper(fieldPredicate, fieldMapper.name(), fieldMapper, fieldMappings, request.includeDefaults());
                }
            } else if (Regex.isSimpleMatchPattern(field)) {
                for (Mapper fieldMapper : allFieldMappers) {
                    if (Regex.simpleMatch(field, fieldMapper.name())) {
                        addFieldMapper(fieldPredicate, fieldMapper.name(), fieldMapper, fieldMappings, request.includeDefaults());
                    }
                }
            } else {
                // not a pattern
                Mapper fieldMapper = allFieldMappers.getMapper(field);
                if (fieldMapper != null) {
                    addFieldMapper(fieldPredicate, field, fieldMapper, fieldMappings, request.includeDefaults());
                }
            }
        }
        return Collections.unmodifiableMap(fieldMappings);
    }

    private static void addFieldMapper(
        Predicate<String> fieldPredicate,
        String field,
        Mapper fieldMapper,
        Map<String, FieldMappingMetadata> fieldMappings,
        boolean includeDefaults
    ) {
        if (fieldMappings.containsKey(field)) {
            return;
        }
        if (fieldPredicate.test(field)) {
            try {
                BytesReference bytes = XContentHelper.toXContent(
                    fieldMapper,
                    XContentType.JSON,
                    includeDefaults ? includeDefaultsParams : ToXContent.EMPTY_PARAMS,
                    false
                );
                fieldMappings.put(field, new FieldMappingMetadata(fieldMapper.name(), bytes));
            } catch (IOException e) {
                throw new OpenSearchException("failed to serialize XContent of field [" + field + "]", e);
            }
        }
    }
}
