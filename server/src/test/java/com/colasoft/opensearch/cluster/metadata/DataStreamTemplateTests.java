/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.metadata;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.metadata.ComposableIndexTemplate.DataStreamTemplate;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.test.AbstractSerializingTestCase;
import com.colasoft.opensearch.test.VersionUtils;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;

public class DataStreamTemplateTests extends AbstractSerializingTestCase<DataStreamTemplate> {

    @Override
    protected DataStreamTemplate doParseInstance(XContentParser parser) throws IOException {
        return DataStreamTemplate.fromXContent(parser);
    }

    @Override
    protected Writeable.Reader<DataStreamTemplate> instanceReader() {
        return DataStreamTemplate::new;
    }

    @Override
    protected DataStreamTemplate createTestInstance() {
        return new DataStreamTemplate(new DataStream.TimestampField("timestamp_" + randomAlphaOfLength(5)));
    }

    public void testBackwardCompatibleSerialization() throws Exception {
        Version version = VersionUtils.getPreviousVersion(Version.V_1_0_0);
        BytesStreamOutput out = new BytesStreamOutput();
        out.setVersion(version);

        DataStreamTemplate outTemplate = new DataStreamTemplate();
        outTemplate.writeTo(out);
        assertThat(out.size(), equalTo(0));

        StreamInput in = out.bytes().streamInput();
        in.setVersion(version);
        DataStreamTemplate inTemplate = new DataStreamTemplate(in);

        assertThat(inTemplate, equalTo(outTemplate));
    }

}
