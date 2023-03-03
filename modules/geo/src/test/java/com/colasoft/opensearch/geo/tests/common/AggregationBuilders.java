/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.geo.tests.common;

import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.GeoHashGridAggregationBuilder;
import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.GeoTileGridAggregationBuilder;
import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.InternalGeoHashGrid;
import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.InternalGeoTileGrid;
import com.colasoft.opensearch.geo.search.aggregations.metrics.GeoBounds;
import com.colasoft.opensearch.geo.search.aggregations.metrics.GeoBoundsAggregationBuilder;

public class AggregationBuilders {
    /**
     * Create a new {@link GeoBounds} aggregation with the given name.
     */
    public static GeoBoundsAggregationBuilder geoBounds(String name) {
        return new GeoBoundsAggregationBuilder(name);
    }

    /**
     * Create a new {@link InternalGeoHashGrid} aggregation with the given name.
     */
    public static GeoHashGridAggregationBuilder geohashGrid(String name) {
        return new GeoHashGridAggregationBuilder(name);
    }

    /**
     * Create a new {@link InternalGeoTileGrid} aggregation with the given name.
     */
    public static GeoTileGridAggregationBuilder geotileGrid(String name) {
        return new GeoTileGridAggregationBuilder(name);
    }
}
