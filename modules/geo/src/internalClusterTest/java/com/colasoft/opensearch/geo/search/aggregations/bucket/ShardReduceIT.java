/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.geo.search.aggregations.bucket;

import com.colasoft.opensearch.action.index.IndexRequestBuilder;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.geo.GeoModulePluginIntegTestCase;
import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.GeoGrid;
import com.colasoft.opensearch.geo.tests.common.AggregationBuilders;
import com.colasoft.opensearch.geometry.utils.Geohash;
import com.colasoft.opensearch.index.query.QueryBuilders;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.Histogram;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import static org.hamcrest.Matchers.equalTo;
import static com.colasoft.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static com.colasoft.opensearch.search.aggregations.AggregationBuilders.dateHistogram;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertSearchResponse;

/**
 * Tests making sure that the reduce is propagated to all aggregations in the hierarchy when executing on a single shard
 * These tests are based on the date histogram in combination of min_doc_count=0. In order for the date histogram to
 * compute empty buckets, its {@code reduce()} method must be called. So by adding the date histogram under other buckets,
 * we can make sure that the reduce is properly propagated by checking that empty buckets were created.
 */
@OpenSearchIntegTestCase.SuiteScopeTestCase
public class ShardReduceIT extends GeoModulePluginIntegTestCase {

    private IndexRequestBuilder indexDoc(String date, int value) throws Exception {
        return client().prepareIndex("idx")
            .setSource(
                jsonBuilder().startObject()
                    .field("value", value)
                    .field("ip", "10.0.0." + value)
                    .field("location", Geohash.stringEncode(5, 52, Geohash.PRECISION))
                    .field("date", date)
                    .field("term-l", 1)
                    .field("term-d", 1.5)
                    .field("term-s", "term")
                    .startObject("nested")
                    .field("date", date)
                    .endObject()
                    .endObject()
            );
    }

    @Override
    public void setupSuiteScopeCluster() throws Exception {
        assertAcked(
            prepareCreate("idx").setMapping(
                "nested",
                "type=nested",
                "ip",
                "type=ip",
                "location",
                "type=geo_point",
                "term-s",
                "type=keyword"
            )
        );

        indexRandom(true, indexDoc("2014-01-01", 1), indexDoc("2014-01-02", 2), indexDoc("2014-01-04", 3));
        ensureSearchable();
    }

    public void testGeoHashGrid() throws Exception {
        SearchResponse response = client().prepareSearch("idx")
            .setQuery(QueryBuilders.matchAllQuery())
            .addAggregation(
                AggregationBuilders.geohashGrid("grid")
                    .field("location")
                    .subAggregation(dateHistogram("histo").field("date").fixedInterval(DateHistogramInterval.DAY).minDocCount(0))
            )
            .get();

        assertSearchResponse(response);

        GeoGrid grid = response.getAggregations().get("grid");
        Histogram histo = grid.getBuckets().iterator().next().getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }

    public void testGeoTileGrid() throws Exception {
        SearchResponse response = client().prepareSearch("idx")
            .setQuery(QueryBuilders.matchAllQuery())
            .addAggregation(
                AggregationBuilders.geotileGrid("grid")
                    .field("location")
                    .subAggregation(dateHistogram("histo").field("date").fixedInterval(DateHistogramInterval.DAY).minDocCount(0))
            )
            .get();

        assertSearchResponse(response);

        GeoGrid grid = response.getAggregations().get("grid");
        Histogram histo = grid.getBuckets().iterator().next().getAggregations().get("histo");
        assertThat(histo.getBuckets().size(), equalTo(4));
    }
}
