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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.MockSynonymAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SynonymQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.lucene.search.MultiPhrasePrefixQuery;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.index.analysis.CustomAnalyzer;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.analysis.StandardTokenizerFactory;
import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.index.mapper.TextFieldMapper.TextFieldType;
import org.elasticsearch.index.query.MatchPhrasePrefixQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.index.search.MatchQuery;
import org.elasticsearch.index.similarity.SimilarityProvider;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

public class TextFieldMapperTests extends FieldMapperTestCase2<TextFieldMapper.Builder> {

    @Override
    protected TextFieldMapper.Builder newBuilder() {
        return new TextFieldMapper.Builder("text")
            .indexAnalyzer(new NamedAnalyzer("standard", AnalyzerScope.INDEX, new StandardAnalyzer()))
            .searchAnalyzer(new NamedAnalyzer("standard", AnalyzerScope.INDEX, new StandardAnalyzer()));
    }

    @Before
    public void addModifiers() {
        addBooleanModifier("fielddata", true, TextFieldMapper.Builder::fielddata);
        addModifier("fielddata_frequency_filter.min", true, (a, b) -> {
            a.fielddataFrequencyFilter(1, 10, 10);
            a.fielddataFrequencyFilter(2, 10, 10);
        });
        addModifier("fielddata_frequency_filter.max", true, (a, b) -> {
            a.fielddataFrequencyFilter(1, 10, 10);
            a.fielddataFrequencyFilter(1, 12, 10);
        });
        addModifier("fielddata_frequency_filter.min_segment_size", true, (a, b) -> {
            a.fielddataFrequencyFilter(1, 10, 10);
            a.fielddataFrequencyFilter(1, 10, 11);
        });
        addModifier("index_phrases", false, (a, b) -> {
            a.indexPhrases(true);
            b.indexPhrases(false);
        });
        addModifier("index_prefixes", false, (a, b) -> {
            a.indexPrefixes(2, 4);
        });
        addModifier("index_options", false, (a, b) -> {
            a.indexOptions(IndexOptions.DOCS_AND_FREQS);
            b.indexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        });
        addModifier("similarity", false, (a, b) -> {
            a.similarity(new SimilarityProvider("BM25", new BM25Similarity()));
            b.similarity(new SimilarityProvider("boolean", new BooleanSimilarity()));
        });
    }

    @Override
    protected Set<String> unsupportedProperties() {
        return org.elasticsearch.common.collect.Set.of("doc_values");
    }

    @Override
    protected IndexAnalyzers createIndexAnalyzers(IndexSettings indexSettings) {
        NamedAnalyzer dflt = new NamedAnalyzer(
            "default",
            AnalyzerScope.INDEX,
            new StandardAnalyzer(),
            TextFieldMapper.Defaults.POSITION_INCREMENT_GAP
        );
        NamedAnalyzer standard = new NamedAnalyzer("standard", AnalyzerScope.INDEX, new StandardAnalyzer());
        NamedAnalyzer keyword = new NamedAnalyzer("keyword", AnalyzerScope.INDEX, new KeywordAnalyzer());
        NamedAnalyzer whitespace = new NamedAnalyzer("whitespace", AnalyzerScope.INDEX, new WhitespaceAnalyzer());
        NamedAnalyzer stop = new NamedAnalyzer(
            "my_stop_analyzer",
            AnalyzerScope.INDEX,
            new CustomAnalyzer(
                new StandardTokenizerFactory(indexSettings, null, "standard", indexSettings.getSettings()),
                new CharFilterFactory[0],
                new TokenFilterFactory[] { new TokenFilterFactory() {
                    @Override
                    public String name() {
                        return "stop";
                    }

                    @Override
                    public TokenStream create(TokenStream tokenStream) {
                        return new StopFilter(tokenStream, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
                    }
                } }
            )
        );
        return new IndexAnalyzers(
            org.elasticsearch.common.collect.Map.of(
                "default", dflt, "standard", standard, "keyword", keyword, "whitespace", whitespace, "my_stop_analyzer", stop
            ),
            org.elasticsearch.common.collect.Map.of(),
            org.elasticsearch.common.collect.Map.of()
        );
    }

    @Override
    protected void minimalMapping(XContentBuilder b) throws IOException {
        b.field("type", "text");
    }

    public void testDefaults() throws IOException {
        DocumentMapper mapper = createDocumentMapper(fieldMapping(this::minimalMapping));
        assertEquals(Strings.toString(fieldMapping(this::minimalMapping)), mapper.mappingSource().toString());

        ParsedDocument doc = mapper.parse(source(b -> b.field("field", "1234")));
        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(1, fields.length);
        assertEquals("1234", fields[0].stringValue());
        IndexableFieldType fieldType = fields[0].fieldType();
        assertThat(fieldType.omitNorms(), equalTo(false));
        assertTrue(fieldType.tokenized());
        assertFalse(fieldType.stored());
        assertThat(fieldType.indexOptions(), equalTo(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS));
        assertThat(fieldType.storeTermVectors(), equalTo(false));
        assertThat(fieldType.storeTermVectorOffsets(), equalTo(false));
        assertThat(fieldType.storeTermVectorPositions(), equalTo(false));
        assertThat(fieldType.storeTermVectorPayloads(), equalTo(false));
        assertEquals(DocValuesType.NONE, fieldType.docValuesType());
    }

    public void testEnableStore() throws IOException {
        DocumentMapper mapper = createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("store", true)));
        ParsedDocument doc = mapper.parse(source(b -> b.field("field", "1234")));
        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(1, fields.length);
        assertTrue(fields[0].fieldType().stored());
    }

    public void testDisableIndex() throws IOException {
        DocumentMapper mapper = createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("index", false)));
        ParsedDocument doc = mapper.parse(source(b -> b.field("field", "1234")));
        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(0, fields.length);
    }

    public void testDisableNorms() throws IOException {
        DocumentMapper mapper = createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("norms", false)));
        ParsedDocument doc = mapper.parse(source(b -> b.field("field", "1234")));
        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(1, fields.length);
        assertTrue(fields[0].fieldType().omitNorms());
    }

    public void testIndexOptions() throws IOException {
        Map<String, IndexOptions> supportedOptions = new HashMap<>();
        supportedOptions.put("docs", IndexOptions.DOCS);
        supportedOptions.put("freqs", IndexOptions.DOCS_AND_FREQS);
        supportedOptions.put("positions", IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        supportedOptions.put("offsets", IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);

        XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject("_doc").startObject("properties");
        for (String option : supportedOptions.keySet()) {
            mapping.startObject(option).field("type", "text").field("index_options", option).endObject();
        }
        mapping.endObject().endObject().endObject();

        DocumentMapper mapper = createDocumentMapper(mapping);
        String serialized = Strings.toString(mapper);
        assertThat(serialized, containsString("\"offsets\":{\"type\":\"text\",\"index_options\":\"offsets\"}"));
        assertThat(serialized, containsString("\"freqs\":{\"type\":\"text\",\"index_options\":\"freqs\"}"));
        assertThat(serialized, containsString("\"docs\":{\"type\":\"text\",\"index_options\":\"docs\"}"));

        ParsedDocument doc = mapper.parse(source(b -> {
            for (String option : supportedOptions.keySet()) {
                b.field(option, "1234");
            }
        }));

        for (Map.Entry<String, IndexOptions> entry : supportedOptions.entrySet()) {
            String field = entry.getKey();
            IndexOptions options = entry.getValue();
            IndexableField[] fields = doc.rootDoc().getFields(field);
            assertEquals(1, fields.length);
            assertEquals(options, fields[0].fieldType().indexOptions());
        }
    }

    public void testDefaultPositionIncrementGap() throws IOException {
        MapperService mapperService = createMapperService(fieldMapping(this::minimalMapping));
        ParsedDocument doc = mapperService.documentMapper().parse(source(b -> b.array("field", new String[] { "a", "b" })));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(2, fields.length);
        assertEquals("a", fields[0].stringValue());
        assertEquals("b", fields[1].stringValue());

        withLuceneIndex(mapperService, iw -> iw.addDocument(doc.rootDoc()), reader -> {
            TermsEnum terms = getOnlyLeafReader(reader).terms("field").iterator();
            assertTrue(terms.seekExact(new BytesRef("b")));
            PostingsEnum postings = terms.postings(null, PostingsEnum.POSITIONS);
            assertEquals(0, postings.nextDoc());
            assertEquals(TextFieldMapper.Defaults.POSITION_INCREMENT_GAP + 1, postings.nextPosition());
        });
    }

    public void testPositionIncrementGap() throws IOException {
        final int positionIncrementGap = randomIntBetween(1, 1000);
        MapperService mapperService = createMapperService(
            fieldMapping(b -> b.field("type", "text").field("position_increment_gap", positionIncrementGap))
        );
        ParsedDocument doc = mapperService.documentMapper().parse(source(b -> b.array("field", new String[]{"a", "b"})));

        IndexableField[] fields = doc.rootDoc().getFields("field");
        assertEquals(2, fields.length);
        assertEquals("a", fields[0].stringValue());
        assertEquals("b", fields[1].stringValue());

        withLuceneIndex(mapperService, iw -> iw.addDocument(doc.rootDoc()), reader -> {
            TermsEnum terms = getOnlyLeafReader(reader).terms("field").iterator();
            assertTrue(terms.seekExact(new BytesRef("b")));
            PostingsEnum postings = terms.postings(null, PostingsEnum.POSITIONS);
            assertEquals(0, postings.nextDoc());
            assertEquals(positionIncrementGap + 1, postings.nextPosition());
        });
    }

    public void testSearchAnalyzerSerialization() throws IOException {
        XContentBuilder mapping = fieldMapping(
            b -> b.field("type", "text").field("analyzer", "standard").field("search_analyzer", "keyword")
        );
        assertEquals(Strings.toString(mapping), createDocumentMapper(mapping).mappingSource().toString());

        // special case: default index analyzer
        mapping = fieldMapping(b -> b.field("type", "text").field("analyzer", "default").field("search_analyzer", "keyword"));
        assertEquals(Strings.toString(mapping), createDocumentMapper(mapping).mappingSource().toString());

        // special case: default search analyzer
        mapping = fieldMapping(b -> b.field("type", "text").field("analyzer", "keyword").field("search_analyzer", "default"));
        assertEquals(Strings.toString(mapping), createDocumentMapper(mapping).mappingSource().toString());

        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        createDocumentMapper(fieldMapping(this::minimalMapping)).toXContent(
            builder,
            new ToXContent.MapParams(Collections.singletonMap("include_defaults", "true"))
        );
        builder.endObject();
        String mappingString = Strings.toString(builder);
        assertTrue(mappingString.contains("analyzer"));
        assertTrue(mappingString.contains("search_analyzer"));
        assertTrue(mappingString.contains("search_quote_analyzer"));
    }

    public void testSearchQuoteAnalyzerSerialization() throws IOException {
        XContentBuilder mapping = fieldMapping(
            b -> b.field("type", "text")
                .field("analyzer", "standard")
                .field("search_analyzer", "standard")
                .field("search_quote_analyzer", "keyword")
        );
        assertEquals(Strings.toString(mapping), createDocumentMapper(mapping).mappingSource().toString());

        // special case: default index/search analyzer
        mapping = fieldMapping(
            b -> b.field("type", "text")
                .field("analyzer", "default")
                .field("search_analyzer", "default")
                .field("search_quote_analyzer", "keyword")
        );
        assertEquals(Strings.toString(mapping), createDocumentMapper(mapping).mappingSource().toString());
    }

    public void testTermVectors() throws IOException {
        XContentBuilder mapping = mapping(b ->
                b.startObject("field1")
                    .field("type", "text")
                    .field("term_vector", "no")
                .endObject()
                .startObject("field2")
                    .field("type", "text")
                    .field("term_vector", "yes")
                .endObject()
                .startObject("field3")
                    .field("type", "text")
                    .field("term_vector", "with_offsets")
                .endObject()
                .startObject("field4")
                    .field("type", "text")
                    .field("term_vector", "with_positions")
                .endObject()
                .startObject("field5")
                    .field("type", "text")
                    .field("term_vector", "with_positions_offsets")
                .endObject()
                .startObject("field6")
                    .field("type", "text")
                    .field("term_vector", "with_positions_offsets_payloads")
                .endObject());

        DocumentMapper defaultMapper = createDocumentMapper(mapping);

        ParsedDocument doc = defaultMapper.parse(
            source(
                b -> b.field("field1", "1234")
                    .field("field2", "1234")
                    .field("field3", "1234")
                    .field("field4", "1234")
                    .field("field5", "1234")
                    .field("field6", "1234")
            )
        );

        assertThat(doc.rootDoc().getField("field1").fieldType().storeTermVectors(), equalTo(false));
        assertThat(doc.rootDoc().getField("field1").fieldType().storeTermVectorOffsets(), equalTo(false));
        assertThat(doc.rootDoc().getField("field1").fieldType().storeTermVectorPositions(), equalTo(false));
        assertThat(doc.rootDoc().getField("field1").fieldType().storeTermVectorPayloads(), equalTo(false));

        assertThat(doc.rootDoc().getField("field2").fieldType().storeTermVectors(), equalTo(true));
        assertThat(doc.rootDoc().getField("field2").fieldType().storeTermVectorOffsets(), equalTo(false));
        assertThat(doc.rootDoc().getField("field2").fieldType().storeTermVectorPositions(), equalTo(false));
        assertThat(doc.rootDoc().getField("field2").fieldType().storeTermVectorPayloads(), equalTo(false));

        assertThat(doc.rootDoc().getField("field3").fieldType().storeTermVectors(), equalTo(true));
        assertThat(doc.rootDoc().getField("field3").fieldType().storeTermVectorOffsets(), equalTo(true));
        assertThat(doc.rootDoc().getField("field3").fieldType().storeTermVectorPositions(), equalTo(false));
        assertThat(doc.rootDoc().getField("field3").fieldType().storeTermVectorPayloads(), equalTo(false));

        assertThat(doc.rootDoc().getField("field4").fieldType().storeTermVectors(), equalTo(true));
        assertThat(doc.rootDoc().getField("field4").fieldType().storeTermVectorOffsets(), equalTo(false));
        assertThat(doc.rootDoc().getField("field4").fieldType().storeTermVectorPositions(), equalTo(true));
        assertThat(doc.rootDoc().getField("field4").fieldType().storeTermVectorPayloads(), equalTo(false));

        assertThat(doc.rootDoc().getField("field5").fieldType().storeTermVectors(), equalTo(true));
        assertThat(doc.rootDoc().getField("field5").fieldType().storeTermVectorOffsets(), equalTo(true));
        assertThat(doc.rootDoc().getField("field5").fieldType().storeTermVectorPositions(), equalTo(true));
        assertThat(doc.rootDoc().getField("field5").fieldType().storeTermVectorPayloads(), equalTo(false));

        assertThat(doc.rootDoc().getField("field6").fieldType().storeTermVectors(), equalTo(true));
        assertThat(doc.rootDoc().getField("field6").fieldType().storeTermVectorOffsets(), equalTo(true));
        assertThat(doc.rootDoc().getField("field6").fieldType().storeTermVectorPositions(), equalTo(true));
        assertThat(doc.rootDoc().getField("field6").fieldType().storeTermVectorPayloads(), equalTo(true));
    }

    public void testEagerGlobalOrdinals() throws IOException {
        DocumentMapper mapper = createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("eager_global_ordinals", true)));

        FieldMapper fieldMapper = (FieldMapper) mapper.mappers().getMapper("field");
        assertTrue(fieldMapper.fieldType().eagerGlobalOrdinals());
    }

    public void testFielddata() throws IOException {
        MapperService disabledMapper = createMapperService(fieldMapping(this::minimalMapping));
        Exception e = expectThrows(
            IllegalArgumentException.class,
            () -> disabledMapper.fieldType("field").fielddataBuilder("test", () -> {
                throw new UnsupportedOperationException();
            })
        );
        assertThat(e.getMessage(), containsString("Text fields are not optimised for operations that require per-document field data"));

        MapperService enabledMapper = createMapperService(fieldMapping(b -> b.field("type", "text").field("fielddata", true)));
        enabledMapper.fieldType("field").fielddataBuilder("test", () -> {
            throw new UnsupportedOperationException();
        }); // no exception this time

        e = expectThrows(
            MapperParsingException.class,
            () -> createMapperService(fieldMapping(b -> b.field("type", "text").field("index", false).field("fielddata", true)))
        );
        assertThat(e.getMessage(), containsString("Cannot enable fielddata on a [text] field that is not indexed"));
    }

    public void testFrequencyFilter() throws IOException {
        MapperService mapperService = createMapperService(
            fieldMapping(
                b -> b.field("type", "text")
                    .field("fielddata", true)
                    .startObject("fielddata_frequency_filter")
                    .field("min", 2d)
                    .field("min_segment_size", 1000)
                    .endObject()
            )
        );
        TextFieldType fieldType = (TextFieldType) mapperService.fieldType("field");

        assertThat(fieldType.fielddataMinFrequency(), equalTo(2d));
        assertThat(fieldType.fielddataMaxFrequency(), equalTo((double) Integer.MAX_VALUE));
        assertThat(fieldType.fielddataMinSegmentSize(), equalTo(1000));
    }

    public void testNullConfigValuesFail() throws MapperParsingException, IOException {
        Exception e = expectThrows(
            MapperParsingException.class,
            () -> createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("analyzer", (String) null)))
        );
        assertThat(e.getMessage(), containsString("[analyzer] must not have a [null] value"));
    }

    public void testNotIndexedFieldPositionIncrement() throws IOException {
        Exception e = expectThrows(
            MapperParsingException.class,
            () -> createDocumentMapper(fieldMapping(b -> b.field("type", "text").field("index", false).field("position_increment_gap", 10)))
        );
        assertThat(e.getMessage(), containsString("Cannot set position_increment_gap on field [field] without positions enabled"));
    }

    public void testAnalyzedFieldPositionIncrementWithoutPositions() throws IOException {
        for (String indexOptions : Arrays.asList("docs", "freqs")) {
            Exception e = expectThrows(
                MapperParsingException.class,
                () -> createDocumentMapper(
                    fieldMapping(b -> b.field("type", "text").field("index_options", indexOptions).field("position_increment_gap", 10))
                )
            );
            assertThat(e.getMessage(), containsString("Cannot set position_increment_gap on field [field] without positions enabled"));
        }
    }

    public void testIndexPrefixIndexTypes() throws IOException {
        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .endObject()
                        .field("index_options", "offsets")
                )
            );
            FieldMapper prefix = (FieldMapper) mapper.mappers().getMapper("field._index_prefix");
            assertEquals(prefix.name(), "field._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS, prefix.fieldType.indexOptions());
        }

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .endObject()
                        .field("index_options", "freqs")
                )
            );
            FieldMapper prefix = (FieldMapper) mapper.mappers().getMapper("field._index_prefix");
            FieldType ft = prefix.fieldType;
            assertEquals(prefix.name(), "field._index_prefix");
            assertEquals(IndexOptions.DOCS, ft.indexOptions());
            assertFalse(ft.storeTermVectors());
        }

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .endObject()
                        .field("index_options", "positions")
                )
            );
            FieldMapper prefix = (FieldMapper) mapper.mappers().getMapper("field._index_prefix");
            FieldType ft = prefix.fieldType;
            assertEquals(prefix.name(), "field._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, ft.indexOptions());
            assertFalse(ft.storeTermVectors());
        }

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .endObject()
                        .field("term_vector", "with_positions_offsets")
                )
            );
            FieldMapper prefix = (FieldMapper) mapper.mappers().getMapper("field._index_prefix");
            FieldType ft = prefix.fieldType;
            assertEquals(prefix.name(), "field._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, ft.indexOptions());
            assertTrue(ft.storeTermVectorOffsets());
        }

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .endObject()
                        .field("term_vector", "with_positions")
                )
            );
            FieldMapper prefix = (FieldMapper) mapper.mappers().getMapper("field._index_prefix");
            FieldType ft = prefix.fieldType;
            assertEquals(prefix.name(), "field._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, ft.indexOptions());
            assertFalse(ft.storeTermVectorOffsets());
        }
    }

    public void testNestedIndexPrefixes() throws IOException {
        {
            MapperService mapperService = createMapperService(mapping(b -> b.startObject("object")
                                .field("type", "object")
                                .startObject("properties")
                                    .startObject("field")
                                        .field("type", "text")
                                        .startObject("index_prefixes").endObject()
                                    .endObject()
                                .endObject()
                            .endObject()));
            MappedFieldType textField = mapperService.fieldType("object.field");
            assertNotNull(textField);
            assertThat(textField, instanceOf(TextFieldType.class));
            MappedFieldType prefix = ((TextFieldType) textField).getPrefixFieldType();
            assertEquals(prefix.name(), "object.field._index_prefix");
            FieldMapper mapper
                = (FieldMapper) mapperService.documentMapper().mappers().getMapper("object.field._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, mapper.fieldType.indexOptions());
            assertFalse(mapper.fieldType.storeTermVectorOffsets());
        }

        {
            MapperService mapperService = createMapperService(mapping(b -> b.startObject("body")
                                    .field("type", "text")
                                    .startObject("fields")
                                        .startObject("with_prefix")
                                            .field("type", "text")
                                            .startObject("index_prefixes").endObject()
                                        .endObject()
                                    .endObject()
                                .endObject()));
            MappedFieldType textField = mapperService.fieldType("body.with_prefix");
            assertNotNull(textField);
            assertThat(textField, instanceOf(TextFieldType.class));
            MappedFieldType prefix = ((TextFieldType) textField).getPrefixFieldType();
            assertEquals(prefix.name(), "body.with_prefix._index_prefix");
            FieldMapper mapper
                = (FieldMapper) mapperService.documentMapper().mappers().getMapper("body.with_prefix._index_prefix");
            assertEquals(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS, mapper.fieldType.indexOptions());
            assertFalse(mapper.fieldType.storeTermVectorOffsets());
        }
    }

    public void testFastPhraseMapping() throws IOException {
        MapperService mapperService = createMapperService(mapping(b -> {
            b.startObject("field").field("type", "text").field("analyzer", "my_stop_analyzer").field("index_phrases", true).endObject();
            // "standard" will be replaced with MockSynonymAnalyzer
            b.startObject("synfield").field("type", "text").field("analyzer", "standard").field("index_phrases", true).endObject();
        }));
        QueryShardContext queryShardContext = createQueryShardContext(mapperService);

        Query q = new MatchPhraseQueryBuilder("field", "two words").toQuery(queryShardContext);
        assertThat(q, is(new PhraseQuery("field._index_phrase", "two words")));

        Query q2 = new MatchPhraseQueryBuilder("field", "three words here").toQuery(queryShardContext);
        assertThat(q2, is(new PhraseQuery("field._index_phrase", "three words", "words here")));

        Query q3 = new MatchPhraseQueryBuilder("field", "two words").slop(1).toQuery(queryShardContext);
        assertThat(q3, is(new PhraseQuery(1, "field", "two", "words")));

        Query q4 = new MatchPhraseQueryBuilder("field", "singleton").toQuery(queryShardContext);
        assertThat(q4, is(new TermQuery(new Term("field", "singleton"))));

        Query q5 = new MatchPhraseQueryBuilder("field", "sparkle a stopword").toQuery(queryShardContext);
        assertThat(q5,
            is(new PhraseQuery.Builder().add(new Term("field", "sparkle")).add(new Term("field", "stopword"), 2).build()));

        MatchQuery matchQuery = new MatchQuery(queryShardContext);
        matchQuery.setAnalyzer(new MockSynonymAnalyzer());
        Query q6 = matchQuery.parse(MatchQuery.Type.PHRASE, "synfield", "motor dogs");
        assertThat(q6, is(new MultiPhraseQuery.Builder()
            .add(new Term[]{
                new Term("synfield._index_phrase", "motor dogs"),
                new Term("synfield._index_phrase", "motor dog")})
            .build()));

        // https://github.com/elastic/elasticsearch/issues/43976
        CannedTokenStream cts = new CannedTokenStream(
            new Token("foo", 1, 0, 2, 2),
            new Token("bar", 0, 0, 2),
            new Token("baz", 1, 0, 2)
        );
        Analyzer synonymAnalyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                return new TokenStreamComponents(reader -> {}, cts);
            }
        };
        matchQuery.setAnalyzer(synonymAnalyzer);
        Query q7 = matchQuery.parse(MatchQuery.Type.BOOLEAN, "synfield", "foo");
        assertThat(q7, is(new BooleanQuery.Builder().add(new BooleanQuery.Builder()
            .add(new TermQuery(new Term("synfield", "foo")), BooleanClause.Occur.SHOULD)
            .add(new PhraseQuery.Builder()
                .add(new Term("synfield._index_phrase", "bar baz"))
                .build(), BooleanClause.Occur.SHOULD)
            .build(), BooleanClause.Occur.SHOULD).build()));

        ParsedDocument doc = mapperService.documentMapper()
            .parse(source(b -> b.field("field", "Some English text that is going to be very useful")));

        IndexableField[] fields = doc.rootDoc().getFields("field._index_phrase");
        assertEquals(1, fields.length);

        try (TokenStream ts = fields[0].tokenStream(queryShardContext.getMapperService().indexAnalyzer(), null)) {
            CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset();
            assertTrue(ts.incrementToken());
            assertEquals("Some English", termAtt.toString());
        }

        Exception e = expectThrows(
            MapperParsingException.class,
            () -> createMapperService(fieldMapping(b -> b.field("type", "text").field("index", "false").field("index_phrases", true)))
        );
        assertThat(e.getMessage(), containsString("Cannot set index_phrases on unindexed field [field]"));

        e = expectThrows(
            MapperParsingException.class,
            () -> createMapperService(
                fieldMapping(b -> b.field("type", "text").field("index_options", "freqs").field("index_phrases", true))
            )
        );
        assertThat(e.getMessage(), containsString("Cannot set index_phrases on field [field] if positions are not enabled"));
    }

    public void testIndexPrefixMapping() throws IOException {

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(
                    b -> b.field("type", "text")
                        .field("analyzer", "standard")
                        .startObject("index_prefixes")
                        .field("min_chars", 2)
                        .field("max_chars", 10)
                        .endObject()
                )
            );

            assertThat(mapper.mappers().getMapper("field._index_prefix").toString(), containsString("prefixChars=2:10"));

            ParsedDocument doc = mapper.parse(source(b -> b.field("field", "Some English text that is going to be very useful")));
            IndexableField[] fields = doc.rootDoc().getFields("field._index_prefix");
            assertEquals(1, fields.length);
        }

        {
            DocumentMapper mapper = createDocumentMapper(
                fieldMapping(b -> b.field("type", "text").field("analyzer", "standard").startObject("index_prefixes").endObject())
            );
            assertThat(mapper.mappers().getMapper("field._index_prefix").toString(), containsString("prefixChars=2:5"));

        }

        {
            MapperParsingException e = expectThrows(MapperParsingException.class, () -> createMapperService(fieldMapping(b -> {
                b.field("type", "text").field("analyzer", "standard");
                b.startObject("index_prefixes").field("min_chars", 1).field("max_chars", 10).endObject();
                b.startObject("fields").startObject("_index_prefix").field("type", "text").endObject().endObject();
            })));
            assertThat(e.getMessage(), containsString("Field [field._index_prefix] is defined more than once"));
        }

        {
            MapperParsingException e = expectThrows(MapperParsingException.class, () -> createMapperService(fieldMapping(b -> {
                b.field("type", "text").field("analyzer", "standard");
                b.startObject("index_prefixes").field("min_chars", 11).field("max_chars", 10).endObject();
            })));
            assertThat(e.getMessage(), containsString("min_chars [11] must be less than max_chars [10]"));
        }

        {
            MapperParsingException e = expectThrows(MapperParsingException.class, () -> createMapperService(fieldMapping(b -> {
                b.field("type", "text").field("analyzer", "standard");
                b.startObject("index_prefixes").field("min_chars", 0).field("max_chars", 10).endObject();
            })));
            assertThat(e.getMessage(), containsString("min_chars [0] must be greater than zero"));
        }

        {
            MapperParsingException e = expectThrows(MapperParsingException.class, () -> createMapperService(fieldMapping(b -> {
                b.field("type", "text").field("analyzer", "standard");
                b.startObject("index_prefixes").field("min_chars", 1).field("max_chars", 25).endObject();
            })));
            assertThat(e.getMessage(), containsString("max_chars [25] must be less than 20"));
        }

        {
            MapperParsingException e = expectThrows(
                MapperParsingException.class,
                () -> createMapperService(
                    fieldMapping(b -> b.field("type", "text").field("analyzer", "standard").nullField("index_prefixes"))
                )
            );
            assertThat(e.getMessage(), containsString("[index_prefixes] must not have a [null] value"));
        }

        {
            MapperParsingException e = expectThrows(MapperParsingException.class, () -> createMapperService(fieldMapping(b -> {
                b.field("type", "text").field("analyzer", "standard").field("index", false);
                b.startObject("index_prefixes").endObject();
            })));
            assertThat(e.getMessage(), containsString("Cannot set index_prefixes on unindexed field [field]"));
        }
    }

    public void testFastPhrasePrefixes() throws IOException {
        MapperService mapperService = createMapperService(mapping(b -> {
            b.startObject("field");
            {
                b.field("type", "text");
                b.field("analyzer", "my_stop_analyzer");
                b.startObject("index_prefixes").field("min_chars", 2).field("max_chars", 10).endObject();
            }
            b.endObject();
            b.startObject("synfield");
            {
                b.field("type", "text");
                b.field("analyzer", "standard"); // "standard" will be replaced with MockSynonymAnalyzer
                b.field("index_phrases", true);
                b.startObject("index_prefixes").field("min_chars", 2).field("max_chars", 10).endObject();
            }
            b.endObject();
        }));
        QueryShardContext queryShardContext = createQueryShardContext(mapperService);

        {
            Query q = new MatchPhrasePrefixQueryBuilder("field", "two words").toQuery(queryShardContext);
            Query expected = new SpanNearQuery.Builder("field", true)
                .addClause(new SpanTermQuery(new Term("field", "two")))
                .addClause(new FieldMaskingSpanQuery(
                    new SpanTermQuery(new Term("field._index_prefix", "words")), "field")
                )
                .build();
            assertThat(q, equalTo(expected));
        }

        {
            Query q = new MatchPhrasePrefixQueryBuilder("field", "three words here").toQuery(queryShardContext);
            Query expected = new SpanNearQuery.Builder("field", true)
                .addClause(new SpanTermQuery(new Term("field", "three")))
                .addClause(new SpanTermQuery(new Term("field", "words")))
                .addClause(new FieldMaskingSpanQuery(
                    new SpanTermQuery(new Term("field._index_prefix", "here")), "field")
                )
                .build();
            assertThat(q, equalTo(expected));
        }

        {
            Query q = new MatchPhrasePrefixQueryBuilder("field", "two words").slop(1).toQuery(queryShardContext);
            MultiPhrasePrefixQuery mpq = new MultiPhrasePrefixQuery("field");
            mpq.setSlop(1);
            mpq.add(new Term("field", "two"));
            mpq.add(new Term("field", "words"));
            assertThat(q, equalTo(mpq));
        }

        {
            Query q = new MatchPhrasePrefixQueryBuilder("field", "singleton").toQuery(queryShardContext);
            assertThat(
                q,
                is(new SynonymQuery.Builder("field._index_prefix").addTerm(new Term("field._index_prefix", "singleton")).build())
            );
        }

        {

            Query q = new MatchPhrasePrefixQueryBuilder("field", "sparkle a stopword").toQuery(queryShardContext);
            Query expected = new SpanNearQuery.Builder("field", true)
                .addClause(new SpanTermQuery(new Term("field", "sparkle")))
                .addGap(1)
                .addClause(new FieldMaskingSpanQuery(
                    new SpanTermQuery(new Term("field._index_prefix", "stopword")), "field")
                )
                .build();
            assertThat(q, equalTo(expected));
        }

        {
            MatchQuery matchQuery = new MatchQuery(queryShardContext);
            matchQuery.setAnalyzer(new MockSynonymAnalyzer());
            Query q = matchQuery.parse(MatchQuery.Type.PHRASE_PREFIX, "synfield", "motor dogs");
            Query expected = new SpanNearQuery.Builder("synfield", true)
                .addClause(new SpanTermQuery(new Term("synfield", "motor")))
                .addClause(
                    new SpanOrQuery(
                        new FieldMaskingSpanQuery(
                            new SpanTermQuery(new Term("synfield._index_prefix", "dogs")), "synfield"
                        ),
                        new FieldMaskingSpanQuery(
                            new SpanTermQuery(new Term("synfield._index_prefix", "dog")), "synfield"
                        )
                    )
                )
                .build();
            assertThat(q, equalTo(expected));
        }

        {
            MatchQuery matchQuery = new MatchQuery(queryShardContext);
            matchQuery.setPhraseSlop(1);
            matchQuery.setAnalyzer(new MockSynonymAnalyzer());
            Query q = matchQuery.parse(MatchQuery.Type.PHRASE_PREFIX, "synfield", "two dogs");
            MultiPhrasePrefixQuery mpq = new MultiPhrasePrefixQuery("synfield");
            mpq.setSlop(1);
            mpq.add(new Term("synfield", "two"));
            mpq.add(new Term[] { new Term("synfield", "dogs"), new Term("synfield", "dog") });
            assertThat(q, equalTo(mpq));
        }

        {
            Query q = new MatchPhrasePrefixQueryBuilder("field", "motor d").toQuery(queryShardContext);
            MultiPhrasePrefixQuery mpq = new MultiPhrasePrefixQuery("field");
            mpq.add(new Term("field", "motor"));
            mpq.add(new Term("field", "d"));
            assertThat(q, equalTo(mpq));
        }
    }

    public void testSimpleMerge() throws IOException {
        XContentBuilder startingMapping = fieldMapping(
            b -> b.field("type", "text").startObject("index_prefixes").endObject().field("index_phrases", true)
        );
        MapperService mapperService = createMapperService(startingMapping);
        assertThat(mapperService.documentMapper().mappers().getMapper("field"), instanceOf(TextFieldMapper.class));

        merge(mapperService, startingMapping);
        assertThat(mapperService.documentMapper().mappers().getMapper("field"), instanceOf(TextFieldMapper.class));

        XContentBuilder differentPrefix = fieldMapping(
            b -> b.field("type", "text").startObject("index_prefixes").field("min_chars", "3").endObject().field("index_phrases", true)
        );
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> merge(mapperService, differentPrefix));
        assertThat(e.getMessage(), containsString("different [index_prefixes]"));

        XContentBuilder differentPhrases = fieldMapping(
            b -> b.field("type", "text").startObject("index_prefixes").endObject().field("index_phrases", false)
        );
        e = expectThrows(IllegalArgumentException.class, () -> merge(mapperService, differentPhrases));
        assertThat(e.getMessage(), containsString("different [index_phrases]"));

        XContentBuilder newField = mapping(b -> {
            b.startObject("field").field("type", "text").startObject("index_prefixes").endObject().field("index_phrases", true).endObject();
            b.startObject("other_field").field("type", "keyword").endObject();
        });
        merge(mapperService, newField);
        assertThat(mapperService.documentMapper().mappers().getMapper("field"), instanceOf(TextFieldMapper.class));
        assertThat(mapperService.documentMapper().mappers().getMapper("other_field"), instanceOf(KeywordFieldMapper.class));
    }

    public void testFetchSourceValue() throws IOException {
        Settings settings = Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT.id).build();
        Mapper.BuilderContext context = new Mapper.BuilderContext(settings, new ContentPath());

        FieldMapper fieldMapper = newBuilder().build(context);
        TextFieldMapper mapper = (TextFieldMapper) fieldMapper;

        assertEquals(org.elasticsearch.common.collect.List.of("value"), fetchSourceValue(mapper, "value"));
        assertEquals(org.elasticsearch.common.collect.List.of("42"), fetchSourceValue(mapper, 42L));
        assertEquals(org.elasticsearch.common.collect.List.of("true"), fetchSourceValue(mapper, true));
    }
}
