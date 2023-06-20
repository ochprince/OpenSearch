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

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.search.sort;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SortField;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.common.TriFunction;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.cache.bitset.BitsetFilterCache;
import com.colasoft.opensearch.index.fielddata.IndexFieldData;
import com.colasoft.opensearch.index.fielddata.IndexFieldDataCache;
import com.colasoft.opensearch.index.mapper.ContentPath;
import com.colasoft.opensearch.index.mapper.MappedFieldType;
import com.colasoft.opensearch.index.mapper.Mapper.BuilderContext;
import com.colasoft.opensearch.index.mapper.NumberFieldMapper;
import com.colasoft.opensearch.index.mapper.ObjectMapper;
import com.colasoft.opensearch.index.mapper.ObjectMapper.Nested;
import com.colasoft.opensearch.index.query.IdsQueryBuilder;
import com.colasoft.opensearch.index.query.MatchAllQueryBuilder;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.index.query.Rewriteable;
import com.colasoft.opensearch.index.query.TermQueryBuilder;
import com.colasoft.opensearch.script.MockScriptEngine;
import com.colasoft.opensearch.script.ScriptEngine;
import com.colasoft.opensearch.script.ScriptModule;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.DocValueFormat;
import com.colasoft.opensearch.search.SearchModule;
import com.colasoft.opensearch.search.lookup.SearchLookup;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.IndexSettingsModule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static com.colasoft.opensearch.test.EqualsHashCodeTestUtils.checkEqualsAndHashCode;

public abstract class AbstractSortTestCase<T extends SortBuilder<T>> extends OpenSearchTestCase {

    private static final int NUMBER_OF_TESTBUILDERS = 20;

    protected static NamedWriteableRegistry namedWriteableRegistry;

    private static NamedXContentRegistry xContentRegistry;
    private static ScriptService scriptService;
    protected static String MOCK_SCRIPT_NAME = "dummy";

    @BeforeClass
    public static void init() {
        Settings baseSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Map<String, Function<Map<String, Object>, Object>> scripts = Collections.singletonMap(MOCK_SCRIPT_NAME, p -> null);
        ScriptEngine engine = new MockScriptEngine(MockScriptEngine.NAME, scripts, Collections.emptyMap());
        scriptService = new ScriptService(baseSettings, Collections.singletonMap(engine.getType(), engine), ScriptModule.CORE_CONTEXTS);

        SearchModule searchModule = new SearchModule(Settings.EMPTY, emptyList());
        namedWriteableRegistry = new NamedWriteableRegistry(searchModule.getNamedWriteables());
        xContentRegistry = new NamedXContentRegistry(searchModule.getNamedXContents());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        namedWriteableRegistry = null;
        xContentRegistry = null;
        scriptService = null;
    }

    /** Returns random sort that is put under test */
    protected abstract T createTestItem();

    /** Returns mutated version of original so the returned sort is different in terms of equals/hashcode */
    protected abstract T mutate(T original) throws IOException;

    /** Parse the sort from xContent. Just delegate to the SortBuilder's static fromXContent method. */
    protected abstract T fromXContent(XContentParser parser, String fieldName) throws IOException;

    /**
     * Test that creates new sort from a random test sort and checks both for equality
     */
    public void testFromXContent() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TESTBUILDERS; runs++) {
            T testItem = createTestItem();

            XContentBuilder builder = XContentFactory.contentBuilder(randomFrom(XContentType.values()));
            if (randomBoolean()) {
                builder.prettyPrint();
            }
            testItem.toXContent(builder, ToXContent.EMPTY_PARAMS);
            XContentBuilder shuffled = shuffleXContent(builder);
            try (XContentParser itemParser = createParser(shuffled)) {
                itemParser.nextToken();

                /*
                 * filter out name of sort, or field name to sort on for element fieldSort
                 */
                itemParser.nextToken();
                String elementName = itemParser.currentName();
                itemParser.nextToken();

                T parsedItem = fromXContent(itemParser, elementName);
                assertNotSame(testItem, parsedItem);
                assertEquals(testItem, parsedItem);
                assertEquals(testItem.hashCode(), parsedItem.hashCode());
                assertWarnings(testItem);
            }
        }
    }

    protected void assertWarnings(T testItem) {
        // assert potential warnings based on the test sort configuration. Do nothing by default, subtests can overwrite
    }

    /**
     * test that build() outputs a {@link SortField} that is similar to the one
     * we would get when parsing the xContent the sort builder is rendering out
     */
    public void testBuildSortField() throws IOException {
        QueryShardContext mockShardContext = createMockShardContext();
        for (int runs = 0; runs < NUMBER_OF_TESTBUILDERS; runs++) {
            T sortBuilder = createTestItem();
            SortFieldAndFormat sortField = Rewriteable.rewrite(sortBuilder, mockShardContext).build(mockShardContext);
            sortFieldAssertions(sortBuilder, sortField.field, sortField.format);
        }
    }

    protected abstract void sortFieldAssertions(T builder, SortField sortField, DocValueFormat format) throws IOException;

    /**
     * Test serialization and deserialization of the test sort.
     */
    public void testSerialization() throws IOException {
        for (int runs = 0; runs < NUMBER_OF_TESTBUILDERS; runs++) {
            T testsort = createTestItem();
            T deserializedsort = copy(testsort);
            assertEquals(testsort, deserializedsort);
            assertEquals(testsort.hashCode(), deserializedsort.hashCode());
            assertNotSame(testsort, deserializedsort);
        }
    }

    /**
     * Test equality and hashCode properties
     */
    public void testEqualsAndHashcode() {
        for (int runs = 0; runs < NUMBER_OF_TESTBUILDERS; runs++) {
            checkEqualsAndHashCode(createTestItem(), this::copy, this::mutate);
        }
    }

    protected final QueryShardContext createMockShardContext() {
        return createMockShardContext(null);
    }

    protected final QueryShardContext createMockShardContext(IndexSearcher searcher) {
        Index index = new Index(randomAlphaOfLengthBetween(1, 10), "_na_");
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings(
            index,
            Settings.builder().put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT).build()
        );
        BitsetFilterCache bitsetFilterCache = new BitsetFilterCache(idxSettings, Mockito.mock(BitsetFilterCache.Listener.class));
        TriFunction<MappedFieldType, String, Supplier<SearchLookup>, IndexFieldData<?>> indexFieldDataLookup = (
            fieldType,
            fieldIndexName,
            searchLookup) -> {
            IndexFieldData.Builder builder = fieldType.fielddataBuilder(fieldIndexName, searchLookup);
            return builder.build(new IndexFieldDataCache.None(), null);
        };
        return new QueryShardContext(
            0,
            idxSettings,
            BigArrays.NON_RECYCLING_INSTANCE,
            bitsetFilterCache,
            indexFieldDataLookup,
            null,
            null,
            scriptService,
            xContentRegistry(),
            namedWriteableRegistry,
            null,
            searcher,
            () -> randomNonNegativeLong(),
            null,
            null,
            () -> true,
            null
        ) {

            @Override
            public MappedFieldType fieldMapper(String name) {
                return provideMappedFieldType(name);
            }

            @Override
            public ObjectMapper getObjectMapper(String name) {
                BuilderContext context = new BuilderContext(this.getIndexSettings().getSettings(), new ContentPath());
                return new ObjectMapper.Builder<>(name).nested(Nested.newNested()).build(context);
            }
        };
    }

    /**
     * Return a field type. We use {@link NumberFieldMapper.NumberFieldType} by default since it is compatible with all sort modes
     * Tests that require other field types can override this.
     */
    protected MappedFieldType provideMappedFieldType(String name) {
        NumberFieldMapper.NumberFieldType doubleFieldType = new NumberFieldMapper.NumberFieldType(
            name,
            NumberFieldMapper.NumberType.DOUBLE
        );
        return doubleFieldType;
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return xContentRegistry;
    }

    protected static QueryBuilder randomNestedFilter() {
        int id = randomIntBetween(0, 2);
        switch (id) {
            case 0:
                return (new MatchAllQueryBuilder()).boost(randomFloat());
            case 1:
                return (new IdsQueryBuilder()).boost(randomFloat());
            case 2:
                return (new TermQueryBuilder(randomAlphaOfLengthBetween(1, 10), randomDouble()).boost(randomFloat()));
            default:
                throw new IllegalStateException("Only three query builders supported for testing sort");
        }
    }

    @SuppressWarnings("unchecked")
    private T copy(T original) throws IOException {
        /* The cast below is required to make Java 9 happy. Java 8 infers the T in copyWriterable to be the same as AbstractSortTestCase's
         * T but Java 9 infers it to be SortBuilder. */
        return (T) copyWriteable(
            original,
            namedWriteableRegistry,
            namedWriteableRegistry.getReader(SortBuilder.class, original.getWriteableName())
        );
    }
}
