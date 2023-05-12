/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package com.colasoft.opensearch.index.mapper;

import org.apache.lucene.index.LeafReaderContext;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.compress.CompressedXContent;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.index.fielddata.AbstractFieldDataTestCase;
import com.colasoft.opensearch.index.fielddata.IndexFieldData;

import java.util.List;

public class FlatObjectFieldDataTests extends AbstractFieldDataTestCase {
    private String FIELD_TYPE = "flat_object";

    @Override
    protected boolean hasDocValues() {
        return true;
    }

    public void testDocValue() throws Exception {
        String mapping = Strings.toString(
            XContentFactory.jsonBuilder()
                .startObject()
                .startObject("test")
                .startObject("properties")
                .startObject("field")
                .field("type", FIELD_TYPE)
                .endObject()
                .endObject()
                .endObject()
                .endObject()
        );
        final DocumentMapper mapper = mapperService.documentMapperParser().parse("test", new CompressedXContent(mapping));

        XContentBuilder json = XContentFactory.jsonBuilder().startObject().startObject("field").field("foo", "bar").endObject().endObject();
        ParsedDocument d = mapper.parse(new SourceToParse("test", "1", BytesReference.bytes(json), XContentType.JSON));
        writer.addDocument(d.rootDoc());
        writer.commit();

        IndexFieldData<?> fieldData = getForField("field");
        List<LeafReaderContext> readers = refreshReader();
        assertEquals(1, readers.size());

        IndexFieldData<?> valueFieldData = getForField("field._value");
        List<LeafReaderContext> valueReaders = refreshReader();
        assertEquals(1, valueReaders.size());
    }

    @Override
    protected String getFieldDataType() {
        return FIELD_TYPE;
    }
}
