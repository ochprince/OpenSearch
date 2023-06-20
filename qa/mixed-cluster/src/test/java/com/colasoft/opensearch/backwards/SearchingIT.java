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

package com.colasoft.opensearch.backwards;

import org.apache.http.HttpHost;
import com.colasoft.opensearch.action.get.MultiGetRequest;
import com.colasoft.opensearch.action.get.MultiGetResponse;
import com.colasoft.opensearch.client.Request;
import com.colasoft.opensearch.client.RequestOptions;
import com.colasoft.opensearch.client.Response;
import com.colasoft.opensearch.client.RestClient;
import com.colasoft.opensearch.client.RestHighLevelClient;
import com.colasoft.opensearch.test.rest.OpenSearchRestTestCase;
import com.colasoft.opensearch.test.rest.yaml.ObjectPath;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class SearchingIT extends OpenSearchRestTestCase {
    public void testMultiGet() throws Exception {
        final Set<HttpHost> nodes = buildNodes();

        final MultiGetRequest multiGetRequest = new MultiGetRequest();
        multiGetRequest.add("index", "id1");

        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(nodes.toArray(HttpHost[]::new)))) {
            MultiGetResponse response = client.mget(multiGetRequest, RequestOptions.DEFAULT);
            assertEquals(1, response.getResponses().length);

            assertTrue(response.getResponses()[0].isFailed());
            assertNotNull(response.getResponses()[0].getFailure());
            assertEquals(response.getResponses()[0].getFailure().getId(), "id1");
            assertEquals(response.getResponses()[0].getFailure().getIndex(), "index");
            assertThat(response.getResponses()[0].getFailure().getMessage(), containsString("no such index [index]"));
       }
    }

    private Set<HttpHost> buildNodes() throws IOException, URISyntaxException {
        Response response = client().performRequest(new Request("GET", "_nodes"));
        ObjectPath objectPath = ObjectPath.createFromResponse(response);
        Map<String, Object> nodesAsMap = objectPath.evaluate("nodes");
        final Set<HttpHost> nodes = new HashSet<>();
        for (String id : nodesAsMap.keySet()) {
            nodes.add(HttpHost.create((String) objectPath.evaluate("nodes." + id + ".http.publish_address")));
        }

        return nodes;
    }
}
