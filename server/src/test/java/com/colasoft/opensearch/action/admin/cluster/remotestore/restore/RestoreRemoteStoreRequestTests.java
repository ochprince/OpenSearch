/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.remotestore.restore;

import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.test.AbstractWireSerializingTestCase;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;

public class RestoreRemoteStoreRequestTests extends AbstractWireSerializingTestCase<RestoreRemoteStoreRequest> {
    private RestoreRemoteStoreRequest randomState(RestoreRemoteStoreRequest instance) {
        if (randomBoolean()) {
            List<String> indices = new ArrayList<>();
            int count = randomInt(3) + 1;

            for (int i = 0; i < count; ++i) {
                indices.add(randomAlphaOfLength(randomInt(3) + 2));
            }

            instance.indices(indices);
        }

        instance.waitForCompletion(randomBoolean());

        if (randomBoolean()) {
            instance.masterNodeTimeout(randomTimeValue());
        }

        return instance;
    }

    @Override
    protected RestoreRemoteStoreRequest createTestInstance() {
        return randomState(new RestoreRemoteStoreRequest());
    }

    @Override
    protected Writeable.Reader<RestoreRemoteStoreRequest> instanceReader() {
        return RestoreRemoteStoreRequest::new;
    }

    @Override
    protected RestoreRemoteStoreRequest mutateInstance(RestoreRemoteStoreRequest instance) throws IOException {
        RestoreRemoteStoreRequest copy = copyInstance(instance);
        // ensure that at least one property is different
        List<String> indices = new ArrayList<>(List.of(instance.indices()));
        indices.add("copied");
        copy.indices(indices);
        return randomState(copy);
    }

    public void testSource() throws IOException {
        RestoreRemoteStoreRequest original = createTestInstance();
        XContentBuilder builder = original.toXContent(XContentFactory.jsonBuilder(), new ToXContent.MapParams(Collections.emptyMap()));
        XContentParser parser = XContentType.JSON.xContent()
            .createParser(NamedXContentRegistry.EMPTY, null, BytesReference.bytes(builder).streamInput());
        Map<String, Object> map = parser.mapOrdered();

        RestoreRemoteStoreRequest processed = new RestoreRemoteStoreRequest();
        processed.masterNodeTimeout(original.masterNodeTimeout());
        processed.waitForCompletion(original.waitForCompletion());
        processed.source(map);

        assertEquals(original, processed);
    }
}
