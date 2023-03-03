/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.pit;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.search.DeletePitRequest;
import com.colasoft.opensearch.action.search.DeletePitResponse;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.SetOnce;
import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.search.RestDeletePitAction;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.client.NoOpNodeClient;
import com.colasoft.opensearch.test.rest.FakeRestChannel;
import com.colasoft.opensearch.test.rest.FakeRestRequest;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests to verify the behavior of rest delete pit action for list delete and delete all PIT endpoints
 */
public class RestDeletePitActionTests extends OpenSearchTestCase {
    public void testParseDeletePitRequestWithInvalidJsonThrowsException() throws Exception {
        RestDeletePitAction action = new RestDeletePitAction();
        RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withContent(
            new BytesArray("{invalid_json}"),
            XContentType.JSON
        ).build();
        Exception e = expectThrows(IllegalArgumentException.class, () -> action.prepareRequest(request, null));
        assertThat(e.getMessage(), equalTo("Failed to parse request body"));
    }

    public void testDeletePitWithBody() throws Exception {
        SetOnce<Boolean> pitCalled = new SetOnce<>();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void deletePits(DeletePitRequest request, ActionListener<DeletePitResponse> listener) {
                pitCalled.set(true);
                assertThat(request.getPitIds(), hasSize(1));
                assertThat(request.getPitIds().get(0), equalTo("BODY"));
            }
        }) {
            RestDeletePitAction action = new RestDeletePitAction();
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withContent(
                new BytesArray("{\"pit_id\": [\"BODY\"]}"),
                XContentType.JSON
            ).build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);
            action.handleRequest(request, channel, nodeClient);

            assertThat(pitCalled.get(), equalTo(true));
        }
    }

    public void testDeleteAllPit() throws Exception {
        SetOnce<Boolean> pitCalled = new SetOnce<>();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void deletePits(DeletePitRequest request, ActionListener<DeletePitResponse> listener) {
                pitCalled.set(true);
                assertThat(request.getPitIds(), hasSize(1));
                assertThat(request.getPitIds().get(0), equalTo("_all"));
            }
        }) {
            RestDeletePitAction action = new RestDeletePitAction();
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withPath("/_all").build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);
            action.handleRequest(request, channel, nodeClient);

            assertThat(pitCalled.get(), equalTo(true));
        }
    }

    public void testDeleteAllPitWithBody() {
        SetOnce<Boolean> pitCalled = new SetOnce<>();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void deletePits(DeletePitRequest request, ActionListener<DeletePitResponse> listener) {
                pitCalled.set(true);
                assertThat(request.getPitIds(), hasSize(1));
                assertThat(request.getPitIds().get(0), equalTo("_all"));
            }
        }) {
            RestDeletePitAction action = new RestDeletePitAction();
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withContent(
                new BytesArray("{\"pit_id\": [\"BODY\"]}"),
                XContentType.JSON
            ).withPath("/_all").build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);

            IllegalArgumentException ex = expectThrows(
                IllegalArgumentException.class,
                () -> action.handleRequest(request, channel, nodeClient)
            );
            assertTrue(ex.getMessage().contains("request [GET /_all] does not support having a body"));
        }
    }

    public void testDeletePitQueryStringParamsShouldThrowException() {
        SetOnce<Boolean> pitCalled = new SetOnce<>();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void deletePits(DeletePitRequest request, ActionListener<DeletePitResponse> listener) {
                pitCalled.set(true);
                assertThat(request.getPitIds(), hasSize(2));
                assertThat(request.getPitIds().get(0), equalTo("QUERY_STRING"));
                assertThat(request.getPitIds().get(1), equalTo("QUERY_STRING_1"));
            }
        }) {
            RestDeletePitAction action = new RestDeletePitAction();
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withParams(
                Collections.singletonMap("pit_id", "QUERY_STRING,QUERY_STRING_1")
            ).build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);
            IllegalArgumentException ex = expectThrows(
                IllegalArgumentException.class,
                () -> action.handleRequest(request, channel, nodeClient)
            );
            assertTrue(ex.getMessage().contains("unrecognized param"));
        }
    }
}
