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

package org.opensearch.index.reindex.remote;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.opensearch.Version;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.client.Request;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.io.Streams;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.opensearch.common.unit.TimeValue.timeValueMillis;
import static org.opensearch.index.reindex.remote.RemoteRequestBuilders.DEPRECATED_URL_ENCODED_INDEX_WARNING;
import static org.opensearch.index.reindex.remote.RemoteRequestBuilders.clearScroll;
import static org.opensearch.index.reindex.remote.RemoteRequestBuilders.initialSearch;
import static org.opensearch.index.reindex.remote.RemoteRequestBuilders.scroll;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link RemoteRequestBuilders} which builds requests for remote version of
 * OpenSearch. Note that unlike most of the rest of OpenSearch this file needs to
 * be compatible with very old versions of OpenSearch. Thus is often uses identifiers
 * for versions like {@code 2000099} for {@code 2.0.0-alpha1}. Do not drop support for
 * features from this file just because the version constants have been removed.
 */
public class RemoteRequestBuildersTests extends OpenSearchTestCase {
    public void testIntialSearchPath() {
        Version remoteVersion = Version.fromId(between(0, Version.CURRENT.id));
        BytesReference query = new BytesArray("{}");

        SearchRequest searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        assertEquals("/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        searchRequest.indices("a");
        searchRequest.types("b");
        assertEquals("/a/b/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        searchRequest.indices("a", "b");
        searchRequest.types("c", "d");
        assertEquals("/a,b/c,d/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        searchRequest.indices("cat,");
        assertEquals("/cat%2C/c,d/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        searchRequest.indices("cat/");
        assertEquals("/cat%2F/c,d/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        searchRequest.indices("cat/", "dog");
        assertEquals("/cat%2F,dog/c,d/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());
        // test a specific date math + all characters that need escaping.
        searchRequest.indices("<cat{now/d}>", "<>/{}|+:,");
        assertEquals("/%3Ccat%7Bnow%2Fd%7D%3E,%3C%3E%2F%7B%7D%7C%2B%3A%2C/c,d/_search",
            initialSearch(searchRequest, query, remoteVersion).getEndpoint());

        // pass-through if already escaped.
        searchRequest.indices("%2f", "%3a");
        assertEquals("/%2f,%3a/c,d/_search", initialSearch(searchRequest, query, remoteVersion).getEndpoint());

        assertWarnings(DEPRECATED_URL_ENCODED_INDEX_WARNING);

        // do not allow , and / if already escaped.
        searchRequest.indices("%2fcat,");
        expectBadStartRequest(searchRequest, "Index", ",", "%2fcat,");
        searchRequest.indices("%3ccat/");
        expectBadStartRequest(searchRequest, "Index", "/", "%3ccat/");

        searchRequest.indices("ok");
        searchRequest.types("cat,");
        expectBadStartRequest(searchRequest, "Type", ",", "cat,");
        searchRequest.types("cat,", "dog");
        expectBadStartRequest(searchRequest, "Type", ",", "cat,");
        searchRequest.types("dog", "cat,");
        expectBadStartRequest(searchRequest, "Type", ",", "cat,");
        searchRequest.types("cat/");
        expectBadStartRequest(searchRequest, "Type", "/", "cat/");
        searchRequest.types("cat/", "dog");
        expectBadStartRequest(searchRequest, "Type", "/", "cat/");
        searchRequest.types("dog", "cat/");
        expectBadStartRequest(searchRequest, "Type", "/", "cat/");
    }

    private void expectBadStartRequest(SearchRequest searchRequest, String type, String bad, String failed) {
        Version remoteVersion = Version.fromId(between(0, Version.CURRENT.id));
        BytesReference query = new BytesArray("{}");
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> initialSearch(searchRequest, query, remoteVersion));
        assertEquals(type + " containing [" + bad + "] not supported but got [" + failed + "]", e.getMessage());
    }

    public void testInitialSearchParamsSort() {
        BytesReference query = new BytesArray("{}");
        SearchRequest searchRequest = new SearchRequest().source(new SearchSourceBuilder());

        // Test sort:_doc for versions that support it.
        Version remoteVersion = Version.fromId(between(2010099, Version.CURRENT.id));
        searchRequest.source().sort("_doc");
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(), hasEntry("sort", "_doc:asc"));

        // Test search_type scan for versions that don't support sort:_doc.
        remoteVersion = Version.fromId(between(0, 2010099 - 1));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(), hasEntry("search_type", "scan"));

        // Test sorting by some field. Version doesn't matter.
        remoteVersion = Version.fromId(between(0, Version.CURRENT.id));
        searchRequest.source().sorts().clear();
        searchRequest.source().sort("foo");
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(), hasEntry("sort", "foo:asc"));
    }

    public void testInitialSearchParamsFields() {
        BytesReference query = new BytesArray("{}");
        SearchRequest searchRequest = new SearchRequest().source(new SearchSourceBuilder());

        // Test request without any fields
        Version remoteVersion = Version.fromId(between(2000099, Version.CURRENT.id));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(),
            not(either(hasKey("stored_fields")).or(hasKey("fields"))));

        // Test stored_fields for versions that support it
        searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        searchRequest.source().storedField("_source").storedField("_id");
        // V_5_0_0_alpha4 => current
        remoteVersion = Version.fromId(between(5000004, Version.CURRENT.id));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(), hasEntry("stored_fields", "_source,_id"));

        // Test fields for versions that support it
        searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        searchRequest.source().storedField("_source").storedField("_id");
        // V_2_0_0 => V_5_0_0_alpha3
        remoteVersion = Version.fromId(between(2000099, 5000003));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(), hasEntry("fields", "_source,_id"));

        // Test extra fields for versions that need it
        searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        searchRequest.source().storedField("_source").storedField("_id");
        remoteVersion = Version.fromId(between(0, 2000099 - 1));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(),
            hasEntry("fields", "_source,_id,_parent,_routing,_ttl"));

        // But only versions before 1.0 force _source to be in the list
        searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        searchRequest.source().storedField("_id");
        remoteVersion = Version.fromId(between(1000099, 2000099 - 1));
        assertThat(initialSearch(searchRequest, query, remoteVersion).getParameters(),
            hasEntry("fields", "_id,_parent,_routing,_ttl"));
    }

    public void testInitialSearchParamsMisc() {
        BytesReference query = new BytesArray("{}");
        SearchRequest searchRequest = new SearchRequest().source(new SearchSourceBuilder());
        Version remoteVersion = Version.fromId(between(0, Version.CURRENT.id));

        TimeValue scroll = null;
        if (randomBoolean()) {
            scroll = TimeValue.parseTimeValue(randomPositiveTimeValue(), "test");
            searchRequest.scroll(scroll);
        }
        int size = between(0, Integer.MAX_VALUE);
        searchRequest.source().size(size);
        Boolean fetchVersion = null;
        if (randomBoolean()) {
            fetchVersion = randomBoolean();
            searchRequest.source().version(fetchVersion);
        }

        Map<String, String> params = initialSearch(searchRequest, query, remoteVersion).getParameters();

        if (scroll == null) {
            assertThat(params, not(hasKey("scroll")));
        } else {
            assertScroll(remoteVersion, params, scroll);
        }
        assertThat(params, hasEntry("size", Integer.toString(size)));
        if (fetchVersion != null) {
            assertThat(params, fetchVersion ? hasEntry("version", Boolean.TRUE.toString()) :
                hasEntry("version", Boolean.FALSE.toString()));
        } else {
            assertThat(params, hasEntry("version", Boolean.FALSE.toString()));
        }
    }

    public void testInitialSearchDisallowPartialResults() {
        final String allowPartialParamName = "allow_partial_search_results";
        final int v6_3 = 6030099;

        BytesReference query = new BytesArray("{}");
        SearchRequest searchRequest = new SearchRequest().source(new SearchSourceBuilder());

        Version disallowVersion = Version.fromId(between(v6_3, Version.CURRENT.id));
        Map<String, String> params = initialSearch(searchRequest, query, disallowVersion).getParameters();
        assertEquals("false", params.get(allowPartialParamName));

        Version allowVersion = Version.fromId(between(0, v6_3-1));
        params = initialSearch(searchRequest, query, allowVersion).getParameters();
        assertThat(params.keySet(), not(contains(allowPartialParamName)));
    }

    private void assertScroll(Version remoteVersion, Map<String, String> params, TimeValue requested) {
        // V_5_0_0
        if (remoteVersion.before(Version.fromId(5000099))) {
            // Versions of Elasticsearch prior to 5.0 can't parse nanos or micros in TimeValue.
            assertThat(params.get("scroll"), not(either(endsWith("nanos")).or(endsWith("micros"))));
            if (requested.getStringRep().endsWith("nanos") || requested.getStringRep().endsWith("micros")) {
                long millis = (long) Math.ceil(requested.millisFrac());
                assertEquals(TimeValue.parseTimeValue(params.get("scroll"), "scroll"), timeValueMillis(millis));
                return;
            }
        }
        assertEquals(requested, TimeValue.parseTimeValue(params.get("scroll"), "scroll"));
    }

    public void testInitialSearchEntity() throws IOException {
        Version remoteVersion = Version.fromId(between(0, Version.CURRENT.id));

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.source(new SearchSourceBuilder());
        String query = "{\"match_all\":{}}";
        HttpEntity entity = initialSearch(searchRequest, new BytesArray(query), remoteVersion).getEntity();
        assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
        if (remoteVersion.onOrAfter(Version.fromId(1000099))) {
            assertEquals("{\"query\":" + query + ",\"_source\":true}",
                Streams.copyToString(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)));
        } else {
            assertEquals("{\"query\":" + query + "}",
                Streams.copyToString(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)));
        }

        // Source filtering is included if set up
        searchRequest.source().fetchSource(new String[]{"in1", "in2"}, new String[]{"out"});
        entity = initialSearch(searchRequest, new BytesArray(query), remoteVersion).getEntity();
        assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
        assertEquals("{\"query\":" + query + ",\"_source\":{\"includes\":[\"in1\",\"in2\"],\"excludes\":[\"out\"]}}",
            Streams.copyToString(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)));

        // Invalid XContent fails
        RuntimeException e = expectThrows(RuntimeException.class,
            () -> initialSearch(searchRequest, new BytesArray("{}, \"trailing\": {}"), remoteVersion));
        assertThat(e.getCause().getMessage(), containsString("Unexpected character (',' (code 44))"));
        e = expectThrows(RuntimeException.class, () -> initialSearch(searchRequest, new BytesArray("{"), remoteVersion));
        assertThat(e.getCause().getMessage(), containsString("Unexpected end-of-input"));
    }

    public void testScrollParams() {
        String scroll = randomAlphaOfLength(30);
        Version remoteVersion = Version.fromId(between(0, Version.CURRENT.id));
        TimeValue keepAlive = TimeValue.parseTimeValue(randomPositiveTimeValue(), "test");
        assertScroll(remoteVersion, scroll(scroll, keepAlive, remoteVersion).getParameters(), keepAlive);
    }

    public void testScrollEntity() throws IOException {
        String scroll = randomAlphaOfLength(30);
        HttpEntity entity = scroll(scroll, timeValueMillis(between(1, 1000)), Version.fromString("5.0.0")).getEntity();
        assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
        assertThat(Streams.copyToString(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)),
            containsString("\"" + scroll + "\""));

        // Test with version < 2.0.0
        entity = scroll(scroll, timeValueMillis(between(1, 1000)), Version.fromId(1070499)).getEntity();
        assertEquals(ContentType.TEXT_PLAIN.toString(), entity.getContentType().getValue());
        assertEquals(scroll, Streams.copyToString(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8)));
    }

    public void testClearScroll() throws IOException {
        String scroll = randomAlphaOfLength(30);
        Request request = clearScroll(scroll, Version.fromString("5.0.0"));
        assertEquals(ContentType.APPLICATION_JSON.toString(), request.getEntity().getContentType().getValue());
        assertThat(Streams.copyToString(new InputStreamReader(request.getEntity().getContent(), StandardCharsets.UTF_8)),
            containsString("\"" + scroll + "\""));
        assertThat(request.getParameters().keySet(), empty());

        // Test with version < 2.0.0
        request = clearScroll(scroll, Version.fromId(1070499));
        assertEquals(ContentType.TEXT_PLAIN.toString(), request.getEntity().getContentType().getValue());
        assertEquals(scroll, Streams.copyToString(new InputStreamReader(request.getEntity().getContent(), StandardCharsets.UTF_8)));
        assertThat(request.getParameters().keySet(), empty());
    }
}
