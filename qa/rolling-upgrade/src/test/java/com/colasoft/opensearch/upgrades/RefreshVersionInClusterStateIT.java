/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.upgrades;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.client.Request;
import com.colasoft.opensearch.client.Response;
import com.colasoft.opensearch.common.io.Streams;

import java.io.IOException;

public class RefreshVersionInClusterStateIT extends AbstractRollingTestCase {

    /*
    This test ensures that after the upgrade from ElasticSearch/ OpenSearch all nodes report the version on and after 1.0.0
     */
    public void testRefresh() throws IOException {
        switch (CLUSTER_TYPE) {
            case OLD:
            case MIXED:
                break;
            case UPGRADED:
                Response response = client().performRequest(new Request("GET", "/_cat/nodes?h=id,version"));
                for (String nodeLine : Streams.readAllLines(response.getEntity().getContent())) {
                    String[] elements = nodeLine.split(" +");
                    assertEquals(Version.fromString(elements[1]), Version.CURRENT);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown cluster type [" + CLUSTER_TYPE + "]");
        }
    }
}
