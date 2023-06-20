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

package com.colasoft.opensearch.search.aggregations.bucket.range;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import com.colasoft.opensearch.common.geo.GeoDistance;
import com.colasoft.opensearch.common.geo.GeoPoint;
import com.colasoft.opensearch.common.geo.GeoUtils;
import com.colasoft.opensearch.common.unit.DistanceUnit;
import com.colasoft.opensearch.index.fielddata.MultiGeoPointValues;
import com.colasoft.opensearch.index.fielddata.SortedBinaryDocValues;
import com.colasoft.opensearch.index.fielddata.SortedNumericDoubleValues;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.aggregations.Aggregator;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.CardinalityUpperBound;
import com.colasoft.opensearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder.Range;
import com.colasoft.opensearch.search.aggregations.support.CoreValuesSourceType;
import com.colasoft.opensearch.search.aggregations.support.ValuesSource;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceRegistry;
import com.colasoft.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

/**
 * Aggregation Factory for geo_distance agg
 *
 * @opensearch.internal
 */
public class GeoDistanceRangeAggregatorFactory extends ValuesSourceAggregatorFactory {

    public static void registerAggregators(ValuesSourceRegistry.Builder builder) {
        builder.register(
            GeoDistanceAggregationBuilder.REGISTRY_KEY,
            CoreValuesSourceType.GEOPOINT,
            (
                name,
                factories,
                distanceType,
                origin,
                units,
                valuesSource,
                format,
                rangeFactory,
                ranges,
                keyed,
                context,
                parent,
                cardinality,
                metadata) -> {
                DistanceSource distanceSource = new DistanceSource((ValuesSource.GeoPoint) valuesSource, distanceType, origin, units);
                return new RangeAggregator(
                    name,
                    factories,
                    distanceSource,
                    format,
                    rangeFactory,
                    ranges,
                    keyed,
                    context,
                    parent,
                    cardinality,
                    metadata
                );
            },
            true
        );
    }

    private final InternalRange.Factory<InternalGeoDistance.Bucket, InternalGeoDistance> rangeFactory = InternalGeoDistance.FACTORY;
    private final GeoPoint origin;
    private final Range[] ranges;
    private final DistanceUnit unit;
    private final GeoDistance distanceType;
    private final boolean keyed;

    public GeoDistanceRangeAggregatorFactory(
        String name,
        ValuesSourceConfig config,
        GeoPoint origin,
        Range[] ranges,
        DistanceUnit unit,
        GeoDistance distanceType,
        boolean keyed,
        QueryShardContext queryShardContext,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, config, queryShardContext, parent, subFactoriesBuilder, metadata);
        this.origin = origin;
        this.ranges = ranges;
        this.unit = unit;
        this.distanceType = distanceType;
        this.keyed = keyed;
    }

    @Override
    protected Aggregator createUnmapped(SearchContext searchContext, Aggregator parent, Map<String, Object> metadata) throws IOException {
        return new RangeAggregator.Unmapped<>(
            name,
            factories,
            ranges,
            keyed,
            config.format(),
            searchContext,
            parent,
            rangeFactory,
            metadata
        );
    }

    @Override
    protected Aggregator doCreateInternal(
        SearchContext searchContext,
        Aggregator parent,
        CardinalityUpperBound cardinality,
        Map<String, Object> metadata
    ) throws IOException {
        return queryShardContext.getValuesSourceRegistry()
            .getAggregator(GeoDistanceAggregationBuilder.REGISTRY_KEY, config)
            .build(
                name,
                factories,
                distanceType,
                origin,
                unit,
                config.getValuesSource(),
                config.format(),
                rangeFactory,
                ranges,
                keyed,
                searchContext,
                parent,
                cardinality,
                metadata
            );
    }

    /**
     * The source location for the distance calculation
     *
     * @opensearch.internal
     */
    private static class DistanceSource extends ValuesSource.Numeric {

        private final ValuesSource.GeoPoint source;
        private final GeoDistance distanceType;
        private final DistanceUnit units;
        private final com.colasoft.opensearch.common.geo.GeoPoint origin;

        DistanceSource(
            ValuesSource.GeoPoint source,
            GeoDistance distanceType,
            com.colasoft.opensearch.common.geo.GeoPoint origin,
            DistanceUnit units
        ) {
            this.source = source;
            // even if the geo points are unique, there's no guarantee the
            // distances are
            this.distanceType = distanceType;
            this.units = units;
            this.origin = origin;
        }

        @Override
        public boolean isFloatingPoint() {
            return true;
        }

        @Override
        public SortedNumericDocValues longValues(LeafReaderContext ctx) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SortedNumericDoubleValues doubleValues(LeafReaderContext ctx) {
            final MultiGeoPointValues geoValues = source.geoPointValues(ctx);
            return GeoUtils.distanceValues(distanceType, units, geoValues, origin);
        }

        @Override
        public SortedBinaryDocValues bytesValues(LeafReaderContext ctx) {
            throw new UnsupportedOperationException();
        }

    }
}
