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

package org.elasticsearch.search.builder;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.inject.multibindings.Multibinder;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.io.stream.NamedWriteableAwareStreamInput;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.test.AbstractQueryTestCase;
import org.elasticsearch.index.query.EmptyQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.indices.breaker.NoneCircuitBreakerService;
import org.elasticsearch.indices.query.IndicesQueriesRegistry;
import org.elasticsearch.script.MockScriptEngine;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptContextRegistry;
import org.elasticsearch.script.ScriptEngineRegistry;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.ScriptModule;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptSettings;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorParsers;
import org.elasticsearch.search.fetch.source.FetchSourceContext;
import org.elasticsearch.search.highlight.HighlightBuilderTests;
import org.elasticsearch.search.rescore.QueryRescoreBuilderTests;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.searchafter.SearchAfterBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilderTests;
import org.elasticsearch.search.suggest.Suggesters;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;
import org.elasticsearch.test.InternalSettingsPlugin;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPoolModule;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ClusterServiceUtils.createClusterService;
import static org.elasticsearch.test.ClusterServiceUtils.setState;
import static org.hamcrest.Matchers.equalTo;

public class SearchSourceBuilderTests extends ESTestCase {
    private static Injector injector;

    private static NamedWriteableRegistry namedWriteableRegistry;

    private static IndicesQueriesRegistry indicesQueriesRegistry;

    private static Index index;

    private static AggregatorParsers aggParsers;

    private static Suggesters suggesters;

    private static String[] currentTypes;

    private static ParseFieldMatcher parseFieldMatcher;

    @BeforeClass
    public static void init() throws IOException {
        // we have to prefer CURRENT since with the range of versions we support
        // it's rather unlikely to get the current actually.
        Version version = randomBoolean() ? Version.CURRENT
                : VersionUtils.randomVersionBetween(random(), Version.V_2_0_0_beta1, Version.CURRENT);
        Settings settings = Settings.builder()
                .put("node.name", AbstractQueryTestCase.class.toString())
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir())
                .put(ScriptService.SCRIPT_AUTO_RELOAD_ENABLED_SETTING.getKey(), false).build();

        namedWriteableRegistry = new NamedWriteableRegistry();
        index = new Index(randomAsciiOfLengthBetween(1, 10), "_na_");
        Settings indexSettings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        final ThreadPool threadPool = new ThreadPool(settings);
        final ClusterService clusterService = createClusterService(threadPool);
        setState(clusterService, new ClusterState.Builder(clusterService.state()).metaData(new MetaData.Builder()
                .put(new IndexMetaData.Builder(index.getName()).settings(indexSettings).numberOfShards(1).numberOfReplicas(0))));
        SettingsModule settingsModule = new SettingsModule(settings);
        settingsModule.registerSetting(InternalSettingsPlugin.VERSION_CREATED);
        ScriptModule scriptModule = new ScriptModule() {
            @Override
            protected void configure() {
                Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir())
                        // no file watching, so we don't need a
                        // ResourceWatcherService
                        .put(ScriptService.SCRIPT_AUTO_RELOAD_ENABLED_SETTING.getKey(), false).build();
                MockScriptEngine mockScriptEngine = new MockScriptEngine();
                Multibinder<ScriptEngineService> multibinder = Multibinder.newSetBinder(binder(), ScriptEngineService.class);
                multibinder.addBinding().toInstance(mockScriptEngine);
                Set<ScriptEngineService> engines = new HashSet<>();
                engines.add(mockScriptEngine);
                List<ScriptContext.Plugin> customContexts = new ArrayList<>();
                ScriptEngineRegistry scriptEngineRegistry = new ScriptEngineRegistry(Collections
                        .singletonList(new ScriptEngineRegistry.ScriptEngineRegistration(MockScriptEngine.class,
                                                                                         MockScriptEngine.NAME,
                                                                                         true)));
                bind(ScriptEngineRegistry.class).toInstance(scriptEngineRegistry);
                ScriptContextRegistry scriptContextRegistry = new ScriptContextRegistry(customContexts);
                bind(ScriptContextRegistry.class).toInstance(scriptContextRegistry);
                ScriptSettings scriptSettings = new ScriptSettings(scriptEngineRegistry, scriptContextRegistry);
                bind(ScriptSettings.class).toInstance(scriptSettings);
                try {
                    ScriptService scriptService = new ScriptService(settings, new Environment(settings), engines, null,
                            scriptEngineRegistry, scriptContextRegistry, scriptSettings);
                    bind(ScriptService.class).toInstance(scriptService);
                } catch (IOException e) {
                    throw new IllegalStateException("error while binding ScriptService", e);
                }
            }
        };
        scriptModule.prepareSettings(settingsModule);
        injector = new ModulesBuilder().add(
                new EnvironmentModule(new Environment(settings)), settingsModule,
                new ThreadPoolModule(threadPool),
                scriptModule, new IndicesModule() {
                    @Override
                    protected void configure() {
                        bindMapperExtension();
                    }
                }, new SearchModule(settings, namedWriteableRegistry) {
                    @Override
                    protected void configureSearch() {
                        // Skip me
                    }
                },
                new IndexSettingsModule(index, settings),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ClusterService.class).toProvider(Providers.of(clusterService));
                        bind(CircuitBreakerService.class).to(NoneCircuitBreakerService.class);
                        bind(NamedWriteableRegistry.class).toInstance(namedWriteableRegistry);
                    }
                }
        ).createInjector();
        aggParsers = injector.getInstance(AggregatorParsers.class);
        suggesters = injector.getInstance(Suggesters.class);
        // create some random type with some default field, those types will
        // stick around for all of the subclasses
        currentTypes = new String[randomIntBetween(0, 5)];
        for (int i = 0; i < currentTypes.length; i++) {
            String type = randomAsciiOfLengthBetween(1, 10);
            currentTypes[i] = type;
        }
        indicesQueriesRegistry = injector.getInstance(IndicesQueriesRegistry.class);
        parseFieldMatcher = ParseFieldMatcher.STRICT;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        injector.getInstance(ClusterService.class).close();
        terminate(injector.getInstance(ThreadPool.class));
        injector = null;
        index = null;
        aggParsers = null;
        suggesters = null;
        currentTypes = null;
        namedWriteableRegistry = null;
    }

    protected final SearchSourceBuilder createSearchSourceBuilder() throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        if (randomBoolean()) {
            builder.from(randomIntBetween(0, 10000));
        }
        if (randomBoolean()) {
            builder.size(randomIntBetween(0, 10000));
        }
        if (randomBoolean()) {
            builder.explain(randomBoolean());
        }
        if (randomBoolean()) {
            builder.version(randomBoolean());
        }
        if (randomBoolean()) {
            builder.trackScores(randomBoolean());
        }
        if (randomBoolean()) {
            builder.minScore(randomFloat() * 1000);
        }
        if (randomBoolean()) {
            builder.timeout(new TimeValue(randomIntBetween(1, 100), randomFrom(TimeUnit.values())));
        }
        if (randomBoolean()) {
            builder.terminateAfter(randomIntBetween(1, 100000));
        }
        // if (randomBoolean()) {
        // builder.defaultRescoreWindowSize(randomIntBetween(1, 100));
        // }
        if (randomBoolean()) {
            int fieldsSize = randomInt(25);
            List<String> fields = new ArrayList<>(fieldsSize);
            for (int i = 0; i < fieldsSize; i++) {
                fields.add(randomAsciiOfLengthBetween(5, 50));
            }
            builder.fields(fields);
        }
        if (randomBoolean()) {
            int fieldDataFieldsSize = randomInt(25);
            for (int i = 0; i < fieldDataFieldsSize; i++) {
                builder.fieldDataField(randomAsciiOfLengthBetween(5, 50));
            }
        }
        if (randomBoolean()) {
            int scriptFieldsSize = randomInt(25);
            for (int i = 0; i < scriptFieldsSize; i++) {
                if (randomBoolean()) {
                    builder.scriptField(randomAsciiOfLengthBetween(5, 50), new Script("foo"), randomBoolean());
                } else {
                    builder.scriptField(randomAsciiOfLengthBetween(5, 50), new Script("foo"));
                }
            }
        }
        if (randomBoolean()) {
            FetchSourceContext fetchSourceContext;
            int branch = randomInt(5);
            String[] includes = new String[randomIntBetween(0, 20)];
            for (int i = 0; i < includes.length; i++) {
                includes[i] = randomAsciiOfLengthBetween(5, 20);
            }
            String[] excludes = new String[randomIntBetween(0, 20)];
            for (int i = 0; i < excludes.length; i++) {
                excludes[i] = randomAsciiOfLengthBetween(5, 20);
            }
            switch (branch) {
                case 0:
                    fetchSourceContext = new FetchSourceContext(randomBoolean());
                    break;
                case 1:
                    fetchSourceContext = new FetchSourceContext(includes, excludes);
                    break;
                case 2:
                    fetchSourceContext = new FetchSourceContext(randomAsciiOfLengthBetween(5, 20), randomAsciiOfLengthBetween(5, 20));
                    break;
                case 3:
                    fetchSourceContext = new FetchSourceContext(true, includes, excludes);
                    break;
                case 4:
                    fetchSourceContext = new FetchSourceContext(includes);
                    break;
                case 5:
                    fetchSourceContext = new FetchSourceContext(randomAsciiOfLengthBetween(5, 20));
                    break;
                default:
                    throw new IllegalStateException();
            }
            builder.fetchSource(fetchSourceContext);
        }
        if (randomBoolean()) {
            int size = randomIntBetween(0, 20);
            List<String> statsGroups = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                statsGroups.add(randomAsciiOfLengthBetween(5, 20));
            }
            builder.stats(statsGroups);
        }
        if (randomBoolean()) {
            int indexBoostSize = randomIntBetween(1, 10);
            for (int i = 0; i < indexBoostSize; i++) {
                builder.indexBoost(randomAsciiOfLengthBetween(5, 20), randomFloat() * 10);
            }
        }
        if (randomBoolean()) {
            builder.query(QueryBuilders.termQuery(randomAsciiOfLengthBetween(5, 20), randomAsciiOfLengthBetween(5, 20)));
        }
        if (randomBoolean()) {
            builder.postFilter(QueryBuilders.termQuery(randomAsciiOfLengthBetween(5, 20), randomAsciiOfLengthBetween(5, 20)));
        }
        if (randomBoolean()) {
            int numSorts = randomIntBetween(1, 5);
            for (int i = 0; i < numSorts; i++) {
                int branch = randomInt(5);
                switch (branch) {
                    case 0:
                        builder.sort(SortBuilders.fieldSort(randomAsciiOfLengthBetween(5, 20)).order(randomFrom(SortOrder.values())));
                        break;
                    case 1:
                        builder.sort(SortBuilders.geoDistanceSort(randomAsciiOfLengthBetween(5, 20),
                                AbstractQueryTestCase.randomGeohash(1, 12)).order(randomFrom(SortOrder.values())));
                        break;
                    case 2:
                        builder.sort(SortBuilders.scoreSort().order(randomFrom(SortOrder.values())));
                        break;
                    case 3:
                        builder.sort(SortBuilders.scriptSort(new Script("foo"),
                                ScriptSortType.NUMBER).order(randomFrom(SortOrder.values())));
                        break;
                    case 4:
                        builder.sort(randomAsciiOfLengthBetween(5, 20));
                        break;
                    case 5:
                        builder.sort(randomAsciiOfLengthBetween(5, 20), randomFrom(SortOrder.values()));
                        break;
                }
            }
        }

        if (randomBoolean()) {
            int numSearchFrom = randomIntBetween(1, 5);
            // We build a json version of the search_from first in order to
            // ensure that every number type remain the same before/after xcontent (de)serialization.
            // This is not a problem because the final type of each field value is extracted from associated sort field.
            // This little trick ensure that equals and hashcode are the same when using the xcontent serialization.
            XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
            jsonBuilder.startObject();
            jsonBuilder.startArray("search_from");
            for (int i = 0; i < numSearchFrom; i++) {
                int branch = randomInt(8);
                switch (branch) {
                    case 0:
                        jsonBuilder.value(randomInt());
                        break;
                    case 1:
                        jsonBuilder.value(randomFloat());
                        break;
                    case 2:
                        jsonBuilder.value(randomLong());
                        break;
                    case 3:
                        jsonBuilder.value(randomDouble());
                        break;
                    case 4:
                        jsonBuilder.value(randomAsciiOfLengthBetween(5, 20));
                        break;
                    case 5:
                        jsonBuilder.value(randomBoolean());
                        break;
                    case 6:
                        jsonBuilder.value(randomByte());
                        break;
                    case 7:
                        jsonBuilder.value(randomShort());
                        break;
                    case 8:
                        jsonBuilder.value(new Text(randomAsciiOfLengthBetween(5, 20)));
                        break;
                }
            }
            jsonBuilder.endArray();
            jsonBuilder.endObject();
            XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(jsonBuilder.bytes());
            parser.nextToken();
            parser.nextToken();
            parser.nextToken();
            builder.searchAfter(SearchAfterBuilder.fromXContent(parser, null).getSortValues());
        }
        if (randomBoolean()) {
            builder.highlighter(HighlightBuilderTests.randomHighlighterBuilder());
        }
        if (randomBoolean()) {
            builder.suggest(SuggestBuilderTests.randomSuggestBuilder());
        }
        if (randomBoolean()) {
            int numRescores = randomIntBetween(1, 5);
            for (int i = 0; i < numRescores; i++) {
                builder.addRescorer(QueryRescoreBuilderTests.randomRescoreBuilder());
            }
        }
        if (randomBoolean()) {
            builder.aggregation(AggregationBuilders.avg(randomAsciiOfLengthBetween(5, 20)));
        }
        if (randomBoolean()) {
            XContentBuilder xContentBuilder = XContentFactory.jsonBuilder();
            xContentBuilder.startObject();
            xContentBuilder.field("term_vectors_fetch", randomAsciiOfLengthBetween(5, 20));
            xContentBuilder.endObject();
            builder.ext(xContentBuilder);
        }
        return builder;
    }

    public void testFromXContent() throws IOException {
        SearchSourceBuilder testSearchSourceBuilder = createSearchSourceBuilder();
        XContentBuilder builder = XContentFactory.contentBuilder(randomFrom(XContentType.values()));
        if (randomBoolean()) {
            builder.prettyPrint();
        }
        testSearchSourceBuilder.toXContent(builder, ToXContent.EMPTY_PARAMS);
        assertParseSearchSource(testSearchSourceBuilder, builder.bytes());
    }

    private void assertParseSearchSource(SearchSourceBuilder testBuilder, BytesReference searchSourceAsBytes) throws IOException {
        XContentParser parser = XContentFactory.xContent(searchSourceAsBytes).createParser(searchSourceAsBytes);
        QueryParseContext parseContext = createParseContext(parser);
        if (randomBoolean()) {
            parser.nextToken(); // sometimes we move it on the START_OBJECT to test the embedded case
        }
        SearchSourceBuilder newBuilder = SearchSourceBuilder.fromXContent(parseContext, aggParsers, suggesters);
        assertNull(parser.nextToken());
        assertEquals(testBuilder, newBuilder);
        assertEquals(testBuilder.hashCode(), newBuilder.hashCode());
    }

    private static QueryParseContext createParseContext(XContentParser parser) {
        QueryParseContext context = new QueryParseContext(indicesQueriesRegistry, parser, parseFieldMatcher);
        return context;
    }

    public void testSerialization() throws IOException {
        SearchSourceBuilder testBuilder = createSearchSourceBuilder();
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            testBuilder.writeTo(output);
            try (StreamInput in = new NamedWriteableAwareStreamInput(StreamInput.wrap(output.bytes()), namedWriteableRegistry)) {
                SearchSourceBuilder deserializedBuilder = new SearchSourceBuilder(in);
                assertEquals(deserializedBuilder, testBuilder);
                assertEquals(deserializedBuilder.hashCode(), testBuilder.hashCode());
                assertNotSame(deserializedBuilder, testBuilder);
            }
        }
    }

    public void testEqualsAndHashcode() throws IOException {
        SearchSourceBuilder firstBuilder = createSearchSourceBuilder();
        assertFalse("source builder is equal to null", firstBuilder.equals(null));
        assertFalse("source builder is equal to incompatible type", firstBuilder.equals(""));
        assertTrue("source builder is not equal to self", firstBuilder.equals(firstBuilder));
        assertThat("same source builder's hashcode returns different values if called multiple times", firstBuilder.hashCode(),
                equalTo(firstBuilder.hashCode()));

        SearchSourceBuilder secondBuilder = copyBuilder(firstBuilder);
        assertTrue("source builder is not equal to self", secondBuilder.equals(secondBuilder));
        assertTrue("source builder is not equal to its copy", firstBuilder.equals(secondBuilder));
        assertTrue("source builder is not symmetric", secondBuilder.equals(firstBuilder));
        assertThat("source builder copy's hashcode is different from original hashcode",
                secondBuilder.hashCode(), equalTo(firstBuilder.hashCode()));

        SearchSourceBuilder thirdBuilder = copyBuilder(secondBuilder);
        assertTrue("source builder is not equal to self", thirdBuilder.equals(thirdBuilder));
        assertTrue("source builder is not equal to its copy", secondBuilder.equals(thirdBuilder));
        assertThat("source builder copy's hashcode is different from original hashcode",
                secondBuilder.hashCode(), equalTo(thirdBuilder.hashCode()));
        assertTrue("equals is not transitive", firstBuilder.equals(thirdBuilder));
        assertThat("source builder copy's hashcode is different from original hashcode",
                firstBuilder.hashCode(), equalTo(thirdBuilder.hashCode()));
        assertTrue("equals is not symmetric", thirdBuilder.equals(secondBuilder));
        assertTrue("equals is not symmetric", thirdBuilder.equals(firstBuilder));
    }

    //we use the streaming infra to create a copy of the query provided as argument
    protected SearchSourceBuilder copyBuilder(SearchSourceBuilder builder) throws IOException {
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            builder.writeTo(output);
            try (StreamInput in = new NamedWriteableAwareStreamInput(StreamInput.wrap(output.bytes()), namedWriteableRegistry)) {
                return new SearchSourceBuilder(in);
            }
        }
    }

    public void testParseIncludeExclude() throws IOException {
        {
            String restContent = " { \"_source\": { \"includes\": \"include\", \"excludes\": \"*.field2\"}}";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertArrayEquals(new String[]{"*.field2"}, searchSourceBuilder.fetchSource().excludes());
                assertArrayEquals(new String[]{"include"}, searchSourceBuilder.fetchSource().includes());
            }
        }
        {
            String restContent = " { \"_source\": false}";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertArrayEquals(new String[]{}, searchSourceBuilder.fetchSource().excludes());
                assertArrayEquals(new String[]{}, searchSourceBuilder.fetchSource().includes());
                assertFalse(searchSourceBuilder.fetchSource().fetchSource());
            }
        }
    }

    public void testParseSort() throws IOException {
        {
            String restContent = " { \"sort\": \"foo\"}";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertEquals(1, searchSourceBuilder.sorts().size());
                assertEquals(new FieldSortBuilder("foo"), searchSourceBuilder.sorts().get(0));
            }
        }

        {
            String restContent = "{\"sort\" : [\n" +
                    "        { \"post_date\" : {\"order\" : \"asc\"}},\n" +
                    "        \"user\",\n" +
                    "        { \"name\" : \"desc\" },\n" +
                    "        { \"age\" : \"desc\" },\n" +
                    "        \"_score\"\n" +
                    "    ]}";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertEquals(5, searchSourceBuilder.sorts().size());
                assertEquals(new FieldSortBuilder("post_date"), searchSourceBuilder.sorts().get(0));
                assertEquals(new FieldSortBuilder("user"), searchSourceBuilder.sorts().get(1));
                assertEquals(new FieldSortBuilder("name").order(SortOrder.DESC), searchSourceBuilder.sorts().get(2));
                assertEquals(new FieldSortBuilder("age").order(SortOrder.DESC), searchSourceBuilder.sorts().get(3));
                assertEquals(new ScoreSortBuilder(), searchSourceBuilder.sorts().get(4));
            }
        }
    }

    /**
     * test that we can parse the `rescore` element either as single object or as array
     */
    public void testParseRescore() throws IOException {
        {
            String restContent = "{\n" +
                "    \"query\" : {\n" +
                "        \"match\": { \"content\": { \"query\": \"foo bar\" }}\n" +
                "     },\n" +
                "    \"rescore\": {" +
                "        \"window_size\": 50,\n" +
                "        \"query\": {\n" +
                "            \"rescore_query\" : {\n" +
                "                \"match\": { \"content\": { \"query\": \"baz\" } }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertEquals(1, searchSourceBuilder.rescores().size());
                assertEquals(new QueryRescorerBuilder(QueryBuilders.matchQuery("content", "baz")).windowSize(50),
                        searchSourceBuilder.rescores().get(0));
            }
        }

        {
            String restContent = "{\n" +
                "    \"query\" : {\n" +
                "        \"match\": { \"content\": { \"query\": \"foo bar\" }}\n" +
                "     },\n" +
                "    \"rescore\": [ {" +
                "        \"window_size\": 50,\n" +
                "        \"query\": {\n" +
                "            \"rescore_query\" : {\n" +
                "                \"match\": { \"content\": { \"query\": \"baz\" } }\n" +
                "            }\n" +
                "        }\n" +
                "    } ]\n" +
                "}\n";
            try (XContentParser parser = XContentFactory.xContent(restContent).createParser(restContent)) {
                SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.fromXContent(createParseContext(parser),
                        aggParsers, suggesters);
                assertEquals(1, searchSourceBuilder.rescores().size());
                assertEquals(new QueryRescorerBuilder(QueryBuilders.matchQuery("content", "baz")).windowSize(50),
                        searchSourceBuilder.rescores().get(0));
            }
        }
    }

    public void testEmptyPostFilter() throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.postFilter(new EmptyQueryBuilder());
        String query = "{ \"post_filter\": {} }";
        assertParseSearchSource(builder, new BytesArray(query));
    }
}
