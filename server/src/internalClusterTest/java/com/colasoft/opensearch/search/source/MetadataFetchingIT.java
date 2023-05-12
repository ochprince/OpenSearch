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

package com.colasoft.opensearch.search.source;

import org.apache.lucene.search.join.ScoreMode;
import com.colasoft.opensearch.ExceptionsHelper;
import com.colasoft.opensearch.action.search.SearchPhaseExecutionException;
import com.colasoft.opensearch.action.search.SearchResponse;
import com.colasoft.opensearch.index.query.InnerHitBuilder;
import com.colasoft.opensearch.index.query.NestedQueryBuilder;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.search.SearchException;
import com.colasoft.opensearch.search.SearchHits;
import com.colasoft.opensearch.search.fetch.subphase.FetchSourceContext;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import java.util.Collections;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class MetadataFetchingIT extends OpenSearchIntegTestCase {
    public void testSimple() {
        assertAcked(prepareCreate("test"));
        ensureGreen();

        client().prepareIndex("test").setId("1").setSource("field", "value").get();
        refresh();

        SearchResponse response = client().prepareSearch("test").storedFields("_none_").setFetchSource(false).setVersion(true).get();
        assertThat(response.getHits().getAt(0).getId(), nullValue());
        assertThat(response.getHits().getAt(0).getSourceAsString(), nullValue());
        assertThat(response.getHits().getAt(0).getVersion(), notNullValue());

        response = client().prepareSearch("test").storedFields("_none_").get();
        assertThat(response.getHits().getAt(0).getId(), nullValue());
        assertThat(response.getHits().getAt(0).getSourceAsString(), nullValue());
    }

    public void testInnerHits() {
        assertAcked(prepareCreate("test").setMapping("nested", "type=nested"));
        ensureGreen();
        client().prepareIndex("test").setId("1").setSource("field", "value", "nested", Collections.singletonMap("title", "foo")).get();
        refresh();

        SearchResponse response = client().prepareSearch("test")
            .storedFields("_none_")
            .setFetchSource(false)
            .setQuery(
                new NestedQueryBuilder("nested", new TermQueryBuilder("nested.title", "foo"), ScoreMode.Total).innerHit(
                    new InnerHitBuilder().setStoredFieldNames(Collections.singletonList("_none_"))
                        .setFetchSourceContext(new FetchSourceContext(false))
                )
            )
            .get();
        assertThat(response.getHits().getTotalHits().value, equalTo(1L));
        assertThat(response.getHits().getAt(0).getId(), nullValue());
        assertThat(response.getHits().getAt(0).getSourceAsString(), nullValue());
        assertThat(response.getHits().getAt(0).getInnerHits().size(), equalTo(1));
        SearchHits hits = response.getHits().getAt(0).getInnerHits().get("nested");
        assertThat(hits.getTotalHits().value, equalTo(1L));
        assertThat(hits.getAt(0).getId(), nullValue());
        assertThat(hits.getAt(0).getSourceAsString(), nullValue());
    }

    public void testWithRouting() {
        assertAcked(prepareCreate("test"));
        ensureGreen();

        client().prepareIndex("test").setId("1").setSource("field", "value").setRouting("toto").get();
        refresh();

        SearchResponse response = client().prepareSearch("test").storedFields("_none_").setFetchSource(false).get();
        assertThat(response.getHits().getAt(0).getId(), nullValue());
        assertThat(response.getHits().getAt(0).field("_routing"), nullValue());
        assertThat(response.getHits().getAt(0).getSourceAsString(), nullValue());

        response = client().prepareSearch("test").storedFields("_none_").get();
        assertThat(response.getHits().getAt(0).getId(), nullValue());
        assertThat(response.getHits().getAt(0).getSourceAsString(), nullValue());
    }

    public void testInvalid() {
        assertAcked(prepareCreate("test"));
        ensureGreen();

        index("test", "type1", "1", "field", "value");
        refresh();

        {
            SearchPhaseExecutionException exc = expectThrows(
                SearchPhaseExecutionException.class,
                () -> client().prepareSearch("test").setFetchSource(true).storedFields("_none_").get()
            );
            Throwable rootCause = ExceptionsHelper.unwrap(exc, SearchException.class);
            assertNotNull(rootCause);
            assertThat(rootCause.getClass(), equalTo(SearchException.class));
            assertThat(rootCause.getMessage(), equalTo("[stored_fields] cannot be disabled if [_source] is requested"));
        }
        {
            SearchPhaseExecutionException exc = expectThrows(
                SearchPhaseExecutionException.class,
                () -> client().prepareSearch("test").storedFields("_none_").addFetchField("field").get()
            );
            Throwable rootCause = ExceptionsHelper.unwrap(exc, SearchException.class);
            assertNotNull(rootCause);
            assertThat(rootCause.getClass(), equalTo(SearchException.class));
            assertThat(rootCause.getMessage(), equalTo("[stored_fields] cannot be disabled when using the [fields] option"));
        }
        {
            IllegalArgumentException exc = expectThrows(
                IllegalArgumentException.class,
                () -> client().prepareSearch("test").storedFields("_none_", "field1").setVersion(true).get()
            );
            assertThat(exc.getMessage(), equalTo("cannot combine _none_ with other fields"));
        }
        {
            IllegalArgumentException exc = expectThrows(
                IllegalArgumentException.class,
                () -> client().prepareSearch("test").storedFields("_none_").storedFields("field1").setVersion(true).get()
            );
            assertThat(exc.getMessage(), equalTo("cannot combine _none_ with other fields"));
        }
    }
}
