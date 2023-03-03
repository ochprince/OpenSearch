/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.metadata;

import com.colasoft.opensearch.cluster.ClusterModule;
import com.colasoft.opensearch.cluster.Diff;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttribute;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttributeMetadata;
import com.colasoft.opensearch.cluster.decommission.DecommissionStatus;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.xcontent.XContentParser;
import com.colasoft.opensearch.test.AbstractDiffableSerializationTestCase;

import java.io.IOException;

public class DecommissionAttributeMetadataSerializationTests extends AbstractDiffableSerializationTestCase<Metadata.Custom> {

    @Override
    protected Writeable.Reader<Metadata.Custom> instanceReader() {
        return DecommissionAttributeMetadata::new;
    }

    @Override
    protected Metadata.Custom createTestInstance() {
        String attributeName = randomAlphaOfLength(6);
        String attributeValue = randomAlphaOfLength(6);
        DecommissionAttribute decommissionAttribute = new DecommissionAttribute(attributeName, attributeValue);
        DecommissionStatus decommissionStatus = randomFrom(DecommissionStatus.values());
        return new DecommissionAttributeMetadata(decommissionAttribute, decommissionStatus, randomAlphaOfLength(10));
    }

    @Override
    protected Metadata.Custom mutateInstance(Metadata.Custom instance) {
        return randomValueOtherThan(instance, this::createTestInstance);
    }

    @Override
    protected Metadata.Custom makeTestChanges(Metadata.Custom testInstance) {
        DecommissionAttributeMetadata decommissionAttributeMetadata = (DecommissionAttributeMetadata) testInstance;
        DecommissionAttribute decommissionAttribute = decommissionAttributeMetadata.decommissionAttribute();
        String attributeName = decommissionAttribute.attributeName();
        String attributeValue = decommissionAttribute.attributeValue();
        DecommissionStatus decommissionStatus = decommissionAttributeMetadata.status();
        if (randomBoolean()) {
            decommissionStatus = randomFrom(DecommissionStatus.values());
        }
        if (randomBoolean()) {
            attributeName = randomAlphaOfLength(6);
        }
        if (randomBoolean()) {
            attributeValue = randomAlphaOfLength(6);
        }
        return new DecommissionAttributeMetadata(
            new DecommissionAttribute(attributeName, attributeValue),
            decommissionStatus,
            randomAlphaOfLength(10)
        );
    }

    @Override
    protected Writeable.Reader<Diff<Metadata.Custom>> diffReader() {
        return DecommissionAttributeMetadata::readDiffFrom;
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(ClusterModule.getNamedWriteables());
    }

    @Override
    protected Metadata.Custom doParseInstance(XContentParser parser) throws IOException {
        assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
        DecommissionAttributeMetadata decommissionAttributeMetadata = DecommissionAttributeMetadata.fromXContent(parser);
        assertEquals(XContentParser.Token.END_OBJECT, parser.currentToken());
        return new DecommissionAttributeMetadata(
            decommissionAttributeMetadata.decommissionAttribute(),
            decommissionAttributeMetadata.status(),
            decommissionAttributeMetadata.requestID()
        );
    }
}
