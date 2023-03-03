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

package com.colasoft.opensearch.search.aggregations.bucket;

import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.common.util.Comparators;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.search.aggregations.Aggregation;
import com.colasoft.opensearch.search.aggregations.Aggregator.SubAggCollectionMode;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.Histogram;
import com.colasoft.opensearch.search.aggregations.bucket.terms.Terms;
import com.colasoft.opensearch.search.aggregations.metrics.Avg;
import com.colasoft.opensearch.search.aggregations.metrics.AvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStats;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.BucketOrder;
import com.colasoft.opensearch.search.aggregations.support.ValuesSource;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import static com.colasoft.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static com.colasoft.opensearch.search.aggregations.AggregationBuilders.avg;
import static com.colasoft.opensearch.search.aggregations.AggregationBuilders.extendedStats;
import static com.colasoft.opensearch.search.aggregations.AggregationBuilders.histogram;
import static com.colasoft.opensearch.search.aggregations.AggregationBuilders.terms;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertSearchResponse;
import static org.hamcrest.core.IsNull.notNullValue;

@OpenSearchIntegTestCase.SuiteScopeTestCase
public class NaNSortingIT extends OpenSearchIntegTestCase {

    private enum SubAggregation {
        AVG("avg") {
            @Override
            public AvgAggregationBuilder builder() {
                AvgAggregationBuilder factory = avg(name);
                factory.field("numeric_field");
                return factory;
            }

            @Override
            public double getValue(Aggregation aggregation) {
                return ((Avg) aggregation).getValue();
            }
        },
        VARIANCE("variance") {
            @Override
            public ExtendedStatsAggregationBuilder builder() {
                ExtendedStatsAggregationBuilder factory = extendedStats(name);
                factory.field("numeric_field");
                return factory;
            }

            @Override
            public String sortKey() {
                return name + ".variance";
            }

            @Override
            public double getValue(Aggregation aggregation) {
                return ((ExtendedStats) aggregation).getVariance();
            }
        },
        STD_DEVIATION("std_deviation") {
            @Override
            public ExtendedStatsAggregationBuilder builder() {
                ExtendedStatsAggregationBuilder factory = extendedStats(name);
                factory.field("numeric_field");
                return factory;
            }

            @Override
            public String sortKey() {
                return name + ".std_deviation";
            }

            @Override
            public double getValue(Aggregation aggregation) {
                return ((ExtendedStats) aggregation).getStdDeviation();
            }
        };

        SubAggregation(String name) {
            this.name = name;
        }

        public String name;

        public abstract
            ValuesSourceAggregationBuilder.LeafOnly<
                ValuesSource.Numeric,
                ? extends ValuesSourceAggregationBuilder.LeafOnly<ValuesSource.Numeric, ?>>
            builder();

        public String sortKey() {
            return name;
        }

        public abstract double getValue(Aggregation aggregation);
    }

    @Override
    public void setupSuiteScopeCluster() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("idx").setMapping("string_value", "type=keyword").get());
        final int numDocs = randomIntBetween(2, 10);
        for (int i = 0; i < numDocs; ++i) {
            final long value = randomInt(5);
            XContentBuilder source = jsonBuilder().startObject()
                .field("long_value", value)
                .field("double_value", value + 0.05)
                .field("string_value", "str_" + value);
            if (randomBoolean()) {
                source.field("numeric_value", randomDouble());
            }
            client().prepareIndex("idx").setSource(source.endObject()).get();
        }
        refresh();
        ensureSearchable();
    }

    private void assertCorrectlySorted(Terms terms, boolean asc, SubAggregation agg) {
        assertThat(terms, notNullValue());
        double previousValue = asc ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (Terms.Bucket bucket : terms.getBuckets()) {
            Aggregation sub = bucket.getAggregations().get(agg.name);
            double value = agg.getValue(sub);
            assertTrue(Comparators.compareDiscardNaN(previousValue, value, asc) <= 0);
            previousValue = value;
        }
    }

    private void assertCorrectlySorted(Histogram histo, boolean asc, SubAggregation agg) {
        assertThat(histo, notNullValue());
        double previousValue = asc ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (Histogram.Bucket bucket : histo.getBuckets()) {
            Aggregation sub = bucket.getAggregations().get(agg.name);
            double value = agg.getValue(sub);
            assertTrue(Comparators.compareDiscardNaN(previousValue, value, asc) <= 0);
            previousValue = value;
        }
    }

    public void testTerms(String fieldName) {
        final boolean asc = randomBoolean();
        SubAggregation agg = randomFrom(SubAggregation.values());
        SearchResponse response = client().prepareSearch("idx")
            .addAggregation(
                terms("terms").field(fieldName)
                    .collectMode(randomFrom(SubAggCollectionMode.values()))
                    .subAggregation(agg.builder())
                    .order(BucketOrder.aggregation(agg.sortKey(), asc))
            )
            .get();

        assertSearchResponse(response);
        final Terms terms = response.getAggregations().get("terms");
        assertCorrectlySorted(terms, asc, agg);
    }

    public void testStringTerms() {
        testTerms("string_value");
    }

    public void testLongTerms() {
        testTerms("long_value");
    }

    public void testDoubleTerms() {
        testTerms("double_value");
    }

    public void testLongHistogram() {
        final boolean asc = randomBoolean();
        SubAggregation agg = randomFrom(SubAggregation.values());
        SearchResponse response = client().prepareSearch("idx")
            .addAggregation(
                histogram("histo").field("long_value")
                    .interval(randomIntBetween(1, 2))
                    .subAggregation(agg.builder())
                    .order(BucketOrder.aggregation(agg.sortKey(), asc))
            )
            .get();

        assertSearchResponse(response);
        final Histogram histo = response.getAggregations().get("histo");
        assertCorrectlySorted(histo, asc, agg);
    }

}
