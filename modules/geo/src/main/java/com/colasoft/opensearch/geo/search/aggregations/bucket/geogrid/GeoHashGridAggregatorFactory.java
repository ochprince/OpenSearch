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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid;

import com.colasoft.opensearch.common.geo.GeoBoundingBox;
import com.colasoft.opensearch.geometry.utils.Geohash;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.aggregations.Aggregator;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.CardinalityUpperBound;
import com.colasoft.opensearch.search.aggregations.InternalAggregation;
import com.colasoft.opensearch.search.aggregations.NonCollectingAggregator;
import com.colasoft.opensearch.search.aggregations.support.CoreValuesSourceType;
import com.colasoft.opensearch.search.aggregations.support.ValuesSource;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceRegistry;
import com.colasoft.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * Aggregation Factory for geohash_grid agg
 *
 * @opensearch.internal
 */
public class GeoHashGridAggregatorFactory extends ValuesSourceAggregatorFactory {

    private final int precision;
    private final int requiredSize;
    private final int shardSize;
    private final GeoBoundingBox geoBoundingBox;

    GeoHashGridAggregatorFactory(
        String name,
        ValuesSourceConfig config,
        int precision,
        int requiredSize,
        int shardSize,
        GeoBoundingBox geoBoundingBox,
        QueryShardContext queryShardContext,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, config, queryShardContext, parent, subFactoriesBuilder, metadata);
        this.precision = precision;
        this.requiredSize = requiredSize;
        this.shardSize = shardSize;
        this.geoBoundingBox = geoBoundingBox;
    }

    @Override
    protected Aggregator createUnmapped(SearchContext searchContext, Aggregator parent, Map<String, Object> metadata) throws IOException {
        final InternalAggregation aggregation = new InternalGeoHashGrid(name, requiredSize, emptyList(), metadata);
        return new NonCollectingAggregator(name, searchContext, parent, factories, metadata) {
            @Override
            public InternalAggregation buildEmptyAggregation() {
                return aggregation;
            }
        };
    }

    @Override
    protected Aggregator doCreateInternal(
        SearchContext searchContext,
        Aggregator parent,
        CardinalityUpperBound cardinality,
        Map<String, Object> metadata
    ) throws IOException {
        return queryShardContext.getValuesSourceRegistry()
            .getAggregator(GeoHashGridAggregationBuilder.REGISTRY_KEY, config)
            .build(
                name,
                factories,
                config.getValuesSource(),
                precision,
                geoBoundingBox,
                requiredSize,
                shardSize,
                searchContext,
                parent,
                cardinality,
                metadata
            );
    }

    static void registerAggregators(ValuesSourceRegistry.Builder builder) {
        builder.register(
            GeoHashGridAggregationBuilder.REGISTRY_KEY,
            CoreValuesSourceType.GEOPOINT,
            (
                name,
                factories,
                valuesSource,
                precision,
                geoBoundingBox,
                requiredSize,
                shardSize,
                aggregationContext,
                parent,
                cardinality,
                metadata) -> {
                CellIdSource cellIdSource = new CellIdSource(
                    (ValuesSource.GeoPoint) valuesSource,
                    precision,
                    geoBoundingBox,
                    Geohash::longEncode
                );
                return new GeoHashGridAggregator(
                    name,
                    factories,
                    cellIdSource,
                    requiredSize,
                    shardSize,
                    aggregationContext,
                    parent,
                    cardinality,
                    metadata
                );
            },
            true
        );
    }
}
