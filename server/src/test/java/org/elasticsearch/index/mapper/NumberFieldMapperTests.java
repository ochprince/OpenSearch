/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.mapper;

import com.carrotsearch.randomizedtesting.annotations.Timeout;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexableField;
import org.elasticsearch.Version;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.mapper.NumberFieldMapper.NumberType;
import org.elasticsearch.index.mapper.NumberFieldTypeTests.OutOfRangeSpec;
import org.elasticsearch.index.termvectors.TermVectorsService;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.containsString;

public class NumberFieldMapperTests extends AbstractNumericFieldMapperTestCase {

    @Override
    protected void setTypeList() {
        TYPES = new HashSet<>(Arrays.asList("byte", "short", "integer", "long", "float", "double", "half_float"));
        WHOLE_TYPES = new HashSet<>(Arrays.asList("byte", "short", "integer", "long"));
    }

    @Override
    public void doTestDefaults(String type) throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", 123)
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(2, fields.length);
        IndexableField pointField = fields[0];
        assertEquals(1, pointField.fieldType().pointIndexDimensionCount());
        assertFalse(pointField.fieldType().stored());
        assertEquals(123, pointField.numericValue().doubleValue(), 0d);
        IndexableField dvField = fields[1];
        assertEquals(DocValuesType.SORTED_NUMERIC, dvField.fieldType().docValuesType());
        assertFalse(dvField.fieldType().stored());
    }

    @Override
    public void doTestNotIndexed(String type) throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).field("index", false).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", 123)
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(1, fields.length);
        IndexableField dvField = fields[0];
        assertEquals(DocValuesType.SORTED_NUMERIC, dvField.fieldType().docValuesType());
    }

    @Override
    public void doTestNoDocValues(String type) throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).field("doc_values", false).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", 123)
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(1, fields.length);
        IndexableField pointField = fields[0];
        assertEquals(1, pointField.fieldType().pointIndexDimensionCount());
        assertEquals(123, pointField.numericValue().doubleValue(), 0d);
    }

    @Override
    public void doTestStore(String type) throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).field("store", true).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", 123)
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(3, fields.length);
        IndexableField pointField = fields[0];
        assertEquals(1, pointField.fieldType().pointIndexDimensionCount());
        assertEquals(123, pointField.numericValue().doubleValue(), 0d);
        IndexableField dvField = fields[1];
        assertEquals(DocValuesType.SORTED_NUMERIC, dvField.fieldType().docValuesType());
        IndexableField storedField = fields[2];
        assertTrue(storedField.fieldType().stored());
        assertEquals(123, storedField.numericValue().doubleValue(), 0d);
    }

    @Override
    public void doTestCoerce(String type) throws IOException {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", "123")
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(2, fields.length);
        IndexableField pointField = fields[0];
        assertEquals(1, pointField.fieldType().pointIndexDimensionCount());
        assertEquals(123, pointField.numericValue().doubleValue(), 0d);
        IndexableField dvField = fields[1];
        assertEquals(DocValuesType.SORTED_NUMERIC, dvField.fieldType().docValuesType());

        mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).field("coerce", false).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper2 = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper2.mappingSource().toString());

        ThrowingRunnable runnable = () -> mapper2.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", "123")
                        .endObject()),
                XContentType.JSON));
        MapperParsingException e = expectThrows(MapperParsingException.class, runnable);
        assertThat(e.getCause().getMessage(), containsString("passed as String"));
    }

    @Override
    protected void doTestDecimalCoerce(String type) throws IOException {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("field").field("type", type).endObject().endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("field", "7.89")
                        .endObject()),
                XContentType.JSON));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        IndexableField pointField = fields[0];
        assertEquals(7, pointField.numericValue().doubleValue(), 0d);
    }

    public void testIgnoreMalformed() throws Exception {
        for (String type : TYPES) {
            for (Object malformedValue : new Object[] { "a", Boolean.FALSE }) {
                String mapping = Strings.toString(jsonBuilder().startObject().startObject("type").startObject("properties")
                        .startObject("field").field("type", type).endObject().endObject().endObject().endObject());

                DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));

                assertEquals(mapping, mapper.mappingSource().toString());

                ThrowingRunnable runnable = () -> mapper.parse(new SourceToParse("test", "type", "1",
                        BytesReference.bytes(jsonBuilder().startObject().field("field", malformedValue).endObject()), XContentType.JSON));
                MapperParsingException e = expectThrows(MapperParsingException.class, runnable);
                if (malformedValue instanceof String) {
                    assertThat(e.getCause().getMessage(), containsString("For input string: \"a\""));
                } else {
                    assertThat(e.getCause().getMessage(), containsString("Current token"));
                    assertThat(e.getCause().getMessage(), containsString("not numeric, can not use numeric value accessors"));
                }

                mapping = Strings.toString(jsonBuilder().startObject().startObject("type").startObject("properties").startObject("field")
                        .field("type", type).field("ignore_malformed", true).endObject().endObject().endObject().endObject());

                DocumentMapper mapper2 = parser.parse("type", new CompressedXContent(mapping));

                ParsedDocument doc = mapper2.parse(new SourceToParse("test", "type", "1",
                        BytesReference.bytes(jsonBuilder().startObject().field("field", malformedValue).endObject()), XContentType.JSON));

                IndexableField[] fields = doc.rootDoc().getFields("field");
                assertEquals(0, fields.length);
                assertArrayEquals(new String[] { "field" }, TermVectorsService.getValues(doc.rootDoc().getFields("_ignored")));
            }
        }
    }

    /**
     * Test that in case the malformed value is an xContent object we throw error regardless of `ignore_malformed`
     */
    public void testIgnoreMalformedWithObject() throws Exception {
        for (String type : TYPES) {
            Object malformedValue = (ToXContentObject) (builder, params) -> builder.startObject().field("foo", "bar").endObject();
            for (Boolean ignoreMalformed : new Boolean[] { true, false }) {
                String mapping = Strings.toString(
                        jsonBuilder().startObject().startObject("type").startObject("properties").startObject("field").field("type", type)
                                .field("ignore_malformed", ignoreMalformed).endObject().endObject().endObject().endObject());
                DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));
                assertEquals(mapping, mapper.mappingSource().toString());

                MapperParsingException e = expectThrows(MapperParsingException.class,
                        () -> mapper.parse(new SourceToParse("test", "type", "1",
                                BytesReference.bytes(jsonBuilder().startObject().field("field", malformedValue).endObject()),
                                XContentType.JSON)));
                assertThat(e.getCause().getMessage(), containsString("Current token"));
                assertThat(e.getCause().getMessage(), containsString("not numeric, can not use numeric value accessors"));
            }
        }
    }

    @Override
    protected void doTestNullValue(String type) throws IOException {
        String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject()
                .startObject("type")
                    .startObject("properties")
                        .startObject("field")
                            .field("type", type)
                        .endObject()
                    .endObject()
                .endObject().endObject());

        DocumentMapper mapper = parser.parse("type", new CompressedXContent(mapping));
        assertEquals(mapping, mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .nullField("field")
                        .endObject()),
                XContentType.JSON));
        assertArrayEquals(new IndexableField[0], doc.rootDoc().getFields("field"));

        Object missing;
        if (Arrays.asList("float", "double", "half_float").contains(type)) {
            missing = 123d;
        } else {
            missing = 123L;
        }
        mapping = Strings.toString(XContentFactory.jsonBuilder().startObject()
                .startObject("type")
                    .startObject("properties")
                        .startObject("field")
                            .field("type", type)
                            .field("null_value", missing)
                        .endObject()
                    .endObject()
                .endObject().endObject());

        mapper = parser.parse("type", new CompressedXContent(mapping));
        assertEquals(mapping, mapper.mappingSource().toString());

        doc = mapper.parse(new SourceToParse("test", "type", "1", BytesReference
                .bytes(XContentFactory.jsonBuilder()
                        .startObject()
                        .nullField("field")
                        .endObject()),
                XContentType.JSON));
        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(2, fields.length);
        IndexableField pointField = fields[0];
        assertEquals(1, pointField.fieldType().pointIndexDimensionCount());
        assertFalse(pointField.fieldType().stored());
        assertEquals(123, pointField.numericValue().doubleValue(), 0d);
        IndexableField dvField = fields[1];
        assertEquals(DocValuesType.SORTED_NUMERIC, dvField.fieldType().docValuesType());
        assertFalse(dvField.fieldType().stored());
    }

    @Override
    public void testEmptyName() throws IOException {
        // after version 5
        for (String type : TYPES) {
            String mapping = Strings.toString(XContentFactory.jsonBuilder().startObject().startObject("type")
                .startObject("properties").startObject("").field("type", type).endObject().endObject()
                .endObject().endObject());

            IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                () -> parser.parse("type", new CompressedXContent(mapping))
            );
            assertThat(e.getMessage(), containsString("name cannot be empty string"));
        }
    }

    public void testParseSourceValue() {
        Settings settings = Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT.id).build();
        Mapper.BuilderContext context = new Mapper.BuilderContext(settings, new ContentPath());

        NumberFieldMapper mapper = new NumberFieldMapper.Builder("field", NumberType.INTEGER, false, true).build(context);
        assertEquals(3, mapper.parseSourceValue(3.14, null));
        assertEquals(42, mapper.parseSourceValue("42.9", null));

        NumberFieldMapper nullValueMapper = new NumberFieldMapper.Builder("field", NumberType.FLOAT, false, true)
            .nullValue(2.71f)
            .build(context);
        assertEquals(2.71f, (float) nullValueMapper.parseSourceValue("", null), 0.00001);

        SourceLookup sourceLookup = new SourceLookup();
        sourceLookup.setSource(Collections.singletonMap("field", null));
        assertEquals(org.elasticsearch.common.collect.List.of(2.71f), nullValueMapper.lookupValues(sourceLookup, null));
    }

    @Timeout(millis = 30000)
    public void testOutOfRangeValues() throws IOException {
        final List<OutOfRangeSpec<Object>> inputs = Arrays.asList(
            OutOfRangeSpec.of(NumberType.BYTE, "128", "is out of range for a byte"),
            OutOfRangeSpec.of(NumberType.SHORT, "32768", "is out of range for a short"),
            OutOfRangeSpec.of(NumberType.INTEGER, "2147483648", "is out of range for an integer"),
            OutOfRangeSpec.of(NumberType.LONG, "9223372036854775808", "out of range for a long"),
            OutOfRangeSpec.of(NumberType.LONG, "1e999999999", "out of range for a long"),

            OutOfRangeSpec.of(NumberType.BYTE, "-129", "is out of range for a byte"),
            OutOfRangeSpec.of(NumberType.SHORT, "-32769", "is out of range for a short"),
            OutOfRangeSpec.of(NumberType.INTEGER, "-2147483649", "is out of range for an integer"),
            OutOfRangeSpec.of(NumberType.LONG, "-9223372036854775809", "out of range for a long"),
            OutOfRangeSpec.of(NumberType.LONG, "-1e999999999", "out of range for a long"),

            OutOfRangeSpec.of(NumberType.BYTE, 128, "is out of range for a byte"),
            OutOfRangeSpec.of(NumberType.SHORT, 32768, "out of range of Java short"),
            OutOfRangeSpec.of(NumberType.INTEGER, 2147483648L, " out of range of int"),
            OutOfRangeSpec.of(NumberType.LONG, new BigInteger("9223372036854775808"), "out of range of long"),

            OutOfRangeSpec.of(NumberType.BYTE, -129, "is out of range for a byte"),
            OutOfRangeSpec.of(NumberType.SHORT, -32769, "out of range of Java short"),
            OutOfRangeSpec.of(NumberType.INTEGER, -2147483649L, " out of range of int"),
            OutOfRangeSpec.of(NumberType.LONG, new BigInteger("-9223372036854775809"), "out of range of long"),

            OutOfRangeSpec.of(NumberType.HALF_FLOAT, "65520", "[half_float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.FLOAT, "3.4028235E39", "[float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.DOUBLE, "1.7976931348623157E309", "[double] supports only finite values"),

            OutOfRangeSpec.of(NumberType.HALF_FLOAT, "-65520", "[half_float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.FLOAT, "-3.4028235E39", "[float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.DOUBLE, "-1.7976931348623157E309", "[double] supports only finite values"),

            OutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.NaN, "[half_float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.FLOAT, Float.NaN, "[float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.DOUBLE, Double.NaN, "[double] supports only finite values"),

            OutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.POSITIVE_INFINITY, "[half_float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.FLOAT, Float.POSITIVE_INFINITY, "[float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.DOUBLE, Double.POSITIVE_INFINITY, "[double] supports only finite values"),

            OutOfRangeSpec.of(NumberType.HALF_FLOAT, Float.NEGATIVE_INFINITY, "[half_float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.FLOAT, Float.NEGATIVE_INFINITY, "[float] supports only finite values"),
            OutOfRangeSpec.of(NumberType.DOUBLE, Double.NEGATIVE_INFINITY, "[double] supports only finite values")
        );

        for(OutOfRangeSpec<Object> item: inputs) {
            try {
                parseRequest(item.type, createIndexRequest(item.value));
                fail("Mapper parsing exception expected for [" + item.type + "] with value [" + item.value + "]");
            } catch (MapperParsingException e) {
                assertThat("Incorrect error message for [" + item.type + "] with value [" + item.value + "]",
                    e.getCause().getMessage(), containsString(item.message));
            }
        }

        // the following two strings are in-range for a long after coercion
        parseRequest(NumberType.LONG, createIndexRequest("9223372036854775807.9"));
        parseRequest(NumberType.LONG, createIndexRequest("-9223372036854775808.9"));
    }

    public void testLongIndexingOutOfRange() throws Exception {
        String mapping = Strings.toString(XContentFactory.jsonBuilder()
            .startObject().startObject("_doc")
            .startObject("properties")
            .startObject("number")
            .field("type", "long")
            .field("ignore_malformed", true)
            .endObject().endObject()
            .endObject().endObject());
        createIndex("test57287");
        client().admin().indices().preparePutMapping("test57287")
            .setType("_doc").setSource(mapping, XContentType.JSON).get();
        String doc = "{\"number\" : 9223372036854775808}";
        IndexResponse response = client().index(new IndexRequest("test57287").source(doc, XContentType.JSON)).get();
        assertSame(response.status(), RestStatus.CREATED);
    }

    private void parseRequest(NumberType type, BytesReference content) throws IOException {
        createDocumentMapper(type).parse(new SourceToParse("test", "type", "1", content, XContentType.JSON));
    }

    private DocumentMapper createDocumentMapper(NumberType type) throws IOException {
        String mapping = Strings
            .toString(XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("type")
                        .startObject("properties")
                            .startObject("field")
                                .field("type", type.typeName())
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject());

        return parser.parse("type", new CompressedXContent(mapping));
    }

    private BytesReference createIndexRequest(Object value) throws IOException {
        if (value instanceof BigInteger) {
            return BytesReference.bytes(XContentFactory.jsonBuilder()
                .startObject()
                    .rawField("field", new ByteArrayInputStream(value.toString().getBytes(StandardCharsets.UTF_8)), XContentType.JSON)
                .endObject());
        } else {
            return BytesReference.bytes(XContentFactory.jsonBuilder().startObject().field("field", value).endObject());
        }
    }

}
