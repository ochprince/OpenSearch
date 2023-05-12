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

package com.colasoft.opensearch.search.aggregations;

import com.colasoft.opensearch.action.index.IndexRequestBuilder;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.search.aggregations.bucket.filter.InternalFilter;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMax;
import com.colasoft.opensearch.search.aggregations.metrics.MaxAggregationBuilder;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class DelayedShardAggregationIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(DelayedShardAggregationPlugin.class);
    }

    public void testSimple() throws Exception {
        assertAcked(client().admin().indices().prepareCreate("index"));
        float expectedMax = Float.MIN_VALUE;
        List<IndexRequestBuilder> reqs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            float rand = randomFloat();
            expectedMax = Math.max(rand, expectedMax);
            reqs.add(client().prepareIndex("index").setSource("number", rand));
        }
        indexRandom(true, reqs);
        SearchResponse response = client().prepareSearch("index")
            .addAggregation(
                new DelayedShardAggregationBuilder("delay", TimeValue.timeValueMillis(10)).subAggregation(
                    new MaxAggregationBuilder("max").field("number")
                )
            )
            .get();
        Aggregations aggs = response.getAggregations();
        assertThat(aggs.get("delay"), instanceOf(InternalFilter.class));
        InternalFilter filter = aggs.get("delay");
        InternalMax max = filter.getAggregations().get("max");
        assertThat((float) max.getValue(), equalTo(expectedMax));
    }
}
