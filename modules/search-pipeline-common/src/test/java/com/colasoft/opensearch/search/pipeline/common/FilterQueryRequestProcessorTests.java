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

package com.colasoft.opensearch.search.pipeline.common;

import com.colasoft.opensearch.action.search.SearchRequest;
import com.colasoft.opensearch.index.query.BoolQueryBuilder;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.search.builder.SearchSourceBuilder;
import com.colasoft.opensearch.test.AbstractBuilderTestCase;

import java.util.Collections;
import java.util.Map;

public class FilterQueryRequestProcessorTests extends AbstractBuilderTestCase {

    public void testFilterQuery() throws Exception {
        QueryBuilder filterQuery = new TermQueryBuilder("field", "value");
        FilterQueryRequestProcessor filterQueryRequestProcessor = new FilterQueryRequestProcessor(null, null, filterQuery);
        QueryBuilder incomingQuery = new TermQueryBuilder("text", "foo");
        SearchSourceBuilder source = new SearchSourceBuilder().query(incomingQuery);
        SearchRequest request = new SearchRequest().source(source);
        SearchRequest transformedRequest = filterQueryRequestProcessor.processRequest(request);
        assertEquals(new BoolQueryBuilder().must(incomingQuery).filter(filterQuery), transformedRequest.source().query());

        // Test missing incoming query
        request = new SearchRequest();
        transformedRequest = filterQueryRequestProcessor.processRequest(request);
        assertEquals(new BoolQueryBuilder().filter(filterQuery), transformedRequest.source().query());
    }

    public void testFactory() throws Exception {
        FilterQueryRequestProcessor.Factory factory = new FilterQueryRequestProcessor.Factory(this.xContentRegistry());
        FilterQueryRequestProcessor processor = factory.create(
            Collections.emptyMap(),
            null,
            null,
            Map.of("query", Map.of("term", Map.of("field", "value")))
        );
        assertEquals(new TermQueryBuilder("field", "value"), processor.filterQuery);

        // Missing "query" parameter:
        expectThrows(IllegalArgumentException.class, () -> factory.create(Collections.emptyMap(), null, null, Collections.emptyMap()));
    }
}
