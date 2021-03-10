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

package org.opensearch.rest.action.document;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.Version;
import org.opensearch.action.ActionListener;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.xcontent.XContentType;
import org.opensearch.rest.RestChannel;
import org.opensearch.rest.RestRequest;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.client.NoOpNodeClient;
import org.elasticsearch.test.rest.FakeRestRequest;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestBulkAction}.
 */
public class RestBulkActionTests extends ESTestCase {

    public void testBulkPipelineUpsert() throws Exception {
        SetOnce<Boolean> bulkCalled = new SetOnce<>();
        try (NodeClient verifyingClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void bulk(BulkRequest request, ActionListener<BulkResponse> listener) {
                bulkCalled.set(true);
                assertThat(request.requests(), hasSize(2));
                UpdateRequest updateRequest = (UpdateRequest) request.requests().get(1);
                assertThat(updateRequest.upsertRequest().getPipeline(), equalTo("timestamps"));
            }
        }) {
            final Map<String, String> params = new HashMap<>();
            params.put("pipeline", "timestamps");
            new RestBulkAction(settings(Version.CURRENT).build())
                .handleRequest(
                    new FakeRestRequest.Builder(
                        xContentRegistry()).withPath("my_index/_bulk").withParams(params)
                        .withContent(
                            new BytesArray(
                                "{\"index\":{\"_id\":\"1\"}}\n" +
                                    "{\"field1\":\"val1\"}\n" +
                                    "{\"update\":{\"_id\":\"2\"}}\n" +
                                    "{\"script\":{\"source\":\"ctx._source.counter++;\"},\"upsert\":{\"field1\":\"upserted_val\"}}\n"
                            ),
                            XContentType.JSON
                        ).withMethod(RestRequest.Method.POST).build(),
                    mock(RestChannel.class), verifyingClient
                );
            assertThat(bulkCalled.get(), equalTo(true));
        }
    }
}
