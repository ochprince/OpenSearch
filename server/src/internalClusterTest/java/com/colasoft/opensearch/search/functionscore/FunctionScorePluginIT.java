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

import org.apache.lucene.search.Explanation;
import com.colasoft.opensearch.action.ActionFuture;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.action.search.SearchType;
import com.colasoft.opensearch.common.Priority;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.index.query.functionscore.DecayFunction;
import com.colasoft.opensearch.index.query.functionscore.DecayFunctionBuilder;
import com.colasoft.opensearch.index.query.functionscore.DecayFunctionParser;
import com.colasoft.opensearch.index.query.functionscore.ScoreFunctionParser;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.search.SearchHits;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.ClusterScope;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.Scope;
import com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static com.colasoft.opensearch.client.Requests.indexRequest;
import static com.colasoft.opensearch.client.Requests.searchRequest;
import static com.colasoft.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static com.colasoft.opensearch.index.query.QueryBuilders.functionScoreQuery;
import static com.colasoft.opensearch.index.query.QueryBuilders.termQuery;
import static com.colasoft.opensearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = Scope.SUITE, supportsDedicatedMasters = false, numDataNodes = 1)
public class FunctionScorePluginIT extends OpenSearchIntegTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(CustomDistanceScorePlugin.class);
    }

    public void testPlugin() throws Exception {
        client().admin()
            .indices()
            .prepareCreate("test")
            .setMapping(
                jsonBuilder().startObject()
                    .startObject("properties")
                    .startObject("test")
                    .field("type", "text")
                    .endObject()
                    .startObject("num1")
                    .field("type", "date")
                    .endObject()
                    .endObject()
                    .endObject()
            )
            .get();
        client().admin().cluster().prepareHealth().setWaitForEvents(Priority.LANGUID).setWaitForYellowStatus().get();

        client().index(
            indexRequest("test").id("1").source(jsonBuilder().startObject().field("test", "value").field("num1", "2013-05-26").endObject())
        ).actionGet();
        client().index(
            indexRequest("test").id("2").source(jsonBuilder().startObject().field("test", "value").field("num1", "2013-05-27").endObject())
        ).actionGet();

        client().admin().indices().prepareRefresh().get();
        DecayFunctionBuilder<?> gfb = new CustomDistanceScoreBuilder("num1", "2013-05-28", "+1d");

        ActionFuture<SearchResponse> response = client().search(
            searchRequest().searchType(SearchType.QUERY_THEN_FETCH)
                .source(searchSource().explain(false).query(functionScoreQuery(termQuery("test", "value"), gfb)))
        );

        SearchResponse sr = response.actionGet();
        OpenSearchAssertions.assertNoFailures(sr);
        SearchHits sh = sr.getHits();

        assertThat(sh.getHits().length, equalTo(2));
        assertThat(sh.getAt(0).getId(), equalTo("1"));
        assertThat(sh.getAt(1).getId(), equalTo("2"));

    }

    public static class CustomDistanceScorePlugin extends Plugin implements SearchPlugin {
        @Override
        public List<ScoreFunctionSpec<?>> getScoreFunctions() {
            return singletonList(
                new ScoreFunctionSpec<>(CustomDistanceScoreBuilder.NAME, CustomDistanceScoreBuilder::new, CustomDistanceScoreBuilder.PARSER)
            );
        }
    }

    public static class CustomDistanceScoreBuilder extends DecayFunctionBuilder<CustomDistanceScoreBuilder> {
        public static final String NAME = "linear_mult";
        public static final ScoreFunctionParser<CustomDistanceScoreBuilder> PARSER = new DecayFunctionParser<>(
            CustomDistanceScoreBuilder::new
        );

        public CustomDistanceScoreBuilder(String fieldName, Object origin, Object scale) {
            super(fieldName, origin, scale, null);
        }

        CustomDistanceScoreBuilder(String fieldName, BytesReference functionBytes) {
            super(fieldName, functionBytes);
        }

        /**
         * Read from a stream.
         */
        CustomDistanceScoreBuilder(StreamInput in) throws IOException {
            super(in);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public DecayFunction getDecayFunction() {
            return decayFunction;
        }

        private static final DecayFunction decayFunction = new LinearMultScoreFunction();

        private static class LinearMultScoreFunction implements DecayFunction {
            LinearMultScoreFunction() {}

            @Override
            public double evaluate(double value, double scale) {

                return value;
            }

            @Override
            public Explanation explainFunction(String distanceString, double distanceVal, double scale, String functionName) {
                return Explanation.match((float) distanceVal, "" + distanceVal);
            }

            @Override
            public double processScale(double userGivenScale, double userGivenValue) {
                return userGivenScale;
            }
        }
    }
}
