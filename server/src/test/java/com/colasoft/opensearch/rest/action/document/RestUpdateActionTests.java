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

package com.colasoft.opensearch.rest.action.document;

import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.index.VersionType;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.test.rest.FakeRestRequest;
import com.colasoft.opensearch.test.rest.RestActionTestCase;
import org.junit.Before;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.mock;

public class RestUpdateActionTests extends RestActionTestCase {

    private RestUpdateAction action;

    @Before
    public void setUpAction() {
        action = new RestUpdateAction();
        controller().registerHandler(action);
    }

    public void testUpdateDocVersion() {
        Map<String, String> params = new HashMap<>();
        if (randomBoolean()) {
            params.put("version", Long.toString(randomNonNegativeLong()));
            params.put("version_type", randomFrom(VersionType.values()).name());
        } else if (randomBoolean()) {
            params.put("version", Long.toString(randomNonNegativeLong()));
        } else {
            params.put("version_type", randomFrom(VersionType.values()).name());
        }
        String content = "{\n" + "    \"doc\" : {\n" + "        \"name\" : \"new_name\"\n" + "    }\n" + "}";
        FakeRestRequest updateRequest = new FakeRestRequest.Builder(xContentRegistry()).withMethod(RestRequest.Method.POST)
            .withPath("test/_update/1")
            .withParams(params)
            .withContent(new BytesArray(content), XContentType.JSON)
            .build();
        ActionRequestValidationException e = expectThrows(
            ActionRequestValidationException.class,
            () -> action.prepareRequest(updateRequest, mock(NodeClient.class))
        );
        assertThat(
            e.getMessage(),
            containsString(
                "internal versioning can not be used for optimistic concurrency control. "
                    + "Please use `if_seq_no` and `if_primary_term` instead"
            )
        );
    }
}
