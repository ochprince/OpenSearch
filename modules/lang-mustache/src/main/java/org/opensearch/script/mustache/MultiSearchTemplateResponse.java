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

package org.opensearch.script.mustache;

import org.opensearch.OpenSearchException;
import org.opensearch.Version;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.search.MultiSearchResponse;
import org.elasticsearch.common.Nullable;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class MultiSearchTemplateResponse extends ActionResponse implements Iterable<MultiSearchTemplateResponse.Item>, ToXContentObject {

    /**
     * A search template response item, holding the actual search template response, or an error message if it failed.
     */
    public static class Item implements Writeable {
        private final SearchTemplateResponse response;
        private final Exception exception;

        private Item(StreamInput in) throws IOException {
            if (in.readBoolean()) {
                this.response = new SearchTemplateResponse(in);
                this.exception = null;
            } else {
                exception = in.readException();
                this.response = null;
            }
        }

        public Item(SearchTemplateResponse response, Exception exception) {
            this.response = response;
            this.exception = exception;
        }

        /**
         * Is it a failed search?
         */
        public boolean isFailure() {
            return exception != null;
        }

        /**
         * The actual failure message, null if its not a failure.
         */
        @Nullable
        public String getFailureMessage() {
            return exception == null ? null : exception.getMessage();
        }

        /**
         * The actual search response, null if its a failure.
         */
        @Nullable
        public SearchTemplateResponse getResponse() {
            return this.response;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            if (response != null) {
                out.writeBoolean(true);
                response.writeTo(out);
            } else {
                out.writeBoolean(false);
                out.writeException(exception);
            }
        }

        public Exception getFailure() {
            return exception;
        }

        @Override
        public String toString() {
            return "Item [response=" + response + ", exception=" + exception + "]";
        }
    }

    private final Item[] items;
    private final long tookInMillis;

    MultiSearchTemplateResponse(StreamInput in) throws IOException {
        super(in);
        items = in.readArray(Item::new, Item[]::new);
        if (in.getVersion().onOrAfter(Version.V_7_0_0)) {
            tookInMillis = in.readVLong();
        } else {
            tookInMillis = -1L;
        }
    }

    MultiSearchTemplateResponse(Item[] items, long tookInMillis) {
        this.items = items;
        this.tookInMillis = tookInMillis;
    }

    @Override
    public Iterator<Item> iterator() {
        return Arrays.stream(items).iterator();
    }

    /**
     * The list of responses, the order is the same as the one provided in the request.
     */
    public Item[] getResponses() {
        return this.items;
    }

    /**
     * How long the msearch_template took.
     */
    public TimeValue getTook() {
        return new TimeValue(tookInMillis);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeArray(items);
        if (out.getVersion().onOrAfter(Version.V_7_0_0)) {
            out.writeVLong(tookInMillis);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field("took", tookInMillis);
        builder.startArray(Fields.RESPONSES);
        for (Item item : items) {
            if (item.isFailure()) {
                builder.startObject();
                OpenSearchException.generateFailureXContent(builder, params, item.getFailure(), true);
                builder.endObject();
            } else {
                item.getResponse().toXContent(builder, params);
            }
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    static final class Fields {
        static final String RESPONSES = "responses";
    }

    public static MultiSearchTemplateResponse fromXContext(XContentParser parser) {
        //The MultiSearchTemplateResponse is identical to the multi search response so we reuse the parsing logic in multi search response
        MultiSearchResponse mSearchResponse = MultiSearchResponse.fromXContext(parser);
        org.opensearch.action.search.MultiSearchResponse.Item[] responses = mSearchResponse.getResponses();
        Item[] templateResponses = new Item[responses.length];
        int i = 0;
        for (org.opensearch.action.search.MultiSearchResponse.Item item : responses) {
            SearchTemplateResponse stResponse = null;
            if(item.getResponse() != null){
                stResponse = new SearchTemplateResponse();
                stResponse.setResponse(item.getResponse());
            }
            templateResponses[i++] = new Item(stResponse, item.getFailure());
        }
        return new MultiSearchTemplateResponse(templateResponses, mSearchResponse.getTook().millis());
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
