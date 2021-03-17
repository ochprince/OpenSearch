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

package org.elasticsearch.index;

import org.opensearch.Version;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.DocumentMapperParser;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.MapperService.MergeReason;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.indices.mapper.MapperRegistry;
import org.elasticsearch.test.IndexSettingsModule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static org.apache.lucene.util.LuceneTestCase.expectThrows;
import static org.elasticsearch.test.ESTestCase.createTestAnalysis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class MapperTestUtils {

    public static MapperService newMapperService(NamedXContentRegistry xContentRegistry,
                                                 Path tempDir,
                                                 Settings indexSettings,
                                                 String indexName) throws IOException {
        IndicesModule indicesModule = new IndicesModule(Collections.emptyList());
        return newMapperService(xContentRegistry, tempDir, indexSettings, indicesModule, indexName);
    }

    public static MapperService newMapperService(NamedXContentRegistry xContentRegistry, Path tempDir, Settings settings,
                                                 IndicesModule indicesModule, String indexName) throws IOException {
        Settings.Builder settingsBuilder = Settings.builder()
            .put(Environment.PATH_HOME_SETTING.getKey(), tempDir)
            .put(settings);
        if (settings.get(IndexMetadata.SETTING_VERSION_CREATED) == null) {
            settingsBuilder.put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT);
        }
        Settings finalSettings = settingsBuilder.build();
        MapperRegistry mapperRegistry = indicesModule.getMapperRegistry();
        IndexSettings indexSettings = IndexSettingsModule.newIndexSettings(indexName, finalSettings);
        IndexAnalyzers indexAnalyzers = createTestAnalysis(indexSettings, finalSettings).indexAnalyzers;
        SimilarityService similarityService = new SimilarityService(indexSettings, null, Collections.emptyMap());
        return new MapperService(indexSettings,
            indexAnalyzers,
            xContentRegistry,
            similarityService,
            mapperRegistry,
            () -> null, () -> false, null);
    }

    public static void assertConflicts(String mapping1,
                                       String mapping2,
                                       DocumentMapperParser
                                           parser, String... conflicts) throws IOException {
        DocumentMapper docMapper = parser.parse("type", new CompressedXContent(mapping1));
        if (conflicts.length == 0) {
            docMapper.merge(parser.parse("type", new CompressedXContent(mapping2)).mapping(), MergeReason.MAPPING_UPDATE);
        } else {
            Exception e = expectThrows(IllegalArgumentException.class,
                () -> docMapper.merge(parser.parse("type", new CompressedXContent(mapping2)).mapping(), MergeReason.MAPPING_UPDATE));
            for (String conflict : conflicts) {
                assertThat(e.getMessage(), containsString(conflict));
            }
        }
    }
}
