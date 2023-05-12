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

package com.colasoft.opensearch.http;

import java.io.IOException;

import org.apache.http.util.EntityUtils;
import com.colasoft.opensearch.client.Request;
import com.colasoft.opensearch.client.Response;
import com.colasoft.opensearch.client.ResponseException;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.ClusterScope;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase.Scope;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Tests that when disabling detailed errors, a request with the error_trace parameter returns an HTTP 400 response.
 */
@ClusterScope(scope = Scope.TEST, supportsDedicatedMasters = false, numDataNodes = 1)
public class DetailedErrorsDisabledIT extends HttpSmokeTestCase {

    // Build our cluster settings
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(HttpTransportSettings.SETTING_HTTP_DETAILED_ERRORS_ENABLED.getKey(), false)
                .build();
    }

    public void testThatErrorTraceParamReturns400() throws IOException {
        Request request = new Request("DELETE", "/");
        request.addParameter("error_trace", "true");
        ResponseException e = expectThrows(ResponseException.class, () ->
            getRestClient().performRequest(request));

        Response response = e.getResponse();
        assertThat(response.getHeader("Content-Type"), is("application/json; charset=UTF-8"));
        assertThat(EntityUtils.toString(e.getResponse().getEntity()),
                   containsString("\"error\":\"error traces in responses are disabled.\""));
        assertThat(response.getStatusLine().getStatusCode(), is(400));
    }
}
