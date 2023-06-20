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

package com.colasoft.opensearch.cluster.metadata;

import com.colasoft.opensearch.cluster.decommission.DecommissionAttribute;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttributeMetadata;
import com.colasoft.opensearch.cluster.decommission.DecommissionStatus;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.test.AbstractNamedWriteableTestCase;

import java.io.IOException;
import java.util.Collections;

public class DecommissionAttributeMetadataTests extends AbstractNamedWriteableTestCase<DecommissionAttributeMetadata> {
    @Override
    protected DecommissionAttributeMetadata createTestInstance() {
        String attributeName = randomAlphaOfLength(6);
        String attributeValue = randomAlphaOfLength(6);
        DecommissionAttribute decommissionAttribute = new DecommissionAttribute(attributeName, attributeValue);
        DecommissionStatus decommissionStatus = randomFrom(DecommissionStatus.values());
        return new DecommissionAttributeMetadata(decommissionAttribute, decommissionStatus, randomAlphaOfLength(10));
    }

    @Override
    protected DecommissionAttributeMetadata mutateInstance(DecommissionAttributeMetadata instance) throws IOException {
        return randomValueOtherThan(instance, this::createTestInstance);
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(
            Collections.singletonList(
                new NamedWriteableRegistry.Entry(
                    DecommissionAttributeMetadata.class,
                    DecommissionAttributeMetadata.TYPE,
                    DecommissionAttributeMetadata::new
                )
            )
        );
    }

    @Override
    protected Class<DecommissionAttributeMetadata> categoryClass() {
        return DecommissionAttributeMetadata.class;
    }
}
