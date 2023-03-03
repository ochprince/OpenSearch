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

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.CharsRefBuilder;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.xcontent.LoggingDeprecationHandler;
import com.colasoft.opensearch.common.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.query.QueryRewriteContext;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.index.query.functionscore.GaussDecayFunctionBuilder;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.search.aggregations.AggregationBuilder;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories.Builder;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.BaseAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.InternalAggregation;
import com.colasoft.opensearch.search.aggregations.InternalAggregation.ReduceContext;
import com.colasoft.opensearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.bucket.terms.heuristic.ChiSquare;
import com.colasoft.opensearch.search.aggregations.pipeline.AbstractPipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.DerivativePipelineAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.pipeline.DerivativePipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalDerivative;
import com.colasoft.opensearch.search.aggregations.pipeline.MovAvgModel;
import com.colasoft.opensearch.search.aggregations.pipeline.PipelineAggregator;
import com.colasoft.opensearch.search.aggregations.pipeline.SimpleModel;
import com.colasoft.opensearch.search.aggregations.support.CoreValuesSourceType;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceRegistry;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceType;
import com.colasoft.opensearch.search.fetch.FetchSubPhase;
import com.colasoft.opensearch.search.fetch.subphase.ExplainPhase;
import com.colasoft.opensearch.search.fetch.subphase.highlight.CustomHighlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.FastVectorHighlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.Highlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.PlainHighlighter;
import com.colasoft.opensearch.search.fetch.subphase.highlight.UnifiedHighlighter;
import com.colasoft.opensearch.search.rescore.QueryRescorerBuilder;
import com.colasoft.opensearch.search.rescore.RescoreContext;
import com.colasoft.opensearch.search.rescore.RescorerBuilder;
import com.colasoft.opensearch.search.suggest.Suggest.Suggestion;
import com.colasoft.opensearch.search.suggest.Suggest.Suggestion.Entry;
import com.colasoft.opensearch.search.suggest.Suggest.Suggestion.Entry.Option;
import com.colasoft.opensearch.search.suggest.Suggester;
import com.colasoft.opensearch.search.suggest.SuggestionBuilder;
import com.colasoft.opensearch.search.suggest.SuggestionSearchContext;
import com.colasoft.opensearch.search.suggest.SuggestionSearchContext.SuggestionContext;
import com.colasoft.opensearch.search.suggest.term.TermSuggestion;
import com.colasoft.opensearch.search.suggest.term.TermSuggestionBuilder;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class SearchModuleTests extends OpenSearchTestCase {

    public void testDoubleRegister() {
        SearchPlugin registersDupeHighlighter = new SearchPlugin() {
            @Override
            public Map<String, Highlighter> getHighlighters() {
                return singletonMap("plain", new PlainHighlighter());
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeHighlighter));

        SearchPlugin registersDupeSuggester = new SearchPlugin() {
            @Override
            public List<SearchPlugin.SuggesterSpec<?>> getSuggesters() {
                return singletonList(
                    new SuggesterSpec<>(
                        TermSuggestionBuilder.SUGGESTION_NAME,
                        TermSuggestionBuilder::new,
                        TermSuggestionBuilder::fromXContent,
                        TermSuggestion::new
                    )
                );
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeSuggester));

        SearchPlugin registersDupeScoreFunction = new SearchPlugin() {
            @Override
            public List<ScoreFunctionSpec<?>> getScoreFunctions() {
                return singletonList(
                    new ScoreFunctionSpec<>(
                        GaussDecayFunctionBuilder.NAME,
                        GaussDecayFunctionBuilder::new,
                        GaussDecayFunctionBuilder.PARSER
                    )
                );
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeScoreFunction));

        SearchPlugin registersDupeSignificanceHeuristic = new SearchPlugin() {
            @Override
            public List<SignificanceHeuristicSpec<?>> getSignificanceHeuristics() {
                return singletonList(new SignificanceHeuristicSpec<>(ChiSquare.NAME, ChiSquare::new, ChiSquare.PARSER));
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeSignificanceHeuristic));

        SearchPlugin registersDupeMovAvgModel = new SearchPlugin() {
            @Override
            public List<SearchExtensionSpec<MovAvgModel, MovAvgModel.AbstractModelParser>> getMovingAverageModels() {
                return singletonList(new SearchExtensionSpec<>(SimpleModel.NAME, SimpleModel::new, SimpleModel.PARSER));
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeMovAvgModel));

        SearchPlugin registersDupeFetchSubPhase = new SearchPlugin() {
            @Override
            public List<FetchSubPhase> getFetchSubPhases(FetchPhaseConstructionContext context) {
                return singletonList(new ExplainPhase());
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeFetchSubPhase));

        SearchPlugin registersDupeQuery = new SearchPlugin() {
            @Override
            public List<SearchPlugin.QuerySpec<?>> getQueries() {
                return singletonList(new QuerySpec<>(TermQueryBuilder.NAME, TermQueryBuilder::new, TermQueryBuilder::fromXContent));
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeQuery));

        SearchPlugin registersDupeAggregation = new SearchPlugin() {
            @Override
            public List<AggregationSpec> getAggregations() {
                return singletonList(
                    new AggregationSpec(TermsAggregationBuilder.NAME, TermsAggregationBuilder::new, TermsAggregationBuilder.PARSER)
                );
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeAggregation));

        SearchPlugin registersDupePipelineAggregation = new SearchPlugin() {
            @Override
            public List<PipelineAggregationSpec> getPipelineAggregations() {
                return singletonList(
                    new PipelineAggregationSpec(
                        DerivativePipelineAggregationBuilder.NAME,
                        DerivativePipelineAggregationBuilder::new,
                        DerivativePipelineAggregator::new,
                        DerivativePipelineAggregationBuilder::parse
                    ).addResultReader(InternalDerivative::new)
                );
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupePipelineAggregation));

        SearchPlugin registersDupeRescorer = new SearchPlugin() {
            @Override
            public List<RescorerSpec<?>> getRescorers() {
                return singletonList(
                    new RescorerSpec<>(QueryRescorerBuilder.NAME, QueryRescorerBuilder::new, QueryRescorerBuilder::fromXContent)
                );
            }
        };
        expectThrows(IllegalArgumentException.class, registryForPlugin(registersDupeRescorer));
    }

    private ThrowingRunnable registryForPlugin(SearchPlugin plugin) {
        return () -> new NamedXContentRegistry(new SearchModule(Settings.EMPTY, singletonList(plugin)).getNamedXContents());
    }

    public void testRegisterSuggester() {
        SearchModule module = new SearchModule(Settings.EMPTY, singletonList(new SearchPlugin() {
            @Override
            public List<SuggesterSpec<?>> getSuggesters() {
                return singletonList(
                    new SuggesterSpec<>(
                        TestSuggestionBuilder.SUGGESTION_NAME,
                        TestSuggestionBuilder::new,
                        TestSuggestionBuilder::fromXContent,
                        TestSuggestion::new
                    )
                );
            }
        }));

        assertEquals(
            1,
            module.getNamedXContents()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.match("term", LoggingDeprecationHandler.INSTANCE))
                .count()
        );
        assertEquals(
            1,
            module.getNamedXContents()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.match("phrase", LoggingDeprecationHandler.INSTANCE))
                .count()
        );
        assertEquals(
            1,
            module.getNamedXContents()
                .stream()
                .filter(
                    e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.match("completion", LoggingDeprecationHandler.INSTANCE)
                )
                .count()
        );
        assertEquals(
            1,
            module.getNamedXContents()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.match("test", LoggingDeprecationHandler.INSTANCE))
                .count()
        );

        assertEquals(
            1,
            module.getNamedWriteables()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.equals("term"))
                .count()
        );
        assertEquals(
            1,
            module.getNamedWriteables()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.equals("phrase"))
                .count()
        );
        assertEquals(
            1,
            module.getNamedWriteables()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.equals("completion"))
                .count()
        );
        assertEquals(
            1,
            module.getNamedWriteables()
                .stream()
                .filter(e -> e.categoryClass.equals(SuggestionBuilder.class) && e.name.equals("test"))
                .count()
        );

        assertEquals(
            1,
            module.getNamedWriteables().stream().filter(e -> e.categoryClass.equals(Suggestion.class) && e.name.equals("term")).count()
        );
        assertEquals(
            1,
            module.getNamedWriteables().stream().filter(e -> e.categoryClass.equals(Suggestion.class) && e.name.equals("phrase")).count()
        );
        assertEquals(
            1,
            module.getNamedWriteables()
                .stream()
                .filter(e -> e.categoryClass.equals(Suggestion.class) && e.name.equals("completion"))
                .count()
        );
        assertEquals(
            1,
            module.getNamedWriteables().stream().filter(e -> e.categoryClass.equals(Suggestion.class) && e.name.equals("test")).count()
        );
    }

    public void testRegisterHighlighter() {
        CustomHighlighter customHighlighter = new CustomHighlighter();
        SearchModule module = new SearchModule(Settings.EMPTY, singletonList(new SearchPlugin() {
            @Override
            public Map<String, Highlighter> getHighlighters() {
                return singletonMap("custom", customHighlighter);
            }
        }));

        Map<String, Highlighter> highlighters = module.getHighlighters();
        assertEquals(FastVectorHighlighter.class, highlighters.get("fvh").getClass());
        assertEquals(PlainHighlighter.class, highlighters.get("plain").getClass());
        assertEquals(UnifiedHighlighter.class, highlighters.get("unified").getClass());
        assertSame(highlighters.get("custom"), customHighlighter);
    }

    public void testRegisteredQueries() {
        List<String> allSupportedQueries = new ArrayList<>();
        Collections.addAll(allSupportedQueries, NON_DEPRECATED_QUERIES);
        Collections.addAll(allSupportedQueries, DEPRECATED_QUERIES);
        SearchModule module = new SearchModule(Settings.EMPTY, emptyList());

        Set<String> registeredNonDeprecated = module.getNamedXContents()
            .stream()
            .filter(e -> e.categoryClass.equals(QueryBuilder.class))
            .filter(e -> e.name.getAllReplacedWith() == null)
            .map(e -> e.name.getPreferredName())
            .collect(toSet());
        Set<String> registeredAll = module.getNamedXContents()
            .stream()
            .filter(e -> e.categoryClass.equals(QueryBuilder.class))
            .flatMap(e -> Arrays.stream(e.name.getAllNamesIncludedDeprecated()))
            .collect(toSet());

        assertThat(registeredNonDeprecated, containsInAnyOrder(NON_DEPRECATED_QUERIES));
        assertThat(registeredAll, containsInAnyOrder(allSupportedQueries.toArray(new String[0])));
    }

    public void testRegisterAggregation() {
        SearchModule module = new SearchModule(Settings.EMPTY, singletonList(new SearchPlugin() {
            @Override
            public List<AggregationSpec> getAggregations() {
                return singletonList(new AggregationSpec("test", TestAggregationBuilder::new, TestAggregationBuilder::fromXContent));
            }
        }));

        assertThat(
            module.getNamedXContents()
                .stream()
                .filter(
                    entry -> entry.categoryClass.equals(BaseAggregationBuilder.class)
                        && entry.name.match("test", LoggingDeprecationHandler.INSTANCE)
                )
                .collect(toList()),
            hasSize(1)
        );
    }

    public void testRegisterPipelineAggregation() {
        SearchModule module = new SearchModule(Settings.EMPTY, singletonList(new SearchPlugin() {
            @Override
            public List<PipelineAggregationSpec> getPipelineAggregations() {
                return singletonList(
                    new PipelineAggregationSpec(
                        "test",
                        TestPipelineAggregationBuilder::new,
                        TestPipelineAggregator::new,
                        TestPipelineAggregationBuilder::fromXContent
                    )
                );
            }
        }));

        assertThat(
            module.getNamedXContents()
                .stream()
                .filter(
                    entry -> entry.categoryClass.equals(BaseAggregationBuilder.class)
                        && entry.name.match("test", LoggingDeprecationHandler.INSTANCE)
                )
                .collect(toList()),
            hasSize(1)
        );
    }

    public void testRegisterRescorer() {
        SearchModule module = new SearchModule(Settings.EMPTY, singletonList(new SearchPlugin() {
            @Override
            public List<RescorerSpec<?>> getRescorers() {
                return singletonList(new RescorerSpec<>("test", TestRescorerBuilder::new, TestRescorerBuilder::fromXContent));
            }
        }));
        assertThat(
            module.getNamedXContents()
                .stream()
                .filter(
                    entry -> entry.categoryClass.equals(RescorerBuilder.class)
                        && entry.name.match("test", LoggingDeprecationHandler.INSTANCE)
                )
                .collect(toList()),
            hasSize(1)
        );
    }

    private static final String[] NON_DEPRECATED_QUERIES = new String[] {
        "bool",
        "boosting",
        "constant_score",
        "dis_max",
        "exists",
        "function_score",
        "fuzzy",
        "geo_bounding_box",
        "geo_distance",
        "geo_polygon",
        "geo_shape",
        "ids",
        "intervals",
        "match",
        "match_all",
        "match_bool_prefix",
        "match_none",
        "match_phrase",
        "match_phrase_prefix",
        "more_like_this",
        "multi_match",
        "nested",
        "prefix",
        "query_string",
        "range",
        "regexp",
        "script",
        "script_score",
        "simple_query_string",
        "span_containing",
        "span_field_masking",
        "span_first",
        "span_gap",
        "span_multi",
        "span_near",
        "span_not",
        "span_or",
        "span_term",
        "span_within",
        "term",
        "terms",
        "terms_set",
        "wildcard",
        "wrapper",
        "distance_feature" };

    // add here deprecated queries to make sure we log a deprecation warnings when they are used
    private static final String[] DEPRECATED_QUERIES = new String[] { "common", "field_masking_span" };

    /**
     * Dummy test {@link AggregationBuilder} used to test registering aggregation builders.
     */
    private static class TestAggregationBuilder extends ValuesSourceAggregationBuilder<TestAggregationBuilder> {
        protected TestAggregationBuilder(TestAggregationBuilder clone, Builder factoriesBuilder, Map<String, Object> metadata) {
            super(clone, factoriesBuilder, metadata);
        }

        @Override
        protected ValuesSourceType defaultValueSourceType() {
            return CoreValuesSourceType.BYTES;
        }

        @Override
        protected AggregationBuilder shallowCopy(Builder factoriesBuilder, Map<String, Object> metadata) {
            return new TestAggregationBuilder(this, factoriesBuilder, metadata);
        }

        /**
         * Read from a stream.
         */
        protected TestAggregationBuilder(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public String getType() {
            return "test";
        }

        @Override
        protected ValuesSourceRegistry.RegistryKey<?> getRegistryKey() {
            return ValuesSourceRegistry.UNREGISTERED_KEY;
        }

        @Override
        protected void innerWriteTo(StreamOutput out) throws IOException {}

        @Override
        public BucketCardinality bucketCardinality() {
            return BucketCardinality.NONE;
        }

        @Override
        protected ValuesSourceAggregatorFactory innerBuild(
            QueryShardContext queryShardContext,
            ValuesSourceConfig config,
            AggregatorFactory parent,
            Builder subFactoriesBuilder
        ) throws IOException {
            return null;
        }

        @Override
        protected XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
            return null;
        }

        private static TestAggregationBuilder fromXContent(String name, XContentParser p) {
            return null;
        }
    }

    /**
     * Dummy test {@link PipelineAggregator} used to test registering aggregation builders.
     */
    private static class TestPipelineAggregationBuilder extends AbstractPipelineAggregationBuilder<TestPipelineAggregationBuilder> {
        /**
         * Read from a stream.
         */
        TestPipelineAggregationBuilder(StreamInput in) throws IOException {
            super(in, "test");
        }

        @Override
        public String getWriteableName() {
            return "test";
        }

        @Override
        protected void doWriteTo(StreamOutput out) throws IOException {}

        @Override
        protected PipelineAggregator createInternal(Map<String, Object> metadata) {
            return null;
        }

        @Override
        protected XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException {
            return null;
        }

        private static TestPipelineAggregationBuilder fromXContent(String name, XContentParser p) {
            return null;
        }

        @Override
        protected void validate(ValidationContext context) {}
    }

    /**
     * Dummy test {@link PipelineAggregator} used to test registering aggregation builders.
     */
    private static class TestPipelineAggregator extends PipelineAggregator {
        /**
         * Read from a stream.
         */
        TestPipelineAggregator(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public String getWriteableName() {
            return "test";
        }

        @Override
        protected void doWriteTo(StreamOutput out) throws IOException {}

        @Override
        public InternalAggregation reduce(InternalAggregation aggregation, ReduceContext reduceContext) {
            return null;
        }
    }

    private static class TestRescorerBuilder extends RescorerBuilder<TestRescorerBuilder> {
        public static TestRescorerBuilder fromXContent(XContentParser parser) {
            return null;
        }

        TestRescorerBuilder(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public String getWriteableName() {
            return "test";
        }

        @Override
        public RescorerBuilder<TestRescorerBuilder> rewrite(QueryRewriteContext ctx) throws IOException {
            return this;
        }

        @Override
        protected void doWriteTo(StreamOutput out) throws IOException {}

        @Override
        protected void doXContent(XContentBuilder builder, Params params) throws IOException {}

        @Override
        public RescoreContext innerBuildContext(int windowSize, QueryShardContext context) throws IOException {
            return null;
        }
    }

    private static class TestSuggester extends Suggester<SuggestionSearchContext.SuggestionContext> {

        @Override
        protected Suggestion<? extends Suggestion.Entry<? extends Suggestion.Entry.Option>> innerExecute(
            String name,
            SuggestionSearchContext.SuggestionContext suggestion,
            IndexSearcher searcher,
            CharsRefBuilder spare
        ) throws IOException {
            return null;
        }

        @Override
        protected Suggestion<? extends Entry<? extends Option>> emptySuggestion(
            String name,
            SuggestionContext suggestion,
            CharsRefBuilder spare
        ) throws IOException {
            return null;
        }
    }

    private static class TestSuggestionBuilder extends SuggestionBuilder<TestSuggestionBuilder> {

        public static final String SUGGESTION_NAME = "test";

        TestSuggestionBuilder(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        protected void doWriteTo(StreamOutput out) throws IOException {}

        public static TestSuggestionBuilder fromXContent(XContentParser parser) {
            return null;
        }

        @Override
        protected XContentBuilder innerToXContent(XContentBuilder builder, Params params) throws IOException {
            return null;
        }

        @Override
        protected SuggestionSearchContext.SuggestionContext build(QueryShardContext context) throws IOException {
            return null;
        }

        @Override
        protected boolean doEquals(TestSuggestionBuilder other) {
            return false;
        }

        @Override
        protected int doHashCode() {
            return 0;
        }

        @Override
        public String getWriteableName() {
            return "test";
        }
    }

    private static class TestSuggestion extends Suggestion {
        TestSuggestion(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        protected Entry newEntry(StreamInput in) throws IOException {
            return null;
        }

        @Override
        public String getWriteableName() {
            return "test";
        }
    }
}
