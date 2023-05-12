/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.pipeline.common;

import com.colasoft.opensearch.action.search.SearchRequest;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.xcontent.LoggingDeprecationHandler;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.common.xcontent.json.JsonXContent;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.BoolQueryBuilder;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.search.builder.SearchSourceBuilder;
import com.colasoft.opensearch.search.pipeline.Processor;
import com.colasoft.opensearch.search.pipeline.SearchRequestProcessor;

import java.io.InputStream;
import java.util.Map;

import static com.colasoft.opensearch.index.query.AbstractQueryBuilder.parseInnerQueryBuilder;

/**
 * This is a {@link SearchRequestProcessor} that replaces the incoming query with a BooleanQuery
 * that MUST match the incoming query with a FILTER clause based on the configured query.
 */
public class FilterQueryRequestProcessor extends AbstractProcessor implements SearchRequestProcessor {
    /**
     * Key to reference this processor type from a search pipeline.
     */
    public static final String TYPE = "filter_query";

    final QueryBuilder filterQuery;

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Constructor that takes a filter query.
     *
     * @param tag         processor tag
     * @param description processor description
     * @param filterQuery the query that will be added as a filter to incoming queries
     */
    public FilterQueryRequestProcessor(String tag, String description, QueryBuilder filterQuery) {
        super(tag, description);
        this.filterQuery = filterQuery;
    }

    @Override
    public SearchRequest processRequest(SearchRequest request) throws Exception {
        QueryBuilder originalQuery = null;
        if (request.source() != null) {
            originalQuery = request.source().query();
        }

        BoolQueryBuilder filteredQuery = new BoolQueryBuilder().filter(filterQuery);
        if (originalQuery != null) {
            filteredQuery.must(originalQuery);
        }
        if (request.source() == null) {
            request.source(new SearchSourceBuilder());
        }
        request.source().query(filteredQuery);
        return request;
    }

    static class Factory implements Processor.Factory {
        private final NamedXContentRegistry namedXContentRegistry;
        public static final ParseField QUERY_FIELD = new ParseField("query");

        Factory(NamedXContentRegistry namedXContentRegistry) {
            this.namedXContentRegistry = namedXContentRegistry;
        }

        @Override
        public FilterQueryRequestProcessor create(
            Map<String, Processor.Factory> processorFactories,
            String tag,
            String description,
            Map<String, Object> config
        ) throws Exception {
            try (
                XContentBuilder builder = XContentBuilder.builder(JsonXContent.jsonXContent).map(config);
                InputStream stream = BytesReference.bytes(builder).streamInput();
                XContentParser parser = XContentType.JSON.xContent()
                    .createParser(namedXContentRegistry, LoggingDeprecationHandler.INSTANCE, stream)
            ) {
                XContentParser.Token token = parser.nextToken();
                assert token == XContentParser.Token.START_OBJECT;
                String currentFieldName = null;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else if (token == XContentParser.Token.START_OBJECT) {
                        if (QUERY_FIELD.match(currentFieldName, parser.getDeprecationHandler())) {
                            return new FilterQueryRequestProcessor(tag, description, parseInnerQueryBuilder(parser));
                        }
                    }
                }
            }
            throw new IllegalArgumentException(
                "Did not specify the " + QUERY_FIELD.getPreferredName() + " property in processor of type " + TYPE
            );
        }
    }
}
