/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.pit;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.search.CreatePitRequest;
import com.colasoft.opensearch.action.search.CreatePitResponse;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.SetOnce;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.action.search.RestCreatePitAction;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.client.NoOpNodeClient;
import com.colasoft.opensearch.test.rest.FakeRestChannel;
import com.colasoft.opensearch.test.rest.FakeRestRequest;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 * Tests to verify behavior of create pit rest action
 */
public class RestCreatePitActionTests extends OpenSearchTestCase {
    public void testRestCreatePit() throws Exception {
        SetOnce<Boolean> createPitCalled = new SetOnce<>();
        RestCreatePitAction action = new RestCreatePitAction();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void createPit(CreatePitRequest request, ActionListener<CreatePitResponse> listener) {
                createPitCalled.set(true);
                assertThat(request.getKeepAlive().getStringRep(), equalTo("1m"));
                assertFalse(request.shouldAllowPartialPitCreation());
            }
        }) {
            Map<String, String> params = new HashMap<>();
            params.put("keep_alive", "1m");
            params.put("allow_partial_pit_creation", "false");
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withParams(params)
                .withMethod(RestRequest.Method.POST)
                .build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);
            action.handleRequest(request, channel, nodeClient);

            assertThat(createPitCalled.get(), equalTo(true));
        }
    }

    public void testRestCreatePitDefaultPartialCreation() throws Exception {
        SetOnce<Boolean> createPitCalled = new SetOnce<>();
        RestCreatePitAction action = new RestCreatePitAction();
        try (NodeClient nodeClient = new NoOpNodeClient(this.getTestName()) {
            @Override
            public void createPit(CreatePitRequest request, ActionListener<CreatePitResponse> listener) {
                createPitCalled.set(true);
                assertThat(request.getKeepAlive().getStringRep(), equalTo("1m"));
                assertTrue(request.shouldAllowPartialPitCreation());
            }
        }) {
            Map<String, String> params = new HashMap<>();
            params.put("keep_alive", "1m");
            RestRequest request = new FakeRestRequest.Builder(xContentRegistry()).withParams(params)
                .withMethod(RestRequest.Method.POST)
                .build();
            FakeRestChannel channel = new FakeRestChannel(request, false, 0);
            action.handleRequest(request, channel, nodeClient);

            assertThat(createPitCalled.get(), equalTo(true));
        }
    }
}
