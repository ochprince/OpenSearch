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

package com.colasoft.opensearch.search.functionscore;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Explanation;
import com.colasoft.opensearch.action.index.IndexRequestBuilder;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.action.search.SearchType;
import com.colasoft.opensearch.common.lucene.search.function.CombineFunction;
import com.colasoft.opensearch.common.lucene.search.function.Functions;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.fielddata.ScriptDocValues;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.ScriptPlugin;
import com.colasoft.opensearch.script.ExplainableScoreScript;
import com.colasoft.opensearch.script.ScoreScript;
import com.colasoft.opensearch.script.Script;
import com.colasoft.opensearch.script.ScriptContext;
import com.colasoft.opensearch.script.ScriptEngine;
import com.colasoft.opensearch.script.ScriptType;
import com.colasoft.opensearch.search.SearchHit;
import com.colasoft.opensearch.search.SearchHits;
import com.colasoft.opensearch.search.lookup.SearchLookup;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.ClusterScope;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.Scope;
import com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.colasoft.opensearch.client.Requests.searchRequest;
import static com.colasoft.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static com.colasoft.opensearch.index.query.QueryBuilders.functionScoreQuery;
import static com.colasoft.opensearch.index.query.QueryBuilders.termQuery;
import static com.colasoft.opensearch.index.query.functionscore.ScoreFunctionBuilders.scriptFunction;
import static com.colasoft.opensearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = Scope.SUITE, supportsDedicatedMasters = false, numDataNodes = 1)
public class ExplainableScriptIT extends OpenSearchIntegTestCase {

    public static class ExplainableScriptPlugin extends Plugin implements ScriptPlugin {
        @Override
        public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
            return new ScriptEngine() {
                @Override
                public String getType() {
                    return "test";
                }

                @Override
                public <T> T compile(String scriptName, String scriptSource, ScriptContext<T> context, Map<String, String> params) {
                    assert scriptSource.equals("explainable_script");
                    assert context == ScoreScript.CONTEXT;
                    ScoreScript.Factory factory = (params1, lookup) -> new ScoreScript.LeafFactory() {
                        @Override
                        public boolean needs_score() {
                            return false;
                        }

                        @Override
                        public ScoreScript newInstance(LeafReaderContext ctx) throws IOException {
                            return new MyScript(params1, lookup, ctx);
                        }
                    };
                    return context.factoryClazz.cast(factory);
                }

                @Override
                public Set<ScriptContext<?>> getSupportedContexts() {
                    return Collections.singleton(ScoreScript.CONTEXT);
                }
            };
        }
    }

    static class MyScript extends ScoreScript implements ExplainableScoreScript {

        MyScript(Map<String, Object> params, SearchLookup lookup, LeafReaderContext leafContext) {
            super(params, lookup, leafContext);
        }

        @Override
        public Explanation explain(Explanation subQueryScore) throws IOException {
            return explain(subQueryScore, null);
        }

        @Override
        public Explanation explain(Explanation subQueryScore, String functionName) throws IOException {
            Explanation scoreExp = Explanation.match(subQueryScore.getValue(), "_score: ", subQueryScore);
            return Explanation.match(
                (float) (execute(null)),
                "This script" + Functions.nameOrEmptyFunc(functionName) + " returned " + execute(null),
                scoreExp
            );
        }

        @Override
        public double execute(ExplanationHolder explanation) {
            return ((Number) ((ScriptDocValues) getDoc().get("number_field")).get(0)).doubleValue();
        }
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(ExplainableScriptPlugin.class);
    }

    public void testExplainScript() throws InterruptedException, IOException, ExecutionException {
        List<IndexRequestBuilder> indexRequests = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            indexRequests.add(
                client().prepareIndex("test")
                    .setId(Integer.toString(i))
                    .setSource(jsonBuilder().startObject().field("number_field", i).field("text", "text").endObject())
            );
        }
        indexRandom(true, true, indexRequests);
        client().admin().indices().prepareRefresh().get();
        ensureYellow();
        SearchResponse response = client().search(
            searchRequest().searchType(SearchType.QUERY_THEN_FETCH)
                .source(
                    searchSource().explain(true)
                        .query(
                            functionScoreQuery(
                                termQuery("text", "text"),
                                scriptFunction(new Script(ScriptType.INLINE, "test", "explainable_script", Collections.emptyMap()))
                            ).boostMode(CombineFunction.REPLACE)
                        )
                )
        ).actionGet();

        OpenSearchAssertions.assertNoFailures(response);
        SearchHits hits = response.getHits();
        assertThat(hits.getTotalHits().value, equalTo(20L));
        int idCounter = 19;
        for (SearchHit hit : hits.getHits()) {
            assertThat(hit.getId(), equalTo(Integer.toString(idCounter)));
            assertThat(hit.getExplanation().toString(), containsString(Double.toString(idCounter)));
            assertThat(hit.getExplanation().toString(), containsString("1 = n"));
            assertThat(hit.getExplanation().toString(), containsString("1 = N"));
            assertThat(hit.getExplanation().getDetails().length, equalTo(2));
            idCounter--;
        }
    }

    public void testExplainScriptWithName() throws InterruptedException, IOException, ExecutionException {
        List<IndexRequestBuilder> indexRequests = new ArrayList<>();
        indexRequests.add(
            client().prepareIndex("test")
                .setId(Integer.toString(1))
                .setSource(jsonBuilder().startObject().field("number_field", 1).field("text", "text").endObject())
        );
        indexRandom(true, true, indexRequests);
        client().admin().indices().prepareRefresh().get();
        ensureYellow();
        SearchResponse response = client().search(
            searchRequest().searchType(SearchType.QUERY_THEN_FETCH)
                .source(
                    searchSource().explain(true)
                        .query(
                            functionScoreQuery(
                                termQuery("text", "text"),
                                scriptFunction(new Script(ScriptType.INLINE, "test", "explainable_script", Collections.emptyMap()), "func1")
                            ).boostMode(CombineFunction.REPLACE)
                        )
                )
        ).actionGet();

        OpenSearchAssertions.assertNoFailures(response);
        SearchHits hits = response.getHits();
        assertThat(hits.getTotalHits().value, equalTo(1L));
        assertThat(hits.getHits()[0].getId(), equalTo("1"));
        assertThat(hits.getHits()[0].getExplanation().getDetails(), arrayWithSize(2));
        assertThat(hits.getHits()[0].getExplanation().getDetails()[0].getDescription(), containsString("_name: func1"));
    }

}
