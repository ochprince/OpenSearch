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

package com.colasoft.opensearch.action.admin.indices.get;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.info.TransportClusterInfoAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.metadata.AliasMetadata;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.metadata.MappingMetadata;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.collect.ImmutableOpenMap;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Get index action.
 *
 * @opensearch.internal
 */
public class TransportGetIndexAction extends TransportClusterInfoAction<GetIndexRequest, GetIndexResponse> {

    private final IndicesService indicesService;
    private final IndexScopedSettings indexScopedSettings;
    private final SettingsFilter settingsFilter;

    @Inject
    public TransportGetIndexAction(
        TransportService transportService,
        ClusterService clusterService,
        ThreadPool threadPool,
        SettingsFilter settingsFilter,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        IndicesService indicesService,
        IndexScopedSettings indexScopedSettings
    ) {
        super(
            GetIndexAction.NAME,
            transportService,
            clusterService,
            threadPool,
            actionFilters,
            GetIndexRequest::new,
            indexNameExpressionResolver
        );
        this.indicesService = indicesService;
        this.settingsFilter = settingsFilter;
        this.indexScopedSettings = indexScopedSettings;
    }

    @Override
    protected GetIndexResponse read(StreamInput in) throws IOException {
        return new GetIndexResponse(in);
    }

    @Override
    protected void doClusterManagerOperation(
        final GetIndexRequest request,
        String[] concreteIndices,
        final ClusterState state,
        final ActionListener<GetIndexResponse> listener
    ) {
        ImmutableOpenMap<String, MappingMetadata> mappingsResult = ImmutableOpenMap.of();
        ImmutableOpenMap<String, List<AliasMetadata>> aliasesResult = ImmutableOpenMap.of();
        ImmutableOpenMap<String, Settings> settings = ImmutableOpenMap.of();
        ImmutableOpenMap<String, Settings> defaultSettings = ImmutableOpenMap.of();
        ImmutableOpenMap<String, String> dataStreams = ImmutableOpenMap.<String, String>builder()
            .putAll(
                StreamSupport.stream(state.metadata().findDataStreams(concreteIndices).spliterator(), false)
                    .collect(Collectors.toMap(k -> k.key, v -> v.value.getName()))
            )
            .build();
        GetIndexRequest.Feature[] features = request.features();
        boolean doneAliases = false;
        boolean doneMappings = false;
        boolean doneSettings = false;
        for (GetIndexRequest.Feature feature : features) {
            switch (feature) {
                case MAPPINGS:
                    if (!doneMappings) {
                        try {
                            mappingsResult = state.metadata().findMappings(concreteIndices, indicesService.getFieldFilter());
                            doneMappings = true;
                        } catch (IOException e) {
                            listener.onFailure(e);
                            return;
                        }
                    }
                    break;
                case ALIASES:
                    if (!doneAliases) {
                        aliasesResult = state.metadata().findAllAliases(concreteIndices);
                        doneAliases = true;
                    }
                    break;
                case SETTINGS:
                    if (!doneSettings) {
                        ImmutableOpenMap.Builder<String, Settings> settingsMapBuilder = ImmutableOpenMap.builder();
                        ImmutableOpenMap.Builder<String, Settings> defaultSettingsMapBuilder = ImmutableOpenMap.builder();
                        for (String index : concreteIndices) {
                            Settings indexSettings = state.metadata().index(index).getSettings();
                            if (request.humanReadable()) {
                                indexSettings = IndexMetadata.addHumanReadableSettings(indexSettings);
                            }
                            settingsMapBuilder.put(index, indexSettings);
                            if (request.includeDefaults()) {
                                Settings defaultIndexSettings = settingsFilter.filter(
                                    indexScopedSettings.diff(indexSettings, Settings.EMPTY)
                                );
                                defaultSettingsMapBuilder.put(index, defaultIndexSettings);
                            }
                        }
                        settings = settingsMapBuilder.build();
                        defaultSettings = defaultSettingsMapBuilder.build();
                        doneSettings = true;
                    }
                    break;

                default:
                    throw new IllegalStateException("feature [" + feature + "] is not valid");
            }
        }
        listener.onResponse(new GetIndexResponse(concreteIndices, mappingsResult, aliasesResult, settings, defaultSettings, dataStreams));
    }
}
