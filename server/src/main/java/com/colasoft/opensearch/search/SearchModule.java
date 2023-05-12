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

package com.colasoft.opensearch.search;

import org.apache.lucene.search.BooleanQuery;
import com.colasoft.opensearch.common.NamedRegistry;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.geo.GeoShapeType;
import com.colasoft.opensearch.common.geo.ShapesAvailability;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry.Entry;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.common.xcontent.ParseFieldRegistry;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.BoolQueryBuilder;
import com.colasoft.opensearch.index.query.BoostingQueryBuilder;
import com.colasoft.opensearch.index.query.CommonTermsQueryBuilder;
import com.colasoft.opensearch.index.query.ConstantScoreQueryBuilder;
import com.colasoft.opensearch.index.query.DisMaxQueryBuilder;
import com.colasoft.opensearch.index.query.DistanceFeatureQueryBuilder;
import com.colasoft.opensearch.index.query.ExistsQueryBuilder;
import com.colasoft.opensearch.index.query.FieldMaskingSpanQueryBuilder;
import com.colasoft.opensearch.index.query.FuzzyQueryBuilder;
import com.colasoft.opensearch.index.query.GeoBoundingBoxQueryBuilder;
import com.colasoft.opensearch.index.query.GeoDistanceQueryBuilder;
import com.colasoft.opensearch.index.query.GeoPolygonQueryBuilder;
import com.colasoft.opensearch.index.query.GeoShapeQueryBuilder;
import com.colasoft.opensearch.index.query.IdsQueryBuilder;
import com.colasoft.opensearch.index.query.IntervalQueryBuilder;
import com.colasoft.opensearch.index.query.IntervalsSourceProvider;
import com.colasoft.opensearch.index.query.MatchAllQueryBuilder;
import com.colasoft.opensearch.index.query.MatchBoolPrefixQueryBuilder;
import com.colasoft.opensearch.index.query.MatchNoneQueryBuilder;
import com.colasoft.opensearch.index.query.MatchPhrasePrefixQueryBuilder;
import com.colasoft.opensearch.index.query.MatchPhraseQueryBuilder;
import com.colasoft.opensearch.index.query.MatchQueryBuilder;
import com.colasoft.opensearch.index.query.MoreLikeThisQueryBuilder;
import com.colasoft.opensearch.index.query.MultiMatchQueryBuilder;
import com.colasoft.opensearch.index.query.NestedQueryBuilder;
import com.colasoft.opensearch.index.query.PrefixQueryBuilder;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.query.QueryStringQueryBuilder;
import com.colasoft.opensearch.index.query.RangeQueryBuilder;
import com.colasoft.opensearch.index.query.RegexpQueryBuilder;
import com.colasoft.opensearch.index.query.ScriptQueryBuilder;
import com.colasoft.opensearch.index.query.SimpleQueryStringBuilder;
import com.colasoft.opensearch.index.query.SpanContainingQueryBuilder;
import com.colasoft.opensearch.index.query.SpanFirstQueryBuilder;
import com.colasoft.opensearch.index.query.SpanMultiTermQueryBuilder;
import com.colasoft.opensearch.index.query.SpanNearQueryBuilder;
import com.colasoft.opensearch.index.query.SpanNearQueryBuilder.SpanGapQueryBuilder;
import com.colasoft.opensearch.index.query.SpanNotQueryBuilder;
import com.colasoft.opensearch.index.query.SpanOrQueryBuilder;
import com.colasoft.opensearch.index.query.SpanTermQueryBuilder;
import com.colasoft.opensearch.index.query.SpanWithinQueryBuilder;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.index.query.TermsQueryBuilder;
import com.colasoft.opensearch.index.query.TermsSetQueryBuilder;
import com.colasoft.opensearch.index.query.WildcardQueryBuilder;
import com.colasoft.opensearch.index.query.WrapperQueryBuilder;
import com.colasoft.opensearch.index.query.functionscore.ExponentialDecayFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import com.colasoft.opensearch.index.query.functionscore.GaussDecayFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.LinearDecayFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.RandomScoreFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.ScoreFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.ScriptScoreFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.ScriptScoreQueryBuilder;
import com.colasoft.opensearch.index.query.functionscore.WeightBuilder;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.plugins.SearchPlugin.AggregationSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.FetchPhaseConstructionContext;
import com.colasoft.opensearch.plugins.SearchPlugin.PipelineAggregationSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.QuerySpec;
import com.colasoft.opensearch.plugins.SearchPlugin.RescorerSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.ScoreFunctionSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.SearchExtSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.SearchExtensionSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.SignificanceHeuristicSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.SortSpec;
import com.colasoft.opensearch.plugins.SearchPlugin.SuggesterSpec;
import com.colasoft.opensearch.search.aggregations.AggregationBuilder;
import com.colasoft.opensearch.search.aggregations.BaseAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.InternalAggregation;
import com.colasoft.opensearch.search.aggregations.PipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.AdjacencyMatrixAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.InternalAdjacencyMatrix;
import com.colasoft.opensearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.composite.InternalComposite;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.filter.InternalFilter;
import com.colasoft.opensearch.search.aggregations.bucket.filter.InternalFilters;
import com.colasoft.opensearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.global.InternalGlobal;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.AutoDateHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalAutoDateHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalDateHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalVariableWidthHistogram;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.VariableWidthHistogramAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.missing.InternalMissing;
import com.colasoft.opensearch.search.aggregations.bucket.missing.MissingAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.nested.InternalNested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.InternalReverseNested;
import com.colasoft.opensearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.nested.ReverseNestedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.GeoDistanceAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalBinaryRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalDateRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalGeoDistance;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalRange;
import com.colasoft.opensearch.search.aggregations.bucket.range.IpRangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.DiversifiedAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.InternalSampler;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.SamplerAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.UnmappedSampler;
import com.colasoft.opensearch.search.aggregations.bucket.terms.DoubleTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.InternalMultiTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.MultiTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.MultiTermsAggregationFactory;
import com.colasoft.opensearch.search.aggregations.bucket.terms.RareTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantLongTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantStringTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantTermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantTextAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.UnmappedRareTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.UnmappedSignificantTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.UnmappedTerms;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.ChiSquare;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.GND;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.JLHScore;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.MutualInformation;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.PercentageScore;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.ScriptHeuristic;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.SignificanceHeuristic;
import com.colasoft.opensearch.search.aggregations.metrics.AvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.GeoCentroidAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.InternalAvg;
import com.colasoft.opensearch.search.aggregations.metrics.InternalCardinality;
import com.colasoft.opensearch.search.aggregations.metrics.InternalExtendedStats;
import com.colasoft.opensearch.search.aggregations.metrics.InternalGeoCentroid;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMax;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMedianAbsoluteDeviation;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMin;
import com.colasoft.opensearch.search.aggregations.metrics.InternalScriptedMetric;
import com.colasoft.opensearch.search.aggregations.metrics.InternalStats;
import com.colasoft.opensearch.search.aggregations.metrics.InternalSum;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentileRanks;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentiles;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTopHits;
import com.colasoft.opensearch.search.aggregations.metrics.InternalValueCount;
import com.colasoft.opensearch.search.aggregations.metrics.InternalWeightedAvg;
import com.colasoft.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.MedianAbsoluteDeviationAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.MinAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.PercentileRanksAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.PercentilesAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ScriptedMetricAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.StatsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.metrics.WeightedAvgAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.AvgBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.AvgBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketScriptPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketScriptPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketSelectorPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketSelectorPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketSortPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.BucketSortPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.CumulativeSumPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.CumulativeSumPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.DerivativePipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.EwmaModel;
import com.colasoft.opensearch.search.aggregations.pipeline.ExtendedStatsBucketParser;
import com.colasoft.opensearch.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.ExtendedStatsBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.HoltLinearModel;
import com.colasoft.opensearch.search.aggregations.pipeline.HoltWintersModel;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalBucketMetricValue;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalDerivative;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalExtendedStatsBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalPercentilesBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalSimpleValue;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalStatsBucket;
import com.colasoft.opensearch.search.aggregations.pipeline.LinearModel;
import com.colasoft.opensearch.search.aggregations.pipeline.MaxBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.MaxBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.MinBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.MinBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.MovAvgModel;
import com.colasoft.opensearch.search.aggregations.pipeline.MovAvgPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.MovAvgPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.MovFnPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.MovFnPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.PercentilesBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.PercentilesBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.PipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.SerialDiffPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.SerialDiffPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.SimpleModel;
import com.colasoft.opensearch.search.aggregations.pipeline.StatsBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.StatsBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.SumBucketPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.SumBucketPipelineAggregator;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceRegistry;
import com.colasoft.opensearch.search.fetch.FetchPhase;
import com.colasoft.opensearch.search.fetch.FetchSubPhase;
import com.colasoft.opensearch.search.fetch.subphase.ExplainPhase;
import com.colasoft.opensearch.search.fetch.subphase.FetchDocValuesPhase;
import com.colasoft.opensearch.search.fetch.subphase.FetchFieldsPhase;
import com.colasoft.opensearch.search.fetch.subphase.FetchScorePhase;
import com.colasoft.opensearch.search.fetch.subphase.FetchSourcePhase;
import com.colasoft.opensearch.search.fetch.subphase.FetchVersionPhase;
import com.colasoft.opensearch.search.fetch.subphase.MatchedQueriesPhase;
import com.colasoft.opensearch.search.fetch.subphase.ScriptFieldsPhase;
import com.colasoft.opensearch.search.fetch.subphase.SeqNoPrimaryTermPhase;
import com.colasoft.opensearch.search.fetch.subphase.highlight.FastVectorHighlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.HighlightPhase;
import com.colasoft.opensearch.search.fetch.subphase.highlight.Highlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.PlainHighlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.UnifiedHighlighter;
import com.colasoft.opensearch.search.query.QueryPhase;
import com.colasoft.opensearch.search.query.QueryPhaseSearcher;
import com.colasoft.opensearch.search.rescore.QueryRescorerBuilder;
import com.colasoft.opensearch.search.rescore.RescorerBuilder;
import com.colasoft.opensearch.search.sort.FieldSortBuilder;
import com.colasoft.opensearch.search.sort.GeoDistanceSortBuilder;
import com.colasoft.opensearch.search.sort.ScoreSortBuilder;
import com.colasoft.opensearch.search.sort.ScriptSortBuilder;
import com.colasoft.opensearch.search.sort.SortBuilder;
import com.colasoft.opensearch.search.sort.SortValue;
import com.colasoft.opensearch.search.suggest.Suggest;
import com.colasoft.opensearch.search.suggest.SuggestionBuilder;
import com.colasoft.opensearch.search.suggest.completion.CompletionSuggestion;
import com.colasoft.opensearch.search.suggest.completion.CompletionSuggestionBuilder;
import com.colasoft.opensearch.search.suggest.phrase.Laplace;
import com.colasoft.opensearch.search.suggest.phrase.LinearInterpolation;
import com.colasoft.opensearch.search.suggest.phrase.PhraseSuggestion;
import com.colasoft.opensearch.search.suggest.phrase.PhraseSuggestionBuilder;
import com.colasoft.opensearch.search.suggest.phrase.SmoothingModel;
import com.colasoft.opensearch.search.suggest.phrase.StupidBackoff;
import com.colasoft.opensearch.search.suggest.term.TermSuggestion;
import com.colasoft.opensearch.search.suggest.term.TermSuggestionBuilder;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static com.colasoft.opensearch.index.query.CommonTermsQueryBuilder.COMMON_TERMS_QUERY_DEPRECATION_MSG;

/**
 * Sets up things that can be done at search time like queries, aggregations, and suggesters.
 *
 * @opensearch.internal
 */
public class SearchModule {
    public static final Setting<Integer> INDICES_MAX_CLAUSE_COUNT_SETTING = Setting.intSetting(
        "indices.query.bool.max_clause_count",
        1024,
        1,
        Integer.MAX_VALUE,
        Setting.Property.NodeScope
    );

    private final Map<String, Highlighter> highlighters;
    private final ParseFieldRegistry<MovAvgModel.AbstractModelParser> movingAverageModelParserRegistry = new ParseFieldRegistry<>(
        "moving_avg_model"
    );

    private final List<FetchSubPhase> fetchSubPhases = new ArrayList<>();

    private final Settings settings;
    private final List<NamedWriteableRegistry.Entry> namedWriteables = new ArrayList<>();
    private final List<NamedXContentRegistry.Entry> namedXContents = new ArrayList<>();
    private final ValuesSourceRegistry valuesSourceRegistry;
    private final QueryPhaseSearcher queryPhaseSearcher;
    private final SearchPlugin.ExecutorServiceProvider indexSearcherExecutorProvider;

    /**
     * Constructs a new SearchModule object
     *
     * NOTE: This constructor should not be called in production unless an accurate {@link Settings} object is provided.
     *       When constructed, a static flag is set in Lucene {@link BooleanQuery#setMaxClauseCount} according to the settings.
     * @param settings Current settings
     * @param plugins List of included {@link SearchPlugin} objects.
     */
    public SearchModule(Settings settings, List<SearchPlugin> plugins) {
        this.settings = settings;
        registerSuggesters(plugins);
        highlighters = setupHighlighters(settings, plugins);
        registerScoreFunctions(plugins);
        registerQueryParsers(plugins);
        registerRescorers(plugins);
        registerSortParsers(plugins);
        registerValueFormats();
        registerSignificanceHeuristics(plugins);
        this.valuesSourceRegistry = registerAggregations(plugins);
        registerMovingAverageModels(plugins);
        registerPipelineAggregations(plugins);
        registerFetchSubPhases(plugins);
        registerSearchExts(plugins);
        registerShapes();
        registerIntervalsSourceProviders();
        queryPhaseSearcher = registerQueryPhaseSearcher(plugins);
        indexSearcherExecutorProvider = registerIndexSearcherExecutorProvider(plugins);
        namedWriteables.addAll(SortValue.namedWriteables());
    }

    public List<NamedWriteableRegistry.Entry> getNamedWriteables() {
        return namedWriteables;
    }

    public List<NamedXContentRegistry.Entry> getNamedXContents() {
        return namedXContents;
    }

    public ValuesSourceRegistry getValuesSourceRegistry() {
        return valuesSourceRegistry;
    }

    /**
     * Returns the {@link Highlighter} registry
     */
    public Map<String, Highlighter> getHighlighters() {
        return highlighters;
    }

    /**
     * The registry of {@link MovAvgModel}s.
     */
    public ParseFieldRegistry<MovAvgModel.AbstractModelParser> getMovingAverageModelParserRegistry() {
        return movingAverageModelParserRegistry;
    }

    private ValuesSourceRegistry registerAggregations(List<SearchPlugin> plugins) {
        ValuesSourceRegistry.Builder builder = new ValuesSourceRegistry.Builder();
        registerAggregation(
            new AggregationSpec(AvgAggregationBuilder.NAME, AvgAggregationBuilder::new, AvgAggregationBuilder.PARSER).addResultReader(
                InternalAvg::new
            ).setAggregatorRegistrar(AvgAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                WeightedAvgAggregationBuilder.NAME,
                WeightedAvgAggregationBuilder::new,
                WeightedAvgAggregationBuilder.PARSER
            ).addResultReader(InternalWeightedAvg::new).setAggregatorRegistrar(WeightedAvgAggregationBuilder::registerUsage),
            builder
        );
        registerAggregation(
            new AggregationSpec(SumAggregationBuilder.NAME, SumAggregationBuilder::new, SumAggregationBuilder.PARSER).addResultReader(
                InternalSum::new
            ).setAggregatorRegistrar(SumAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(MinAggregationBuilder.NAME, MinAggregationBuilder::new, MinAggregationBuilder.PARSER).addResultReader(
                InternalMin::new
            ).setAggregatorRegistrar(MinAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(MaxAggregationBuilder.NAME, MaxAggregationBuilder::new, MaxAggregationBuilder.PARSER).addResultReader(
                InternalMax::new
            ).setAggregatorRegistrar(MaxAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(StatsAggregationBuilder.NAME, StatsAggregationBuilder::new, StatsAggregationBuilder.PARSER).addResultReader(
                InternalStats::new
            ).setAggregatorRegistrar(StatsAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                ExtendedStatsAggregationBuilder.NAME,
                ExtendedStatsAggregationBuilder::new,
                ExtendedStatsAggregationBuilder.PARSER
            ).addResultReader(InternalExtendedStats::new).setAggregatorRegistrar(ExtendedStatsAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(ValueCountAggregationBuilder.NAME, ValueCountAggregationBuilder::new, ValueCountAggregationBuilder.PARSER)
                .addResultReader(InternalValueCount::new)
                .setAggregatorRegistrar(ValueCountAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                PercentilesAggregationBuilder.NAME,
                PercentilesAggregationBuilder::new,
                PercentilesAggregationBuilder::parse
            ).addResultReader(InternalTDigestPercentiles.NAME, InternalTDigestPercentiles::new)
                .addResultReader(InternalHDRPercentiles.NAME, InternalHDRPercentiles::new)
                .setAggregatorRegistrar(PercentilesAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                PercentileRanksAggregationBuilder.NAME,
                PercentileRanksAggregationBuilder::new,
                PercentileRanksAggregationBuilder::parse
            ).addResultReader(InternalTDigestPercentileRanks.NAME, InternalTDigestPercentileRanks::new)
                .addResultReader(InternalHDRPercentileRanks.NAME, InternalHDRPercentileRanks::new)
                .setAggregatorRegistrar(PercentileRanksAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                MedianAbsoluteDeviationAggregationBuilder.NAME,
                MedianAbsoluteDeviationAggregationBuilder::new,
                MedianAbsoluteDeviationAggregationBuilder.PARSER
            ).addResultReader(InternalMedianAbsoluteDeviation::new)
                .setAggregatorRegistrar(MedianAbsoluteDeviationAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                CardinalityAggregationBuilder.NAME,
                CardinalityAggregationBuilder::new,
                CardinalityAggregationBuilder.PARSER
            ).addResultReader(InternalCardinality::new).setAggregatorRegistrar(CardinalityAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(GlobalAggregationBuilder.NAME, GlobalAggregationBuilder::new, GlobalAggregationBuilder::parse)
                .addResultReader(InternalGlobal::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(MissingAggregationBuilder.NAME, MissingAggregationBuilder::new, MissingAggregationBuilder.PARSER)
                .addResultReader(InternalMissing::new)
                .setAggregatorRegistrar(MissingAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(FilterAggregationBuilder.NAME, FilterAggregationBuilder::new, FilterAggregationBuilder::parse)
                .addResultReader(InternalFilter::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(FiltersAggregationBuilder.NAME, FiltersAggregationBuilder::new, FiltersAggregationBuilder::parse)
                .addResultReader(InternalFilters::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                AdjacencyMatrixAggregationBuilder.NAME,
                AdjacencyMatrixAggregationBuilder::new,
                AdjacencyMatrixAggregationBuilder::parse
            ).addResultReader(InternalAdjacencyMatrix::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(SamplerAggregationBuilder.NAME, SamplerAggregationBuilder::new, SamplerAggregationBuilder::parse)
                .addResultReader(InternalSampler.NAME, InternalSampler::new)
                .addResultReader(UnmappedSampler.NAME, UnmappedSampler::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                DiversifiedAggregationBuilder.NAME,
                DiversifiedAggregationBuilder::new,
                DiversifiedAggregationBuilder.PARSER
            ).setAggregatorRegistrar(DiversifiedAggregationBuilder::registerAggregators)
            /* Reuses result readers from SamplerAggregator*/,
            builder
        );
        registerAggregation(
            new AggregationSpec(TermsAggregationBuilder.NAME, TermsAggregationBuilder::new, TermsAggregationBuilder.PARSER).addResultReader(
                StringTerms.NAME,
                StringTerms::new
            )
                .addResultReader(UnmappedTerms.NAME, UnmappedTerms::new)
                .addResultReader(LongTerms.NAME, LongTerms::new)
                .addResultReader(DoubleTerms.NAME, DoubleTerms::new)
                .setAggregatorRegistrar(TermsAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(RareTermsAggregationBuilder.NAME, RareTermsAggregationBuilder::new, RareTermsAggregationBuilder.PARSER)
                .addResultReader(StringRareTerms.NAME, StringRareTerms::new)
                .addResultReader(UnmappedRareTerms.NAME, UnmappedRareTerms::new)
                .addResultReader(LongRareTerms.NAME, LongRareTerms::new)
                .setAggregatorRegistrar(RareTermsAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                SignificantTermsAggregationBuilder.NAME,
                SignificantTermsAggregationBuilder::new,
                SignificantTermsAggregationBuilder::parse
            ).addResultReader(SignificantStringTerms.NAME, SignificantStringTerms::new)
                .addResultReader(SignificantLongTerms.NAME, SignificantLongTerms::new)
                .addResultReader(UnmappedSignificantTerms.NAME, UnmappedSignificantTerms::new)
                .setAggregatorRegistrar(SignificantTermsAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                SignificantTextAggregationBuilder.NAME,
                SignificantTextAggregationBuilder::new,
                SignificantTextAggregationBuilder::parse
            ),
            builder
        );
        registerAggregation(
            new AggregationSpec(RangeAggregationBuilder.NAME, RangeAggregationBuilder::new, RangeAggregationBuilder.PARSER).addResultReader(
                InternalRange::new
            ).setAggregatorRegistrar(RangeAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(DateRangeAggregationBuilder.NAME, DateRangeAggregationBuilder::new, DateRangeAggregationBuilder.PARSER)
                .addResultReader(InternalDateRange::new)
                .setAggregatorRegistrar(DateRangeAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(IpRangeAggregationBuilder.NAME, IpRangeAggregationBuilder::new, IpRangeAggregationBuilder.PARSER)
                .addResultReader(InternalBinaryRange::new)
                .setAggregatorRegistrar(IpRangeAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(HistogramAggregationBuilder.NAME, HistogramAggregationBuilder::new, HistogramAggregationBuilder.PARSER)
                .addResultReader(InternalHistogram::new)
                .setAggregatorRegistrar(HistogramAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                DateHistogramAggregationBuilder.NAME,
                DateHistogramAggregationBuilder::new,
                DateHistogramAggregationBuilder.PARSER
            ).addResultReader(InternalDateHistogram::new).setAggregatorRegistrar(DateHistogramAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                AutoDateHistogramAggregationBuilder.NAME,
                AutoDateHistogramAggregationBuilder::new,
                AutoDateHistogramAggregationBuilder.PARSER
            ).addResultReader(InternalAutoDateHistogram::new)
                .setAggregatorRegistrar(AutoDateHistogramAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                VariableWidthHistogramAggregationBuilder.NAME,
                VariableWidthHistogramAggregationBuilder::new,
                VariableWidthHistogramAggregationBuilder.PARSER
            ).addResultReader(InternalVariableWidthHistogram::new)
                .setAggregatorRegistrar(VariableWidthHistogramAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                GeoDistanceAggregationBuilder.NAME,
                GeoDistanceAggregationBuilder::new,
                GeoDistanceAggregationBuilder::parse
            ).addResultReader(InternalGeoDistance::new).setAggregatorRegistrar(GeoDistanceAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(NestedAggregationBuilder.NAME, NestedAggregationBuilder::new, NestedAggregationBuilder::parse)
                .addResultReader(InternalNested::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                ReverseNestedAggregationBuilder.NAME,
                ReverseNestedAggregationBuilder::new,
                ReverseNestedAggregationBuilder::parse
            ).addResultReader(InternalReverseNested::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(TopHitsAggregationBuilder.NAME, TopHitsAggregationBuilder::new, TopHitsAggregationBuilder::parse)
                .addResultReader(InternalTopHits::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                GeoCentroidAggregationBuilder.NAME,
                GeoCentroidAggregationBuilder::new,
                GeoCentroidAggregationBuilder.PARSER
            ).addResultReader(InternalGeoCentroid::new).setAggregatorRegistrar(GeoCentroidAggregationBuilder::registerAggregators),
            builder
        );
        registerAggregation(
            new AggregationSpec(
                ScriptedMetricAggregationBuilder.NAME,
                ScriptedMetricAggregationBuilder::new,
                ScriptedMetricAggregationBuilder.PARSER
            ).addResultReader(InternalScriptedMetric::new),
            builder
        );
        registerAggregation(
            new AggregationSpec(CompositeAggregationBuilder.NAME, CompositeAggregationBuilder::new, CompositeAggregationBuilder.PARSER)
                .addResultReader(InternalComposite::new)
                .setAggregatorRegistrar(reg -> CompositeAggregationBuilder.registerAggregators(reg, plugins)),
            builder
        );
        registerAggregation(
            new AggregationSpec(MultiTermsAggregationBuilder.NAME, MultiTermsAggregationBuilder::new, MultiTermsAggregationBuilder.PARSER)
                .addResultReader(InternalMultiTerms::new)
                .setAggregatorRegistrar(MultiTermsAggregationFactory::registerAggregators),
            builder
        );
        registerFromPlugin(plugins, SearchPlugin::getAggregations, (agg) -> this.registerAggregation(agg, builder));

        // after aggs have been registered, see if there are any new VSTypes that need to be linked to core fields
        registerFromPlugin(plugins, SearchPlugin::getAggregationExtentions, (registrar) -> {
            if (registrar != null) {
                registrar.accept(builder);
            }
        });

        return builder.build();
    }

    private void registerAggregation(AggregationSpec spec, ValuesSourceRegistry.Builder builder) {
        namedXContents.add(new NamedXContentRegistry.Entry(BaseAggregationBuilder.class, spec.getName(), (p, c) -> {
            String name = (String) c;
            return spec.getParser().parse(p, name);
        }));
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(AggregationBuilder.class, spec.getName().getPreferredName(), spec.getReader())
        );
        for (Map.Entry<String, Writeable.Reader<? extends InternalAggregation>> t : spec.getResultReaders().entrySet()) {
            String writeableName = t.getKey();
            Writeable.Reader<? extends InternalAggregation> internalReader = t.getValue();
            namedWriteables.add(new NamedWriteableRegistry.Entry(InternalAggregation.class, writeableName, internalReader));
        }
        Consumer<ValuesSourceRegistry.Builder> register = spec.getAggregatorRegistrar();
        if (register != null) {
            register.accept(builder);
        } else {
            // Register is typically handling usage registration, but for the older aggregations that don't use register, we
            // have to register usage explicitly here.
            builder.registerUsage(spec.getName().getPreferredName());
        }
    }

    private void registerPipelineAggregations(List<SearchPlugin> plugins) {
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                DerivativePipelineAggregationBuilder.NAME,
                DerivativePipelineAggregationBuilder::new,
                DerivativePipelineAggregator::new,
                DerivativePipelineAggregationBuilder::parse
            ).addResultReader(InternalDerivative::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                MaxBucketPipelineAggregationBuilder.NAME,
                MaxBucketPipelineAggregationBuilder::new,
                MaxBucketPipelineAggregator::new,
                MaxBucketPipelineAggregationBuilder.PARSER
            )
                // This bucket is used by many pipeline aggreations.
                .addResultReader(InternalBucketMetricValue.NAME, InternalBucketMetricValue::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                MinBucketPipelineAggregationBuilder.NAME,
                MinBucketPipelineAggregationBuilder::new,
                MinBucketPipelineAggregator::new,
                MinBucketPipelineAggregationBuilder.PARSER
            )
            /* Uses InternalBucketMetricValue */
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                AvgBucketPipelineAggregationBuilder.NAME,
                AvgBucketPipelineAggregationBuilder::new,
                AvgBucketPipelineAggregator::new,
                AvgBucketPipelineAggregationBuilder.PARSER
            )
                // This bucket is used by many pipeline aggreations.
                .addResultReader(InternalSimpleValue.NAME, InternalSimpleValue::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                SumBucketPipelineAggregationBuilder.NAME,
                SumBucketPipelineAggregationBuilder::new,
                SumBucketPipelineAggregator::new,
                SumBucketPipelineAggregationBuilder.PARSER
            )
            /* Uses InternalSimpleValue */
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                StatsBucketPipelineAggregationBuilder.NAME,
                StatsBucketPipelineAggregationBuilder::new,
                StatsBucketPipelineAggregator::new,
                StatsBucketPipelineAggregationBuilder.PARSER
            ).addResultReader(InternalStatsBucket::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                ExtendedStatsBucketPipelineAggregationBuilder.NAME,
                ExtendedStatsBucketPipelineAggregationBuilder::new,
                ExtendedStatsBucketPipelineAggregator::new,
                new ExtendedStatsBucketParser()
            ).addResultReader(InternalExtendedStatsBucket::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                PercentilesBucketPipelineAggregationBuilder.NAME,
                PercentilesBucketPipelineAggregationBuilder::new,
                PercentilesBucketPipelineAggregator::new,
                PercentilesBucketPipelineAggregationBuilder.PARSER
            ).addResultReader(InternalPercentilesBucket::new)
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                MovAvgPipelineAggregationBuilder.NAME,
                MovAvgPipelineAggregationBuilder::new,
                MovAvgPipelineAggregator::new,
                (XContentParser parser, String name) -> MovAvgPipelineAggregationBuilder.parse(
                    movingAverageModelParserRegistry,
                    name,
                    parser
                )
            )/* Uses InternalHistogram for buckets */
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                CumulativeSumPipelineAggregationBuilder.NAME,
                CumulativeSumPipelineAggregationBuilder::new,
                CumulativeSumPipelineAggregator::new,
                CumulativeSumPipelineAggregationBuilder.PARSER
            )
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                BucketScriptPipelineAggregationBuilder.NAME,
                BucketScriptPipelineAggregationBuilder::new,
                BucketScriptPipelineAggregator::new,
                BucketScriptPipelineAggregationBuilder.PARSER
            )
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                BucketSelectorPipelineAggregationBuilder.NAME,
                BucketSelectorPipelineAggregationBuilder::new,
                BucketSelectorPipelineAggregator::new,
                BucketSelectorPipelineAggregationBuilder::parse
            )
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                BucketSortPipelineAggregationBuilder.NAME,
                BucketSortPipelineAggregationBuilder::new,
                BucketSortPipelineAggregator::new,
                BucketSortPipelineAggregationBuilder::parse
            )
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                SerialDiffPipelineAggregationBuilder.NAME,
                SerialDiffPipelineAggregationBuilder::new,
                SerialDiffPipelineAggregator::new,
                SerialDiffPipelineAggregationBuilder::parse
            )
        );
        registerPipelineAggregation(
            new PipelineAggregationSpec(
                MovFnPipelineAggregationBuilder.NAME,
                MovFnPipelineAggregationBuilder::new,
                MovFnPipelineAggregator::new,
                MovFnPipelineAggregationBuilder.PARSER
            )
        );

        registerFromPlugin(plugins, SearchPlugin::getPipelineAggregations, this::registerPipelineAggregation);
    }

    private void registerPipelineAggregation(PipelineAggregationSpec spec) {
        namedXContents.add(
            new NamedXContentRegistry.Entry(BaseAggregationBuilder.class, spec.getName(), (p, c) -> spec.getParser().parse(p, (String) c))
        );
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(PipelineAggregationBuilder.class, spec.getName().getPreferredName(), spec.getReader())
        );
        if (spec.getAggregatorReader() != null) {
            namedWriteables.add(
                new NamedWriteableRegistry.Entry(PipelineAggregator.class, spec.getName().getPreferredName(), spec.getAggregatorReader())
            );
        }
        for (Map.Entry<String, Writeable.Reader<? extends InternalAggregation>> resultReader : spec.getResultReaders().entrySet()) {
            namedWriteables.add(
                new NamedWriteableRegistry.Entry(InternalAggregation.class, resultReader.getKey(), resultReader.getValue())
            );
        }
    }

    private void registerShapes() {
        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            namedWriteables.addAll(GeoShapeType.getShapeWriteables());
        }
    }

    private void registerRescorers(List<SearchPlugin> plugins) {
        registerRescorer(new RescorerSpec<>(QueryRescorerBuilder.NAME, QueryRescorerBuilder::new, QueryRescorerBuilder::fromXContent));
        registerFromPlugin(plugins, SearchPlugin::getRescorers, this::registerRescorer);
    }

    private void registerRescorer(RescorerSpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(RescorerBuilder.class, spec.getName(), (p, c) -> spec.getParser().apply(p)));
        namedWriteables.add(new NamedWriteableRegistry.Entry(RescorerBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
    }

    private <T> void registerFromPlugin(List<SearchPlugin> plugins, Function<SearchPlugin, List<T>> producer, Consumer<T> consumer) {
        for (SearchPlugin plugin : plugins) {
            for (T t : producer.apply(plugin)) {
                consumer.accept(t);
            }
        }
    }

    public static void registerSmoothingModels(List<Entry> namedWriteables) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, Laplace.NAME, Laplace::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, LinearInterpolation.NAME, LinearInterpolation::new));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SmoothingModel.class, StupidBackoff.NAME, StupidBackoff::new));
    }

    private void registerSuggesters(List<SearchPlugin> plugins) {
        registerSmoothingModels(namedWriteables);

        registerSuggester(
            new SuggesterSpec<>(
                TermSuggestionBuilder.SUGGESTION_NAME,
                TermSuggestionBuilder::new,
                TermSuggestionBuilder::fromXContent,
                TermSuggestion::new
            )
        );

        registerSuggester(
            new SuggesterSpec<>(
                PhraseSuggestionBuilder.SUGGESTION_NAME,
                PhraseSuggestionBuilder::new,
                PhraseSuggestionBuilder::fromXContent,
                PhraseSuggestion::new
            )
        );

        registerSuggester(
            new SuggesterSpec<>(
                CompletionSuggestionBuilder.SUGGESTION_NAME,
                CompletionSuggestionBuilder::new,
                CompletionSuggestionBuilder::fromXContent,
                CompletionSuggestion::new
            )
        );

        registerFromPlugin(plugins, SearchPlugin::getSuggesters, this::registerSuggester);
    }

    private void registerSuggester(SuggesterSpec<?> suggester) {
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(SuggestionBuilder.class, suggester.getName().getPreferredName(), suggester.getReader())
        );
        namedXContents.add(new NamedXContentRegistry.Entry(SuggestionBuilder.class, suggester.getName(), suggester.getParser()));

        namedWriteables.add(
            new NamedWriteableRegistry.Entry(
                Suggest.Suggestion.class,
                suggester.getName().getPreferredName(),
                suggester.getSuggestionReader()
            )
        );
    }

    private Map<String, Highlighter> setupHighlighters(Settings settings, List<SearchPlugin> plugins) {
        NamedRegistry<Highlighter> highlighters = new NamedRegistry<>("highlighter");
        highlighters.register("fvh", new FastVectorHighlighter(settings));
        highlighters.register("plain", new PlainHighlighter());
        highlighters.register("unified", new UnifiedHighlighter());
        highlighters.extractAndRegister(plugins, SearchPlugin::getHighlighters);

        return unmodifiableMap(highlighters.getRegistry());
    }

    private void registerScoreFunctions(List<SearchPlugin> plugins) {
        // ScriptScoreFunctionBuilder has it own named writable because of a new script_score query
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(
                ScriptScoreFunctionBuilder.class,
                ScriptScoreFunctionBuilder.NAME,
                ScriptScoreFunctionBuilder::new
            )
        );
        registerScoreFunction(
            new ScoreFunctionSpec<>(
                ScriptScoreFunctionBuilder.NAME,
                ScriptScoreFunctionBuilder::new,
                ScriptScoreFunctionBuilder::fromXContent
            )
        );

        registerScoreFunction(
            new ScoreFunctionSpec<>(GaussDecayFunctionBuilder.NAME, GaussDecayFunctionBuilder::new, GaussDecayFunctionBuilder.PARSER)
        );
        registerScoreFunction(
            new ScoreFunctionSpec<>(LinearDecayFunctionBuilder.NAME, LinearDecayFunctionBuilder::new, LinearDecayFunctionBuilder.PARSER)
        );
        registerScoreFunction(
            new ScoreFunctionSpec<>(
                ExponentialDecayFunctionBuilder.NAME,
                ExponentialDecayFunctionBuilder::new,
                ExponentialDecayFunctionBuilder.PARSER
            )
        );
        registerScoreFunction(
            new ScoreFunctionSpec<>(
                RandomScoreFunctionBuilder.NAME,
                RandomScoreFunctionBuilder::new,
                RandomScoreFunctionBuilder::fromXContent
            )
        );
        registerScoreFunction(
            new ScoreFunctionSpec<>(
                FieldValueFactorFunctionBuilder.NAME,
                FieldValueFactorFunctionBuilder::new,
                FieldValueFactorFunctionBuilder::fromXContent
            )
        );

        // weight doesn't have its own parser, so every function supports it out of the box.
        // Can be a single function too when not associated to any other function, which is why it needs to be registered manually here.
        namedWriteables.add(new NamedWriteableRegistry.Entry(ScoreFunctionBuilder.class, WeightBuilder.NAME, WeightBuilder::new));

        registerFromPlugin(plugins, SearchPlugin::getScoreFunctions, this::registerScoreFunction);
    }

    private void registerScoreFunction(ScoreFunctionSpec<?> scoreFunction) {
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(
                ScoreFunctionBuilder.class,
                scoreFunction.getName().getPreferredName(),
                scoreFunction.getReader()
            )
        );
        // TODO remove funky contexts
        namedXContents.add(
            new NamedXContentRegistry.Entry(
                ScoreFunctionBuilder.class,
                scoreFunction.getName(),
                (XContentParser p, Object c) -> scoreFunction.getParser().fromXContent(p)
            )
        );
    }

    private void registerValueFormats() {
        registerValueFormat(DocValueFormat.BOOLEAN.getWriteableName(), in -> DocValueFormat.BOOLEAN);
        registerValueFormat(DocValueFormat.DateTime.NAME, DocValueFormat.DateTime::new);
        registerValueFormat(DocValueFormat.Decimal.NAME, DocValueFormat.Decimal::new);
        registerValueFormat(DocValueFormat.GEOHASH.getWriteableName(), in -> DocValueFormat.GEOHASH);
        registerValueFormat(DocValueFormat.GEOTILE.getWriteableName(), in -> DocValueFormat.GEOTILE);
        registerValueFormat(DocValueFormat.IP.getWriteableName(), in -> DocValueFormat.IP);
        registerValueFormat(DocValueFormat.RAW.getWriteableName(), in -> DocValueFormat.RAW);
        registerValueFormat(DocValueFormat.BINARY.getWriteableName(), in -> DocValueFormat.BINARY);
        registerValueFormat(DocValueFormat.UNSIGNED_LONG_SHIFTED.getWriteableName(), in -> DocValueFormat.UNSIGNED_LONG_SHIFTED);
    }

    /**
     * Register a new ValueFormat.
     */
    private void registerValueFormat(String name, Writeable.Reader<? extends DocValueFormat> reader) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(DocValueFormat.class, name, reader));
    }

    private void registerSignificanceHeuristics(List<SearchPlugin> plugins) {
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(ChiSquare.NAME, ChiSquare::new, ChiSquare.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(GND.NAME, GND::new, GND.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(JLHScore.NAME, JLHScore::new, JLHScore.PARSER));
        registerSignificanceHeuristic(
            new SignificanceHeuristicSpec<>(MutualInformation.NAME, MutualInformation::new, MutualInformation.PARSER)
        );
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(PercentageScore.NAME, PercentageScore::new, PercentageScore.PARSER));
        registerSignificanceHeuristic(new SignificanceHeuristicSpec<>(ScriptHeuristic.NAME, ScriptHeuristic::new, ScriptHeuristic.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getSignificanceHeuristics, this::registerSignificanceHeuristic);
    }

    private <T extends SignificanceHeuristic> void registerSignificanceHeuristic(SignificanceHeuristicSpec<?> spec) {
        namedXContents.add(
            new NamedXContentRegistry.Entry(SignificanceHeuristic.class, spec.getName(), p -> spec.getParser().apply(p, null))
        );
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(SignificanceHeuristic.class, spec.getName().getPreferredName(), spec.getReader())
        );
    }

    private void registerMovingAverageModels(List<SearchPlugin> plugins) {
        registerMovingAverageModel(new SearchExtensionSpec<>(SimpleModel.NAME, SimpleModel::new, SimpleModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(LinearModel.NAME, LinearModel::new, LinearModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(EwmaModel.NAME, EwmaModel::new, EwmaModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(HoltLinearModel.NAME, HoltLinearModel::new, HoltLinearModel.PARSER));
        registerMovingAverageModel(new SearchExtensionSpec<>(HoltWintersModel.NAME, HoltWintersModel::new, HoltWintersModel.PARSER));

        registerFromPlugin(plugins, SearchPlugin::getMovingAverageModels, this::registerMovingAverageModel);
    }

    private void registerMovingAverageModel(SearchExtensionSpec<MovAvgModel, MovAvgModel.AbstractModelParser> movAvgModel) {
        movingAverageModelParserRegistry.register(movAvgModel.getParser(), movAvgModel.getName());
        namedWriteables.add(
            new NamedWriteableRegistry.Entry(MovAvgModel.class, movAvgModel.getName().getPreferredName(), movAvgModel.getReader())
        );
    }

    private void registerFetchSubPhases(List<SearchPlugin> plugins) {
        registerFetchSubPhase(new ExplainPhase());
        registerFetchSubPhase(new FetchDocValuesPhase());
        registerFetchSubPhase(new ScriptFieldsPhase());
        registerFetchSubPhase(new FetchSourcePhase());
        registerFetchSubPhase(new FetchFieldsPhase());
        registerFetchSubPhase(new FetchVersionPhase());
        registerFetchSubPhase(new SeqNoPrimaryTermPhase());
        registerFetchSubPhase(new MatchedQueriesPhase());
        registerFetchSubPhase(new HighlightPhase(highlighters));
        registerFetchSubPhase(new FetchScorePhase());

        FetchPhaseConstructionContext context = new FetchPhaseConstructionContext(highlighters);
        registerFromPlugin(plugins, p -> p.getFetchSubPhases(context), this::registerFetchSubPhase);
    }

    private void registerSearchExts(List<SearchPlugin> plugins) {
        registerFromPlugin(plugins, SearchPlugin::getSearchExts, this::registerSearchExt);
    }

    private void registerSearchExt(SearchExtSpec<?> spec) {
        namedXContents.add(new NamedXContentRegistry.Entry(SearchExtBuilder.class, spec.getName(), spec.getParser()));
        namedWriteables.add(new NamedWriteableRegistry.Entry(SearchExtBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
    }

    private void registerFetchSubPhase(FetchSubPhase subPhase) {
        Class<?> subPhaseClass = subPhase.getClass();
        if (fetchSubPhases.stream().anyMatch(p -> p.getClass().equals(subPhaseClass))) {
            throw new IllegalArgumentException("FetchSubPhase [" + subPhaseClass + "] already registered");
        }
        fetchSubPhases.add(requireNonNull(subPhase, "FetchSubPhase must not be null"));
    }

    private void registerQueryParsers(List<SearchPlugin> plugins) {
        registerQuery(new QuerySpec<>(MatchQueryBuilder.NAME, MatchQueryBuilder::new, MatchQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchPhraseQueryBuilder.NAME, MatchPhraseQueryBuilder::new, MatchPhraseQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(
                MatchPhrasePrefixQueryBuilder.NAME,
                MatchPhrasePrefixQueryBuilder::new,
                MatchPhrasePrefixQueryBuilder::fromXContent
            )
        );
        registerQuery(new QuerySpec<>(MultiMatchQueryBuilder.NAME, MultiMatchQueryBuilder::new, MultiMatchQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(NestedQueryBuilder.NAME, NestedQueryBuilder::new, NestedQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(DisMaxQueryBuilder.NAME, DisMaxQueryBuilder::new, DisMaxQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(IdsQueryBuilder.NAME, IdsQueryBuilder::new, IdsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchAllQueryBuilder.NAME, MatchAllQueryBuilder::new, MatchAllQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(QueryStringQueryBuilder.NAME, QueryStringQueryBuilder::new, QueryStringQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(BoostingQueryBuilder.NAME, BoostingQueryBuilder::new, BoostingQueryBuilder::fromXContent));
        BooleanQuery.setMaxClauseCount(INDICES_MAX_CLAUSE_COUNT_SETTING.get(settings));
        registerQuery(new QuerySpec<>(BoolQueryBuilder.NAME, BoolQueryBuilder::new, BoolQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermQueryBuilder.NAME, TermQueryBuilder::new, TermQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermsQueryBuilder.NAME, TermsQueryBuilder::new, TermsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(FuzzyQueryBuilder.NAME, FuzzyQueryBuilder::new, FuzzyQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(RegexpQueryBuilder.NAME, RegexpQueryBuilder::new, RegexpQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(RangeQueryBuilder.NAME, RangeQueryBuilder::new, RangeQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(PrefixQueryBuilder.NAME, PrefixQueryBuilder::new, PrefixQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(WildcardQueryBuilder.NAME, WildcardQueryBuilder::new, WildcardQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(ConstantScoreQueryBuilder.NAME, ConstantScoreQueryBuilder::new, ConstantScoreQueryBuilder::fromXContent)
        );
        registerQuery(new QuerySpec<>(SpanTermQueryBuilder.NAME, SpanTermQueryBuilder::new, SpanTermQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanNotQueryBuilder.NAME, SpanNotQueryBuilder::new, SpanNotQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanWithinQueryBuilder.NAME, SpanWithinQueryBuilder::new, SpanWithinQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(SpanContainingQueryBuilder.NAME, SpanContainingQueryBuilder::new, SpanContainingQueryBuilder::fromXContent)
        );
        registerQuery(
            new QuerySpec<>(
                FieldMaskingSpanQueryBuilder.SPAN_FIELD_MASKING_FIELD,
                FieldMaskingSpanQueryBuilder::new,
                FieldMaskingSpanQueryBuilder::fromXContent
            )
        );
        registerQuery(new QuerySpec<>(SpanFirstQueryBuilder.NAME, SpanFirstQueryBuilder::new, SpanFirstQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanNearQueryBuilder.NAME, SpanNearQueryBuilder::new, SpanNearQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanGapQueryBuilder.NAME, SpanGapQueryBuilder::new, SpanGapQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(SpanOrQueryBuilder.NAME, SpanOrQueryBuilder::new, SpanOrQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(MoreLikeThisQueryBuilder.NAME, MoreLikeThisQueryBuilder::new, MoreLikeThisQueryBuilder::fromXContent)
        );
        registerQuery(new QuerySpec<>(WrapperQueryBuilder.NAME, WrapperQueryBuilder::new, WrapperQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(
                new ParseField(CommonTermsQueryBuilder.NAME).withAllDeprecated(COMMON_TERMS_QUERY_DEPRECATION_MSG),
                CommonTermsQueryBuilder::new,
                CommonTermsQueryBuilder::fromXContent
            )
        );
        registerQuery(
            new QuerySpec<>(SpanMultiTermQueryBuilder.NAME, SpanMultiTermQueryBuilder::new, SpanMultiTermQueryBuilder::fromXContent)
        );
        registerQuery(
            new QuerySpec<>(FunctionScoreQueryBuilder.NAME, FunctionScoreQueryBuilder::new, FunctionScoreQueryBuilder::fromXContent)
        );
        registerQuery(new QuerySpec<>(ScriptScoreQueryBuilder.NAME, ScriptScoreQueryBuilder::new, ScriptScoreQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(SimpleQueryStringBuilder.NAME, SimpleQueryStringBuilder::new, SimpleQueryStringBuilder::fromXContent)
        );
        registerQuery(new QuerySpec<>(ScriptQueryBuilder.NAME, ScriptQueryBuilder::new, ScriptQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(GeoDistanceQueryBuilder.NAME, GeoDistanceQueryBuilder::new, GeoDistanceQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(GeoBoundingBoxQueryBuilder.NAME, GeoBoundingBoxQueryBuilder::new, GeoBoundingBoxQueryBuilder::fromXContent)
        );
        registerQuery(new QuerySpec<>(GeoPolygonQueryBuilder.NAME, GeoPolygonQueryBuilder::new, GeoPolygonQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(ExistsQueryBuilder.NAME, ExistsQueryBuilder::new, ExistsQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(MatchNoneQueryBuilder.NAME, MatchNoneQueryBuilder::new, MatchNoneQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(TermsSetQueryBuilder.NAME, TermsSetQueryBuilder::new, TermsSetQueryBuilder::fromXContent));
        registerQuery(new QuerySpec<>(IntervalQueryBuilder.NAME, IntervalQueryBuilder::new, IntervalQueryBuilder::fromXContent));
        registerQuery(
            new QuerySpec<>(DistanceFeatureQueryBuilder.NAME, DistanceFeatureQueryBuilder::new, DistanceFeatureQueryBuilder::fromXContent)
        );
        registerQuery(
            new QuerySpec<>(MatchBoolPrefixQueryBuilder.NAME, MatchBoolPrefixQueryBuilder::new, MatchBoolPrefixQueryBuilder::fromXContent)
        );

        if (ShapesAvailability.JTS_AVAILABLE && ShapesAvailability.SPATIAL4J_AVAILABLE) {
            registerQuery(new QuerySpec<>(GeoShapeQueryBuilder.NAME, GeoShapeQueryBuilder::new, GeoShapeQueryBuilder::fromXContent));
        }

        registerFromPlugin(plugins, SearchPlugin::getQueries, this::registerQuery);
    }

    private void registerSortParsers(List<SearchPlugin> plugins) {
        registerSort(new SortSpec<>(FieldSortBuilder.NAME, FieldSortBuilder::new, FieldSortBuilder::fromXContentObject));
        registerSort(new SortSpec<>(ScriptSortBuilder.NAME, ScriptSortBuilder::new, ScriptSortBuilder::fromXContent));
        registerSort(
            new SortSpec<>(
                new ParseField(GeoDistanceSortBuilder.NAME, GeoDistanceSortBuilder.ALTERNATIVE_NAME),
                GeoDistanceSortBuilder::new,
                GeoDistanceSortBuilder::fromXContent
            )
        );
        registerSort(new SortSpec<>(ScoreSortBuilder.NAME, ScoreSortBuilder::new, ScoreSortBuilder::fromXContent));
        registerFromPlugin(plugins, SearchPlugin::getSorts, this::registerSort);
    }

    private void registerIntervalsSourceProviders() {
        namedWriteables.addAll(getIntervalsSourceProviderNamedWritables());
    }

    public static List<NamedWriteableRegistry.Entry> getIntervalsSourceProviderNamedWritables() {
        return unmodifiableList(
            Arrays.asList(
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Match.NAME,
                    IntervalsSourceProvider.Match::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Combine.NAME,
                    IntervalsSourceProvider.Combine::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Disjunction.NAME,
                    IntervalsSourceProvider.Disjunction::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Prefix.NAME,
                    IntervalsSourceProvider.Prefix::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Wildcard.NAME,
                    IntervalsSourceProvider.Wildcard::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Regexp.NAME,
                    IntervalsSourceProvider.Regexp::new
                ),
                new NamedWriteableRegistry.Entry(
                    IntervalsSourceProvider.class,
                    IntervalsSourceProvider.Fuzzy.NAME,
                    IntervalsSourceProvider.Fuzzy::new
                )
            )
        );
    }

    private void registerQuery(QuerySpec<?> spec) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(QueryBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
        namedXContents.add(new NamedXContentRegistry.Entry(QueryBuilder.class, spec.getName(), (p, c) -> spec.getParser().fromXContent(p)));
    }

    private void registerSort(SortSpec<?> spec) {
        namedWriteables.add(new NamedWriteableRegistry.Entry(SortBuilder.class, spec.getName().getPreferredName(), spec.getReader()));
        namedXContents.add(
            new NamedXContentRegistry.Entry(
                SortBuilder.class,
                spec.getName(),
                (p, c) -> spec.getParser().fromXContent(p, spec.getName().getPreferredName())
            )
        );
    }

    private QueryPhaseSearcher registerQueryPhaseSearcher(List<SearchPlugin> plugins) {
        QueryPhaseSearcher searcher = null;

        for (SearchPlugin plugin : plugins) {
            final Optional<QueryPhaseSearcher> searcherOpt = plugin.getQueryPhaseSearcher();

            if (searcher == null) {
                searcher = searcherOpt.orElse(null);
            } else if (searcherOpt.isPresent()) {
                throw new IllegalStateException("Only one QueryPhaseSearcher is allowed, but more than one are provided by the plugins");
            }
        }

        return searcher;
    }

    private SearchPlugin.ExecutorServiceProvider registerIndexSearcherExecutorProvider(List<SearchPlugin> plugins) {
        SearchPlugin.ExecutorServiceProvider provider = null;

        for (SearchPlugin plugin : plugins) {
            final Optional<SearchPlugin.ExecutorServiceProvider> providerOpt = plugin.getIndexSearcherExecutorProvider();

            if (provider == null) {
                provider = providerOpt.orElse(null);
            } else if (providerOpt.isPresent()) {
                throw new IllegalStateException(
                    "The index searcher executor is already assigned but more than one are provided by the plugins"
                );
            }
        }

        return provider;
    }

    public FetchPhase getFetchPhase() {
        return new FetchPhase(fetchSubPhases);
    }

    public QueryPhase getQueryPhase() {
        return (queryPhaseSearcher == null) ? new QueryPhase() : new QueryPhase(queryPhaseSearcher);
    }

    public @Nullable ExecutorService getIndexSearcherExecutor(ThreadPool pool) {
        return (indexSearcherExecutorProvider == null) ? null : indexSearcherExecutorProvider.getExecutor(pool);
    }
}
