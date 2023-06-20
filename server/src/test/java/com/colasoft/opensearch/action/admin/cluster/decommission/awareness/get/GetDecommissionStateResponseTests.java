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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get;

import com.colasoft.opensearch.cluster.decommission.DecommissionStatus;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.test.AbstractXContentTestCase;

import java.io.IOException;

public class GetDecommissionStateResponseTests extends AbstractXContentTestCase<GetDecommissionStateResponse> {
    @Override
    protected GetDecommissionStateResponse createTestInstance() {
        DecommissionStatus status = null;
        String attributeValue = null;
        if (randomBoolean()) {
            status = randomFrom(DecommissionStatus.values());
            attributeValue = randomAlphaOfLength(10);
        }
        return new GetDecommissionStateResponse(attributeValue, status);
    }

    @Override
    protected GetDecommissionStateResponse doParseInstance(XContentParser parser) throws IOException {
        return GetDecommissionStateResponse.fromXContent(parser);
    }

    @Override
    protected boolean supportsUnknownFields() {
        return false;
    }
}
