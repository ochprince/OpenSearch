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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness;

import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateResponse;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

public class DeleteDecommissionStateResponseTests extends OpenSearchTestCase {

    public void testSerialization() throws IOException {
        final DeleteDecommissionStateResponse originalResponse = new DeleteDecommissionStateResponse(true);

        final DeleteDecommissionStateResponse deserialized = copyWriteable(
            originalResponse,
            writableRegistry(),
            DeleteDecommissionStateResponse::new
        );
        assertEquals(deserialized, originalResponse);

    }
}
