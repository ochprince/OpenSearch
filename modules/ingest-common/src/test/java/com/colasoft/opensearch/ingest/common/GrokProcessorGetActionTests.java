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

package com.colasoft.opensearch.ingest.common;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.common.xcontent.json.JsonXContent;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.transport.TransportService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class GrokProcessorGetActionTests extends OpenSearchTestCase {
    private static final Map<String, String> TEST_PATTERNS = Map.of("PATTERN2", "foo2", "PATTERN1", "foo1");

    public void testRequest() throws Exception {
        GrokProcessorGetAction.Request request = new GrokProcessorGetAction.Request(false);
        BytesStreamOutput out = new BytesStreamOutput();
        request.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GrokProcessorGetAction.Request otherRequest = new GrokProcessorGetAction.Request(streamInput);
        assertThat(otherRequest.validate(), nullValue());
    }

    public void testResponseSerialization() throws Exception {
        GrokProcessorGetAction.Response response = new GrokProcessorGetAction.Response(TEST_PATTERNS);
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        StreamInput streamInput = out.bytes().streamInput();
        GrokProcessorGetAction.Response otherResponse = new GrokProcessorGetAction.Response(streamInput);
        assertThat(response.getGrokPatterns(), equalTo(TEST_PATTERNS));
        assertThat(response.getGrokPatterns(), equalTo(otherResponse.getGrokPatterns()));
    }

    public void testResponseSorting() {
        List<String> sortedKeys = new ArrayList<>(TEST_PATTERNS.keySet());
        Collections.sort(sortedKeys);
        GrokProcessorGetAction.TransportAction transportAction = new GrokProcessorGetAction.TransportAction(
            mock(TransportService.class),
            mock(ActionFilters.class),
            TEST_PATTERNS
        );
        GrokProcessorGetAction.Response[] receivedResponse = new GrokProcessorGetAction.Response[1];
        transportAction.doExecute(null, new GrokProcessorGetAction.Request(true), new ActionListener<GrokProcessorGetAction.Response>() {
            @Override
            public void onResponse(GrokProcessorGetAction.Response response) {
                receivedResponse[0] = response;
            }

            @Override
            public void onFailure(Exception e) {
                fail();
            }
        });
        assertThat(receivedResponse[0], notNullValue());
        assertThat(receivedResponse[0].getGrokPatterns().keySet().toArray(), equalTo(sortedKeys.toArray()));

        GrokProcessorGetAction.Response firstResponse = receivedResponse[0];
        transportAction.doExecute(null, new GrokProcessorGetAction.Request(true), new ActionListener<GrokProcessorGetAction.Response>() {
            @Override
            public void onResponse(GrokProcessorGetAction.Response response) {
                receivedResponse[0] = response;
            }

            @Override
            public void onFailure(Exception e) {
                fail();
            }
        });
        assertThat(receivedResponse[0], notNullValue());
        assertThat(receivedResponse[0], not(sameInstance(firstResponse)));
        assertThat(receivedResponse[0].getGrokPatterns(), sameInstance(firstResponse.getGrokPatterns()));
    }

    @SuppressWarnings("unchecked")
    public void testResponseToXContent() throws Exception {
        GrokProcessorGetAction.Response response = new GrokProcessorGetAction.Response(TEST_PATTERNS);
        try (XContentBuilder builder = JsonXContent.contentBuilder()) {
            response.toXContent(builder, ToXContent.EMPTY_PARAMS);
            Map<String, Object> converted = XContentHelper.convertToMap(BytesReference.bytes(builder), false, builder.contentType()).v2();
            Map<String, String> patterns = (Map<String, String>) converted.get("patterns");
            assertThat(patterns.size(), equalTo(2));
            assertThat(patterns.get("PATTERN1"), equalTo("foo1"));
            assertThat(patterns.get("PATTERN2"), equalTo("foo2"));
        }
    }
}
