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

package com.colasoft.opensearch.extensions;

import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.BytesStreamInput;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.test.OpenSearchTestCase;

public class ExtensionResponseTests extends OpenSearchTestCase {

    public void testAcknowledgedResponse() throws Exception {
        boolean response = true;
        AcknowledgedResponse booleanResponse = new AcknowledgedResponse(response);

        assertEquals(response, booleanResponse.getStatus());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            booleanResponse.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                booleanResponse = new AcknowledgedResponse(in);

                assertEquals(response, booleanResponse.getStatus());
            }
        }
    }
}
