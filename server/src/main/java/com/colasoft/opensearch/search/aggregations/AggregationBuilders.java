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

package com.colasoft.opensearch.search.aggregations;

import com.colasoft.opensearch.common.geo.GeoDistance;
import com.colasoft.opensearch.common.geo.GeoPoint;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.AdjacencyMatrix;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.Filter;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.Filters;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
import com.colasoft.opensearch.search.aggregations.bucket.global.Global;
import com.colasoft.opensearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.Histogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.missing.Missing;
import com.colasoft.opensearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.nested.Nested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ReverseNested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.Range;
import com.colasoft.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.DiversifiedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.Sampler;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.SamplerAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.MultiTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantTextAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.Terms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Avg;
import com.colasoft.opensearch.search.aggregations.metrics.AvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Cardinality;
import com.colasoft.opensearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStats;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.GeoCentroid;
import com.colasoft.opensearch.search.aggregations.metrics.GeoCentroidAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Max;
import com.colasoft.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.MedianAbsoluteDeviation;
import com.colasoft.opensearch.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Min;
import com.colasoft.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.PercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.PercentileRanksAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Percentiles;
import com.colasoft.opensearch.search.aggregations.metrics.PercentilesAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ScriptedMetric;
import com.colasoft.opensearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Stats;
import com.colasoft.opensearch.search.aggregations.metrics.StatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.Sum;
import com.colasoft.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.TopHits;
import com.colasoft.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ValueCount;
import com.colasoft.opensearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.WeightedAvgAggregationBuilder;

import java.util.List;
import java.util.Map;

/**
 * Utility class to create aggregations.
 *
 * @opensearch.internal
 */
public class AggregationBuilders {

    private AggregationBuilders() {}

    /**
     * Create a new {@link ValueCount} aggregation with the given name.
     */
    public static ValueCountAggregationBuilder count(String name) {
        return new ValueCountAggregationBuilder(name);
    }

    /**
     * Create a new {@link Avg} aggregation with the given name.
     */
    public static AvgAggregationBuilder avg(String name) {
        return new AvgAggregationBuilder(name);
    }

    /**
     * Create a new {@link Avg} aggregation with the given name.
     */
    public static WeightedAvgAggregationBuilder weightedAvg(String name) {
        return new WeightedAvgAggregationBuilder(name);
    }

    /**
     * Create a new {@link Max} aggregation with the given name.
     */
    public static MaxAggregationBuilder max(String name) {
        return new MaxAggregationBuilder(name);
    }

    /**
     * Create a new {@link Min} aggregation with the given name.
     */
    public static MinAggregationBuilder min(String name) {
        return new MinAggregationBuilder(name);
    }

    /**
     * Create a new {@link Sum} aggregation with the given name.
     */
    public static SumAggregationBuilder sum(String name) {
        return new SumAggregationBuilder(name);
    }

    /**
     * Create a new {@link Stats} aggregation with the given name.
     */
    public static StatsAggregationBuilder stats(String name) {
        return new StatsAggregationBuilder(name);
    }

    /**
     * Create a new {@link ExtendedStats} aggregation with the given name.
     */
    public static ExtendedStatsAggregationBuilder extendedStats(String name) {
        return new ExtendedStatsAggregationBuilder(name);
    }

    /**
     * Create a new {@link Filter} aggregation with the given name.
     */
    public static FilterAggregationBuilder filter(String name, QueryBuilder filter) {
        return new FilterAggregationBuilder(name, filter);
    }

    /**
     * Create a new {@link Filters} aggregation with the given name.
     */
    public static FiltersAggregationBuilder filters(String name, KeyedFilter... filters) {
        return new FiltersAggregationBuilder(name, filters);
    }

    /**
     * Create a new {@link Filters} aggregation with the given name.
     */
    public static FiltersAggregationBuilder filters(String name, QueryBuilder... filters) {
        return new FiltersAggregationBuilder(name, filters);
    }

    /**
     * Create a new {@link AdjacencyMatrix} aggregation with the given name.
     */
    public static AdjacencyMatrixAggregationBuilder adjacencyMatrix(String name, Map<String, QueryBuilder> filters) {
        return new AdjacencyMatrixAggregationBuilder(name, filters);
    }

    /**
     * Create a new {@link AdjacencyMatrix} aggregation with the given name and separator
     */
    public static AdjacencyMatrixAggregationBuilder adjacencyMatrix(String name, String separator, Map<String, QueryBuilder> filters) {
        return new AdjacencyMatrixAggregationBuilder(name, separator, filters);
    }

    /**
     * Create a new {@link Sampler} aggregation with the given name.
     */
    public static SamplerAggregationBuilder sampler(String name) {
        return new SamplerAggregationBuilder(name);
    }

    /**
     * Create a new {@link Sampler} aggregation with the given name.
     */
    public static DiversifiedAggregationBuilder diversifiedSampler(String name) {
        return new DiversifiedAggregationBuilder(name);
    }

    /**
     * Create a new {@link Global} aggregation with the given name.
     */
    public static GlobalAggregationBuilder global(String name) {
        return new GlobalAggregationBuilder(name);
    }

    /**
     * Create a new {@link Missing} aggregation with the given name.
     */
    public static MissingAggregationBuilder missing(String name) {
        return new MissingAggregationBuilder(name);
    }

    /**
     * Create a new {@link Nested} aggregation with the given name.
     */
    public static NestedAggregationBuilder nested(String name, String path) {
        return new NestedAggregationBuilder(name, path);
    }

    /**
     * Create a new {@link ReverseNested} aggregation with the given name.
     */
    public static ReverseNestedAggregationBuilder reverseNested(String name) {
        return new ReverseNestedAggregationBuilder(name);
    }

    /**
     * Create a new {@link GeoDistance} aggregation with the given name.
     */
    public static GeoDistanceAggregationBuilder geoDistance(String name, GeoPoint origin) {
        return new GeoDistanceAggregationBuilder(name, origin);
    }

    /**
     * Create a new {@link Histogram} aggregation with the given name.
     */
    public static HistogramAggregationBuilder histogram(String name) {
        return new HistogramAggregationBuilder(name);
    }

    /**
     * Create a new {@link SignificantTerms} aggregation with the given name.
     */
    public static SignificantTermsAggregationBuilder significantTerms(String name) {
        return new SignificantTermsAggregationBuilder(name);
    }

    /**
     * Create a new {@link SignificantTextAggregationBuilder} aggregation with the given name and text field name
     */
    public static SignificantTextAggregationBuilder significantText(String name, String fieldName) {
        return new SignificantTextAggregationBuilder(name, fieldName);
    }

    /**
     * Create a new {@link DateHistogramAggregationBuilder} aggregation with the given
     * name.
     */
    public static DateHistogramAggregationBuilder dateHistogram(String name) {
        return new DateHistogramAggregationBuilder(name);
    }

    /**
     * Create a new {@link Range} aggregation with the given name.
     */
    public static RangeAggregationBuilder range(String name) {
        return new RangeAggregationBuilder(name);
    }

    /**
     * Create a new {@link DateRangeAggregationBuilder} aggregation with the
     * given name.
     */
    public static DateRangeAggregationBuilder dateRange(String name) {
        return new DateRangeAggregationBuilder(name);
    }

    /**
     * Create a new {@link IpRangeAggregationBuilder} aggregation with the
     * given name.
     */
    public static IpRangeAggregationBuilder ipRange(String name) {
        return new IpRangeAggregationBuilder(name);
    }

    /**
     * Create a new {@link Terms} aggregation with the given name.
     */
    public static TermsAggregationBuilder terms(String name) {
        return new TermsAggregationBuilder(name);
    }

    /**
     * Create a new {@link Percentiles} aggregation with the given name.
     */
    public static PercentilesAggregationBuilder percentiles(String name) {
        return new PercentilesAggregationBuilder(name);
    }

    /**
     * Create a new {@link PercentileRanks} aggregation with the given name.
     */
    public static PercentileRanksAggregationBuilder percentileRanks(String name, double[] values) {
        return new PercentileRanksAggregationBuilder(name, values);
    }

    /**
     * Create a new {@link MedianAbsoluteDeviation} aggregation with the given name
     */
    public static MedianAbsoluteDeviationAggregationBuilder medianAbsoluteDeviation(String name) {
        return new MedianAbsoluteDeviationAggregationBuilder(name);
    }

    /**
     * Create a new {@link Cardinality} aggregation with the given name.
     */
    public static CardinalityAggregationBuilder cardinality(String name) {
        return new CardinalityAggregationBuilder(name);
    }

    /**
     * Create a new {@link TopHits} aggregation with the given name.
     */
    public static TopHitsAggregationBuilder topHits(String name) {
        return new TopHitsAggregationBuilder(name);
    }

    /**
     * Create a new {@link GeoCentroid} aggregation with the given name.
     */
    public static GeoCentroidAggregationBuilder geoCentroid(String name) {
        return new GeoCentroidAggregationBuilder(name);
    }

    /**
     * Create a new {@link ScriptedMetric} aggregation with the given name.
     */
    public static ScriptedMetricAggregationBuilder scriptedMetric(String name) {
        return new ScriptedMetricAggregationBuilder(name);
    }

    /**
     * Create a new {@link CompositeAggregationBuilder} aggregation with the given name.
     */
    public static CompositeAggregationBuilder composite(String name, List<CompositeValuesSourceBuilder<?>> sources) {
        return new CompositeAggregationBuilder(name, sources);
    }

    /**
     * Create a new {@link MultiTermsAggregationBuilder} aggregation with the given name.
     */
    public static MultiTermsAggregationBuilder multiTerms(String name) {
        return new MultiTermsAggregationBuilder(name);
    }
}
