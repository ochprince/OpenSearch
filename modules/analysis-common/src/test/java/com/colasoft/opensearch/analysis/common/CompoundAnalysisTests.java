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

package com.colasoft.opensearch.analysis.common;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.env.TestEnvironment;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.analysis.IndexAnalyzers;
import com.colasoft.opensearch.index.analysis.MyFilterTokenFilterFactory;
import com.colasoft.opensearch.index.analysis.TokenFilterFactory;
import com.colasoft.opensearch.indices.analysis.AnalysisModule;
import com.colasoft.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import com.colasoft.opensearch.plugins.AnalysisPlugin;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.IndexSettingsModule;
import org.hamcrest.MatcherAssert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;

public class CompoundAnalysisTests extends OpenSearchTestCase {
    public void testDefaultsCompoundAnalysis() throws Exception {
        Settings settings = getJsonSettings();
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings("test", settings);
        AnalysisModule analysisModule = createAnalysisModule(settings);
        TokenFilterFactory filterFactory = analysisModule.getAnalysisRegistry().buildTokenFilterFactories(idxSettings).get("dict_dec");
        MatcherAssert.assertThat(filterFactory, instanceOf(DictionaryCompoundWordTokenFilterFactory.class));
    }

    public void testDictionaryDecompounder() throws Exception {
        Settings[] settingsArr = new Settings[] { getJsonSettings(), getYamlSettings() };
        for (Settings settings : settingsArr) {
            List<String> terms = analyze(settings, "decompoundingAnalyzer", "donaudampfschiff spargelcremesuppe");
            MatcherAssert.assertThat(terms.size(), equalTo(8));
            MatcherAssert.assertThat(
                terms,
                hasItems("donau", "dampf", "schiff", "donaudampfschiff", "spargel", "creme", "suppe", "spargelcremesuppe")
            );
        }
    }

    private List<String> analyze(Settings settings, String analyzerName, String text) throws IOException {
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings("test", settings);
        AnalysisModule analysisModule = createAnalysisModule(settings);
        IndexAnalyzers indexAnalyzers = analysisModule.getAnalysisRegistry().build(idxSettings);
        Analyzer analyzer = indexAnalyzers.get(analyzerName).analyzer();

        TokenStream stream = analyzer.tokenStream("", text);
        stream.reset();
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        List<String> terms = new ArrayList<>();
        while (stream.incrementToken()) {
            String tokText = termAtt.toString();
            terms.add(tokText);
        }
        return terms;
    }

    private AnalysisModule createAnalysisModule(Settings settings) throws IOException {
        CommonAnalysisPlugin commonAnalysisPlugin = new CommonAnalysisPlugin();
        return new AnalysisModule(TestEnvironment.newEnvironment(settings), Arrays.asList(commonAnalysisPlugin, new AnalysisPlugin() {
            @Override
            public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
                return singletonMap("myfilter", MyFilterTokenFilterFactory::new);
            }
        }));
    }

    private Settings getJsonSettings() throws IOException {
        String json = "/com/colasoft/opensearch/analysis/common/test1.json";
        return Settings.builder()
            .loadFromStream(json, getClass().getResourceAsStream(json), false)
            .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
            .build();
    }

    private Settings getYamlSettings() throws IOException {
        String yaml = "/com/colasoft/opensearch/analysis/common/test1.yml";
        return Settings.builder()
            .loadFromStream(yaml, getClass().getResourceAsStream(yaml), false)
            .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
            .build();
    }
}
