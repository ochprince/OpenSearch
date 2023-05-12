/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.awarenesshealth;

import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.test.AbstractSerializingTestCase;

import java.io.IOException;

public class ClusterAwarenessAttributeValueHealthTests extends AbstractSerializingTestCase<ClusterAwarenessAttributeValueHealth> {

    @Override
    protected Writeable.Reader<ClusterAwarenessAttributeValueHealth> instanceReader() {
        return ClusterAwarenessAttributeValueHealth::new;
    }

    @Override
    protected ClusterAwarenessAttributeValueHealth createTestInstance() {
        return new ClusterAwarenessAttributeValueHealth(
            randomFrom("zone-1", "zone-2", "zone-3"),
            randomInt(1000),
            randomInt(1000),
            randomInt(1000),
            randomInt(1000),
            randomInt(1000),
            randomFrom(0.0, 1.0)
        );
    }

    @Override
    protected ClusterAwarenessAttributeValueHealth doParseInstance(XContentParser parser) throws IOException {
        return ClusterAwarenessAttributeValueHealth.fromXContent(parser);
    }
}
