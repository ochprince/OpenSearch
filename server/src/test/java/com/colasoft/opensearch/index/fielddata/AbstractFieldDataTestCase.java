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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.index.fielddata;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.ByteBuffersDirectory;
import com.colasoft.opensearch.common.lucene.index.OpenSearchDirectoryReader;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.cache.bitset.BitsetFilterCache;
import com.colasoft.opensearch.index.fielddata.IndexFieldData.XFieldComparatorSource.Nested;
import com.colasoft.opensearch.index.mapper.BinaryFieldMapper;
import com.colasoft.opensearch.index.mapper.ContentPath;
import com.colasoft.opensearch.index.mapper.FlatObjectFieldMapper;
import com.colasoft.opensearch.index.mapper.GeoPointFieldMapper;
import com.colasoft.opensearch.index.mapper.KeywordFieldMapper;
import com.colasoft.opensearch.index.mapper.MappedFieldType;
import com.colasoft.opensearch.index.mapper.Mapper.BuilderContext;
import com.colasoft.opensearch.index.mapper.MapperService;
import com.colasoft.opensearch.index.mapper.NumberFieldMapper;
import com.colasoft.opensearch.index.mapper.TextFieldMapper;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.fielddata.cache.IndicesFieldDataCache;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchSingleNodeTestCase;
import com.colasoft.opensearch.test.InternalSettingsPlugin;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

public abstract class AbstractFieldDataTestCase extends OpenSearchSingleNodeTestCase {

    protected IndexService indexService;
    protected MapperService mapperService;
    protected IndexWriter writer;
    protected List<LeafReaderContext> readerContexts = null;
    protected DirectoryReader topLevelReader = null;
    protected IndicesFieldDataCache indicesFieldDataCache;
    protected QueryShardContext shardContext;

    protected abstract String getFieldDataType();

    protected boolean hasDocValues() {
        return false;
    }

    public <IFD extends IndexFieldData<?>> IFD getForField(String fieldName) {
        return getForField(getFieldDataType(), fieldName, hasDocValues());
    }

    public <IFD extends IndexFieldData<?>> IFD getForField(String type, String fieldName) {
        return getForField(type, fieldName, hasDocValues());
    }

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return pluginList(InternalSettingsPlugin.class);
    }

    public <IFD extends IndexFieldData<?>> IFD getForField(String type, String fieldName, boolean docValues) {
        final MappedFieldType fieldType;
        final BuilderContext context = new BuilderContext(indexService.getIndexSettings().getSettings(), new ContentPath(1));
        if (type.equals("string")) {
            if (docValues) {
                fieldType = new KeywordFieldMapper.Builder(fieldName).build(context).fieldType();
            } else {
                fieldType = new TextFieldMapper.Builder(fieldName, createDefaultIndexAnalyzers()).fielddata(true)
                    .build(context)
                    .fieldType();
            }
        } else if (type.equals("float")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.FLOAT, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("double")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.DOUBLE, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("long")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.LONG, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("int")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.INTEGER, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("short")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.SHORT, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("byte")) {
            fieldType = new NumberFieldMapper.Builder(fieldName, NumberFieldMapper.NumberType.BYTE, false, true).docValues(docValues)
                .build(context)
                .fieldType();
        } else if (type.equals("geo_point")) {
            fieldType = new GeoPointFieldMapper.Builder(fieldName).docValues(docValues).build(context).fieldType();
        } else if (type.equals("flat_object")) {
            fieldType = new FlatObjectFieldMapper.Builder(fieldName).docValues(docValues).build(context).fieldType();
        } else if (type.equals("binary")) {
            fieldType = new BinaryFieldMapper.Builder(fieldName, docValues).build(context).fieldType();
        } else {
            throw new UnsupportedOperationException(type);
        }
        return shardContext.getForField(fieldType);
    }

    @Before
    public void setup() throws Exception {
        indexService = createIndex("test", Settings.builder().build());
        mapperService = indexService.mapperService();
        indicesFieldDataCache = getInstanceFromNode(IndicesService.class).getIndicesFieldDataCache();
        // LogByteSizeMP to preserve doc ID order
        writer = new IndexWriter(
            new ByteBuffersDirectory(),
            new IndexWriterConfig(new StandardAnalyzer()).setMergePolicy(new LogByteSizeMergePolicy())
        );
        shardContext = indexService.newQueryShardContext(0, null, () -> 0, null);
    }

    protected final List<LeafReaderContext> refreshReader() throws Exception {
        if (readerContexts != null && topLevelReader != null) {
            topLevelReader.close();
        }
        topLevelReader = OpenSearchDirectoryReader.wrap(DirectoryReader.open(writer), new ShardId("foo", "_na_", 1));
        readerContexts = topLevelReader.leaves();
        return readerContexts;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (topLevelReader != null) {
            topLevelReader.close();
        }
        writer.close();
        shardContext = null;
    }

    protected Nested createNested(IndexSearcher searcher, Query parentFilter, Query childFilter) throws IOException {
        BitsetFilterCache s = indexService.cache().bitsetFilterCache();
        return new Nested(s.getBitSetProducer(parentFilter), childFilter, null, searcher);
    }

    public void testEmpty() throws Exception {
        Document d = new Document();
        d.add(new StringField("field", "value", Field.Store.NO));
        writer.addDocument(d);
        refreshReader();

        IndexFieldData<?> fieldData = getForField("non_existing_field");
        int max = randomInt(7);
        for (LeafReaderContext readerContext : readerContexts) {
            LeafFieldData previous = null;
            for (int i = 0; i < max; i++) {
                LeafFieldData current = fieldData.load(readerContext);
                assertThat(current.ramBytesUsed(), equalTo(0L));
                if (previous != null) {
                    assertThat(current, not(sameInstance(previous)));
                }
                previous = current;
            }
        }
    }
}
