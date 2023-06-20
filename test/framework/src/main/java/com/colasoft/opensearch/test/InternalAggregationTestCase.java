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

package com.colasoft.opensearch.test;

import com.colasoft.opensearch.common.SetOnce;
import com.colasoft.opensearch.common.breaker.CircuitBreaker;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.common.util.MockBigArrays;
import com.colasoft.opensearch.common.util.MockPageCacheRecycler;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.core.xcontent.ContextParser;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentParserUtils;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.indices.breaker.NoneCircuitBreakerService;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.rest.action.search.RestSearchAction;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.DocValueFormat;
import com.colasoft.opensearch.search.SearchModule;
import com.colasoft.opensearch.search.aggregations.Aggregation;
import com.colasoft.opensearch.search.aggregations.InternalAggregation;
import com.colasoft.opensearch.search.aggregations.InternalAggregation.ReduceContext;
import com.colasoft.opensearch.search.aggregations.MultiBucketConsumerService;
import com.colasoft.opensearch.search.aggregations.MultiBucketConsumerService.MultiBucketConsumer;
import com.colasoft.opensearch.search.aggregations.ParsedAggregation;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.ParsedAdjacencyMatrix;
import com.colasoft.opensearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.composite.ParsedComposite;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.ParsedFilter;
import com.colasoft.opensearch.search.aggregations.bucket.filter.ParsedFilters;
import com.colasoft.opensearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.global.ParsedGlobal;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.ParsedAutoDateHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.ParsedHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.ParsedVariableWidthHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.VariableWidthHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.missing.ParsedMissing;
import com.colasoft.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ParsedNested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ParsedReverseNested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.ParsedBinaryRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.ParsedDateRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.ParsedGeoDistance;
import com.colasoft.opensearch.search.aggregations.bucket.range.ParsedRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.InternalSampler;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.ParsedSampler;
import com.colasoft.opensearch.search.aggregations.bucket.terms.DoubleTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.MultiTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedDoubleTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedLongRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedLongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedMultiTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedSignificantLongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedSignificantStringTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedStringRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantLongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantStringTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringTerms;
import com.colasoft.opensearch.search.aggregations.metrics.AvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.GeoCentroidAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedAvg;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedCardinality;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedExtendedStats;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedGeoCentroid;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedHDRPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedHDRPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedMax;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedMedianAbsoluteDeviation;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedMin;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedScriptedMetric;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedStats;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedSum;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedTDigestPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedTDigestPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedTopHits;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedValueCount;
import com.colasoft.opensearch.search.aggregations.metrics.ParsedWeightedAvg;
import com.colasoft.opensearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.StatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.WeightedAvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.AvgBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalBucketMetricValue;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalSimpleValue;
import com.colasoft.opensearch.search.aggregations.pipeline.MaxBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedBucketMetricValue;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedDerivative;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedExtendedStatsBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedPercentilesBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedSimpleValue;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedStatsBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.PercentilesBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.PipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.PipelineAggregator.PipelineTree;
import com.colasoft.opensearch.search.aggregations.pipeline.StatsBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.SumBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static com.colasoft.opensearch.common.xcontent.XContentHelper.toXContent;
import static com.colasoft.opensearch.search.aggregations.InternalMultiBucketAggregation.countInnerBucket;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Implementors of this test case should be aware that the aggregation under test needs to be registered
 * in the test's namedWriteableRegistry.  Core aggregations are registered already, but non-core
 * aggs should override {@link InternalAggregationTestCase#registerPlugin()} so that the NamedWriteables
 * can be extracted from the AggregatorSpecs in the plugin (as well as any other custom NamedWriteables)
 */
public abstract class InternalAggregationTestCase<T extends InternalAggregation> extends AbstractNamedWriteableTestCase<T> {
    /**
     * Builds an {@link InternalAggregation.ReduceContextBuilder} that is valid but empty.
     */
    public static InternalAggregation.ReduceContextBuilder emptyReduceContextBuilder() {
        return emptyReduceContextBuilder(PipelineTree.EMPTY);
    }

    /**
     * Builds an {@link InternalAggregation.ReduceContextBuilder} that is valid and nearly
     * empty <strong>except</strong> that it contain {@link PipelineAggregator}s.
     */
    public static InternalAggregation.ReduceContextBuilder emptyReduceContextBuilder(PipelineTree pipelineTree) {
        return new InternalAggregation.ReduceContextBuilder() {
            @Override
            public InternalAggregation.ReduceContext forPartialReduction() {
                return InternalAggregation.ReduceContext.forPartialReduction(BigArrays.NON_RECYCLING_INSTANCE, null, () -> pipelineTree);
            }

            @Override
            public ReduceContext forFinalReduction() {
                return InternalAggregation.ReduceContext.forFinalReduction(BigArrays.NON_RECYCLING_INSTANCE, null, b -> {}, pipelineTree);
            }
        };
    }

    public static final int DEFAULT_MAX_BUCKETS = 100000;
    protected static final double TOLERANCE = 1e-10;

    private static final Comparator<InternalAggregation> INTERNAL_AGG_COMPARATOR = (agg1, agg2) -> {
        if (agg1.isMapped() == agg2.isMapped()) {
            return 0;
        } else if (agg1.isMapped() && agg2.isMapped() == false) {
            return -1;
        } else {
            return 1;
        }
    };

    private final NamedWriteableRegistry namedWriteableRegistry = new NamedWriteableRegistry(getNamedWriteables());

    private final NamedXContentRegistry namedXContentRegistry = new NamedXContentRegistry(getNamedXContents());

    private static final List<NamedXContentRegistry.Entry> namedXContents;
    static {
        Map<String, ContextParser<Object, ? extends Aggregation>> map = new HashMap<>();
        map.put(CardinalityAggregationBuilder.NAME, (p, c) -> ParsedCardinality.fromXContent(p, (String) c));
        map.put(InternalHDRPercentiles.NAME, (p, c) -> ParsedHDRPercentiles.fromXContent(p, (String) c));
        map.put(InternalHDRPercentileRanks.NAME, (p, c) -> ParsedHDRPercentileRanks.fromXContent(p, (String) c));
        map.put(InternalTDigestPercentiles.NAME, (p, c) -> ParsedTDigestPercentiles.fromXContent(p, (String) c));
        map.put(InternalTDigestPercentileRanks.NAME, (p, c) -> ParsedTDigestPercentileRanks.fromXContent(p, (String) c));
        map.put(PercentilesBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedPercentilesBucket.fromXContent(p, (String) c));
        map.put(MedianAbsoluteDeviationAggregationBuilder.NAME, (p, c) -> ParsedMedianAbsoluteDeviation.fromXContent(p, (String) c));
        map.put(MinAggregationBuilder.NAME, (p, c) -> ParsedMin.fromXContent(p, (String) c));
        map.put(MaxAggregationBuilder.NAME, (p, c) -> ParsedMax.fromXContent(p, (String) c));
        map.put(SumAggregationBuilder.NAME, (p, c) -> ParsedSum.fromXContent(p, (String) c));
        map.put(AvgAggregationBuilder.NAME, (p, c) -> ParsedAvg.fromXContent(p, (String) c));
        map.put(WeightedAvgAggregationBuilder.NAME, (p, c) -> ParsedWeightedAvg.fromXContent(p, (String) c));
        map.put(ValueCountAggregationBuilder.NAME, (p, c) -> ParsedValueCount.fromXContent(p, (String) c));
        map.put(InternalSimpleValue.NAME, (p, c) -> ParsedSimpleValue.fromXContent(p, (String) c));
        map.put(DerivativePipelineAggregationBuilder.NAME, (p, c) -> ParsedDerivative.fromXContent(p, (String) c));
        map.put(InternalBucketMetricValue.NAME, (p, c) -> ParsedBucketMetricValue.fromXContent(p, (String) c));
        map.put(StatsAggregationBuilder.NAME, (p, c) -> ParsedStats.fromXContent(p, (String) c));
        map.put(StatsBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedStatsBucket.fromXContent(p, (String) c));
        map.put(ExtendedStatsAggregationBuilder.NAME, (p, c) -> ParsedExtendedStats.fromXContent(p, (String) c));
        map.put(ExtendedStatsBucketPipelineAggregationBuilder.NAME, (p, c) -> ParsedExtendedStatsBucket.fromXContent(p, (String) c));
        map.put(GeoCentroidAggregationBuilder.NAME, (p, c) -> ParsedGeoCentroid.fromXContent(p, (String) c));
        map.put(HistogramAggregationBuilder.NAME, (p, c) -> ParsedHistogram.fromXContent(p, (String) c));
        map.put(DateHistogramAggregationBuilder.NAME, (p, c) -> ParsedDateHistogram.fromXContent(p, (String) c));
        map.put(AutoDateHistogramAggregationBuilder.NAME, (p, c) -> ParsedAutoDateHistogram.fromXContent(p, (String) c));
        map.put(VariableWidthHistogramAggregationBuilder.NAME, (p, c) -> ParsedVariableWidthHistogram.fromXContent(p, (String) c));
        map.put(StringTerms.NAME, (p, c) -> ParsedStringTerms.fromXContent(p, (String) c));
        map.put(LongTerms.NAME, (p, c) -> ParsedLongTerms.fromXContent(p, (String) c));
        map.put(DoubleTerms.NAME, (p, c) -> ParsedDoubleTerms.fromXContent(p, (String) c));
        map.put(LongRareTerms.NAME, (p, c) -> ParsedLongRareTerms.fromXContent(p, (String) c));
        map.put(StringRareTerms.NAME, (p, c) -> ParsedStringRareTerms.fromXContent(p, (String) c));
        map.put(MissingAggregationBuilder.NAME, (p, c) -> ParsedMissing.fromXContent(p, (String) c));
        map.put(NestedAggregationBuilder.NAME, (p, c) -> ParsedNested.fromXContent(p, (String) c));
        map.put(ReverseNestedAggregationBuilder.NAME, (p, c) -> ParsedReverseNested.fromXContent(p, (String) c));
        map.put(GlobalAggregationBuilder.NAME, (p, c) -> ParsedGlobal.fromXContent(p, (String) c));
        map.put(FilterAggregationBuilder.NAME, (p, c) -> ParsedFilter.fromXContent(p, (String) c));
        map.put(InternalSampler.PARSER_NAME, (p, c) -> ParsedSampler.fromXContent(p, (String) c));
        map.put(RangeAggregationBuilder.NAME, (p, c) -> ParsedRange.fromXContent(p, (String) c));
        map.put(DateRangeAggregationBuilder.NAME, (p, c) -> ParsedDateRange.fromXContent(p, (String) c));
        map.put(GeoDistanceAggregationBuilder.NAME, (p, c) -> ParsedGeoDistance.fromXContent(p, (String) c));
        map.put(FiltersAggregationBuilder.NAME, (p, c) -> ParsedFilters.fromXContent(p, (String) c));
        map.put(AdjacencyMatrixAggregationBuilder.NAME, (p, c) -> ParsedAdjacencyMatrix.fromXContent(p, (String) c));
        map.put(SignificantLongTerms.NAME, (p, c) -> ParsedSignificantLongTerms.fromXContent(p, (String) c));
        map.put(SignificantStringTerms.NAME, (p, c) -> ParsedSignificantStringTerms.fromXContent(p, (String) c));
        map.put(ScriptedMetricAggregationBuilder.NAME, (p, c) -> ParsedScriptedMetric.fromXContent(p, (String) c));
        map.put(IpRangeAggregationBuilder.NAME, (p, c) -> ParsedBinaryRange.fromXContent(p, (String) c));
        map.put(TopHitsAggregationBuilder.NAME, (p, c) -> ParsedTopHits.fromXContent(p, (String) c));
        map.put(CompositeAggregationBuilder.NAME, (p, c) -> ParsedComposite.fromXContent(p, (String) c));
        map.put(MultiTermsAggregationBuilder.NAME, (p, c) -> ParsedMultiTerms.fromXContent(p, (String) c));

        namedXContents = map.entrySet()
            .stream()
            .map(entry -> new NamedXContentRegistry.Entry(Aggregation.class, new ParseField(entry.getKey()), entry.getValue()))
            .collect(Collectors.toList());
    }

    public static List<NamedXContentRegistry.Entry> getDefaultNamedXContents() {
        return namedXContents;
    }

    protected List<NamedXContentRegistry.Entry> getNamedXContents() {
        return namedXContents;
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return namedXContentRegistry;
    }

    @Override
    protected final NamedWriteableRegistry getNamedWriteableRegistry() {
        return namedWriteableRegistry;
    }

    /**
     * Implementors can override this if they want to provide a custom list of namedWriteables.  If the implementor
     * _just_ wants to register in namedWriteables provided by a plugin, prefer overriding
     * {@link InternalAggregationTestCase#registerPlugin()} instead because that route handles the automatic
     * conversion of AggSpecs into namedWriteables.
     */
    protected List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        SearchPlugin plugin = registerPlugin();
        SearchModule searchModule = new SearchModule(Settings.EMPTY, plugin == null ? emptyList() : Collections.singletonList(plugin));
        List<NamedWriteableRegistry.Entry> entries = new ArrayList<>(searchModule.getNamedWriteables());

        // Modules/plugins may have extra namedwriteables that are not added by agg specs
        if (plugin != null) {
            entries.addAll(((Plugin) plugin).getNamedWriteables());
        }

        return entries;
    }

    /**
     * If a test needs to register additional aggregation specs for namedWriteable, etc, this method
     * can be overridden by the implementor.
     */
    protected SearchPlugin registerPlugin() {
        return null;
    }

    protected abstract T createTestInstance(String name, Map<String, Object> metadata);

    /** Return an instance on an unmapped field. */
    protected T createUnmappedInstance(String name, Map<String, Object> metadata) {
        // For most impls, we use the same instance in the unmapped case and in the mapped case
        return createTestInstance(name, metadata);
    }

    @Override
    protected final Class<T> categoryClass() {
        return (Class<T>) InternalAggregation.class;
    }

    /**
     * Generate a list of inputs to reduce. Defaults to calling
     * {@link #createTestInstance(String)} and
     * {@link #createUnmappedInstance(String)} but should be overridden
     * if it isn't realistic to reduce test instances.
     */
    protected List<T> randomResultsToReduce(String name, int size) {
        List<T> inputs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            T t = randomBoolean() ? createUnmappedInstance(name) : createTestInstance(name);
            inputs.add(t);
        }
        return inputs;
    }

    public void testReduceRandom() throws IOException {
        String name = randomAlphaOfLength(5);
        int size = between(1, 200);
        List<T> inputs = randomResultsToReduce(name, size);
        assertThat(inputs, hasSize(size));
        List<InternalAggregation> toReduce = new ArrayList<>();
        toReduce.addAll(inputs);
        // Sort aggs so that unmapped come last. This mimicks the behavior of InternalAggregations.reduce()
        inputs.sort(INTERNAL_AGG_COMPARATOR);
        ScriptService mockScriptService = mockScriptService();
        MockBigArrays bigArrays = new MockBigArrays(new MockPageCacheRecycler(Settings.EMPTY), new NoneCircuitBreakerService());
        if (randomBoolean() && toReduce.size() > 1) {
            // sometimes do a partial reduce
            Collections.shuffle(toReduce, random());
            int r = randomIntBetween(1, inputs.size());
            List<InternalAggregation> toPartialReduce = toReduce.subList(0, r);
            // Sort aggs so that unmapped come last. This mimicks the behavior of InternalAggregations.reduce()
            toPartialReduce.sort(INTERNAL_AGG_COMPARATOR);
            InternalAggregation.ReduceContext context = InternalAggregation.ReduceContext.forPartialReduction(
                bigArrays,
                mockScriptService,
                () -> PipelineAggregator.PipelineTree.EMPTY
            );
            @SuppressWarnings("unchecked")
            T reduced = (T) toPartialReduce.get(0).reduce(toPartialReduce, context);
            int initialBucketCount = 0;
            for (InternalAggregation internalAggregation : toPartialReduce) {
                initialBucketCount += countInnerBucket(internalAggregation);
            }
            int reducedBucketCount = countInnerBucket(reduced);
            // check that non final reduction never adds buckets
            assertThat(reducedBucketCount, lessThanOrEqualTo(initialBucketCount));
            /*
             * Sometimes serializing and deserializing the partially reduced
             * result to simulate the compaction that we attempt after a
             * partial reduce. And to simulate cross cluster search.
             */
            if (randomBoolean()) {
                reduced = copyNamedWriteable(reduced, getNamedWriteableRegistry(), categoryClass());
            }
            toReduce = new ArrayList<>(toReduce.subList(r, inputs.size()));
            toReduce.add(reduced);
        }
        MultiBucketConsumer bucketConsumer = new MultiBucketConsumer(
            DEFAULT_MAX_BUCKETS,
            new NoneCircuitBreakerService().getBreaker(CircuitBreaker.REQUEST)
        );
        InternalAggregation.ReduceContext context = InternalAggregation.ReduceContext.forFinalReduction(
            bigArrays,
            mockScriptService,
            bucketConsumer,
            PipelineTree.EMPTY
        );
        @SuppressWarnings("unchecked")
        T reduced = (T) inputs.get(0).reduce(toReduce, context);
        doAssertReducedMultiBucketConsumer(reduced, bucketConsumer);
        assertReduced(reduced, inputs);
    }

    protected void doAssertReducedMultiBucketConsumer(Aggregation agg, MultiBucketConsumerService.MultiBucketConsumer bucketConsumer) {
        InternalAggregationTestCase.assertMultiBucketConsumer(agg, bucketConsumer);
    }

    /**
     * overwrite in tests that need it
     */
    protected ScriptService mockScriptService() {
        return null;
    }

    protected abstract void assertReduced(T reduced, List<T> inputs);

    @Override
    public final T createTestInstance() {
        return createTestInstance(randomAlphaOfLength(5));
    }

    public final Map<String, Object> createTestMetadata() {
        Map<String, Object> metadata = null;
        if (randomBoolean()) {
            metadata = new HashMap<>();
            int metadataCount = between(0, 10);
            while (metadata.size() < metadataCount) {
                metadata.put(randomAlphaOfLength(5), randomAlphaOfLength(5));
            }
        }
        return metadata;
    }

    private T createTestInstance(String name) {
        return createTestInstance(name, createTestMetadata());
    }

    /** Return an instance on an unmapped field. */
    protected final T createUnmappedInstance(String name) {
        Map<String, Object> metadata = new HashMap<>();
        int metadataCount = randomBoolean() ? 0 : between(1, 10);
        while (metadata.size() < metadataCount) {
            metadata.put(randomAlphaOfLength(5), randomAlphaOfLength(5));
        }
        return createUnmappedInstance(name, metadata);
    }

    public T createTestInstanceForXContent() {
        return createTestInstance();
    }

    public final void testFromXContent() throws IOException {
        final T aggregation = createTestInstanceForXContent();
        final ParsedAggregation parsedAggregation = parseAndAssert(aggregation, randomBoolean(), false);
        assertFromXContent(aggregation, parsedAggregation);
    }

    public final void testFromXContentWithRandomFields() throws IOException {
        final T aggregation = createTestInstanceForXContent();
        final ParsedAggregation parsedAggregation = parseAndAssert(aggregation, randomBoolean(), true);
        assertFromXContent(aggregation, parsedAggregation);
    }

    public void testMergePipelineTreeForBWCSerialization() {
        T agg = createTestInstance();
        PipelineAggregator.PipelineTree pipelineTree = randomPipelineTree(agg);
        agg.mergePipelineTreeForBWCSerialization(pipelineTree);
        assertMergedPipelineTreeForBWCSerialization(agg, pipelineTree);
    }

    public void testMergePipelineTreeTwice() {
        T agg = createTestInstance();
        PipelineAggregator.PipelineTree pipelineTree = randomPipelineTree(agg);
        agg.mergePipelineTreeForBWCSerialization(pipelineTree);
        agg.mergePipelineTreeForBWCSerialization(randomPipelineTree(agg)); // This should be ignored
        assertMergedPipelineTreeForBWCSerialization(agg, pipelineTree);
    }

    public static PipelineAggregator.PipelineTree randomPipelineTree(InternalAggregation aggregation) {
        Map<String, PipelineTree> subTree = new HashMap<>();
        aggregation.forEachBucket(bucketAggs -> {
            for (Aggregation subAgg : bucketAggs) {
                if (subTree.containsKey(subAgg.getName())) {
                    continue;
                }
                subTree.put(subAgg.getName(), randomPipelineTree((InternalAggregation) subAgg));
            }
        });
        return new PipelineAggregator.PipelineTree(emptyMap(), randomPipelineAggregators());
    }

    public static List<PipelineAggregator> randomPipelineAggregators() {
        List<PipelineAggregator> pipelines = new ArrayList<>();
        if (randomBoolean()) {
            if (randomBoolean()) {
                pipelines.add(new MaxBucketPipelineAggregationBuilder("name1", "bucket1").create());
            }
            if (randomBoolean()) {
                pipelines.add(new AvgBucketPipelineAggregationBuilder("name2", "bucket2").create());
            }
            if (randomBoolean()) {
                pipelines.add(new SumBucketPipelineAggregationBuilder("name3", "bucket3").create());
            }
        }
        return pipelines;
    }

    @SuppressWarnings("deprecation")
    private void assertMergedPipelineTreeForBWCSerialization(InternalAggregation agg, PipelineAggregator.PipelineTree pipelineTree) {
        assertThat(agg.pipelineAggregatorsForBwcSerialization(), equalTo(pipelineTree.aggregators()));
        agg.forEachBucket(bucketAggs -> {
            for (Aggregation subAgg : bucketAggs) {
                assertMergedPipelineTreeForBWCSerialization((InternalAggregation) subAgg, pipelineTree.subTree(subAgg.getName()));
            }
        });
    }

    protected abstract void assertFromXContent(T aggregation, ParsedAggregation parsedAggregation) throws IOException;

    @SuppressWarnings("unchecked")
    protected <P extends ParsedAggregation> P parseAndAssert(
        final InternalAggregation aggregation,
        final boolean shuffled,
        final boolean addRandomFields
    ) throws IOException {

        final ToXContent.Params params = new ToXContent.MapParams(singletonMap(RestSearchAction.TYPED_KEYS_PARAM, "true"));
        final XContentType xContentType = randomFrom(XContentType.values());
        final boolean humanReadable = randomBoolean();

        final BytesReference originalBytes;
        if (shuffled) {
            originalBytes = toShuffledXContent(aggregation, xContentType, params, humanReadable);
        } else {
            originalBytes = toXContent(aggregation, xContentType, params, humanReadable);
        }
        BytesReference mutated;
        if (addRandomFields) {
            /*
             * - we don't add to the root object because it should only contain
             * the named aggregation to test - we don't want to insert into the
             * "meta" object, because we pass on everything we find there
             *
             * - we don't want to directly insert anything random into "buckets"
             * objects, they are used with "keyed" aggregations and contain
             * named bucket objects. Any new named object on this level should
             * also be a bucket and be parsed as such.
             *
             * we also exclude top_hits that contain SearchHits, as all unknown fields
             * on a root level of SearchHit are interpreted as meta-fields and will be kept.
             */
            Predicate<String> basicExcludes = path -> path.isEmpty()
                || path.endsWith(Aggregation.CommonFields.META.getPreferredName())
                || path.endsWith(Aggregation.CommonFields.BUCKETS.getPreferredName())
                || path.contains("top_hits");
            Predicate<String> excludes = basicExcludes.or(excludePathsFromXContentInsertion());
            mutated = XContentTestUtils.insertRandomFields(xContentType, originalBytes, excludes, random());
        } else {
            mutated = originalBytes;
        }

        SetOnce<Aggregation> parsedAggregation = new SetOnce<>();
        try (XContentParser parser = createParser(xContentType.xContent(), mutated)) {
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            assertEquals(XContentParser.Token.FIELD_NAME, parser.nextToken());
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            XContentParserUtils.parseTypedKeysObject(parser, Aggregation.TYPED_KEYS_DELIMITER, Aggregation.class, parsedAggregation::set);

            assertEquals(XContentParser.Token.END_OBJECT, parser.currentToken());
            assertEquals(XContentParser.Token.END_OBJECT, parser.nextToken());
            assertNull(parser.nextToken());

            Aggregation agg = parsedAggregation.get();
            assertEquals(aggregation.getName(), agg.getName());
            assertEquals(aggregation.getMetadata(), agg.getMetadata());

            assertTrue(agg instanceof ParsedAggregation);
            assertEquals(aggregation.getType(), agg.getType());

            BytesReference parsedBytes = toXContent(agg, xContentType, params, humanReadable);
            OpenSearchAssertions.assertToXContentEquivalent(originalBytes, parsedBytes, xContentType);

            return (P) agg;
        }

    }

    /**
     * Overwrite this in your test if other than the basic xContent paths should be excluded during insertion of random fields
     */
    protected Predicate<String> excludePathsFromXContentInsertion() {
        return path -> false;
    }

    /**
     * @return a random {@link DocValueFormat} that can be used in aggregations which
     * compute numbers.
     */
    protected static DocValueFormat randomNumericDocValueFormat() {
        final List<Supplier<DocValueFormat>> formats = new ArrayList<>(3);
        formats.add(() -> DocValueFormat.RAW);
        formats.add(() -> new DocValueFormat.Decimal(randomFrom("###.##", "###,###.##")));
        return randomFrom(formats).get();
    }

    public static void assertMultiBucketConsumer(Aggregation agg, MultiBucketConsumer bucketConsumer) {
        assertMultiBucketConsumer(countInnerBucket(agg), bucketConsumer);
    }

    private static void assertMultiBucketConsumer(int innerBucketCount, MultiBucketConsumer bucketConsumer) {
        assertThat(bucketConsumer.getCount(), equalTo(innerBucketCount));
    }
}
