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

package org.opensearch.analysis.common;

import org.opensearch.Version;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.env.TestEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.IndexAnalyzers;
import org.opensearch.index.analysis.NamedAnalyzer;
import org.opensearch.indices.analysis.AnalysisModule;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptService;
import org.opensearch.test.OpenSearchTokenStreamTestCase;
import org.opensearch.test.IndexSettingsModule;

import java.io.IOException;
import java.util.Collections;

public class PredicateTokenScriptFilterTests extends OpenSearchTokenStreamTestCase {

    public void testSimpleFilter() throws IOException {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Settings indexSettings = Settings.builder()
            .put(IndexMetadata.SETTING_VERSION_CREATED, Version.CURRENT)
            .put("index.analysis.filter.f.type", "predicate_token_filter")
            .put("index.analysis.filter.f.script.source", "my_script")
            .put("index.analysis.analyzer.myAnalyzer.type", "custom")
            .put("index.analysis.analyzer.myAnalyzer.tokenizer", "standard")
            .putList("index.analysis.analyzer.myAnalyzer.filter", "f")
            .build();
        IndexSettings idxSettings = IndexSettingsModule.newIndexSettings("index", indexSettings);

        AnalysisPredicateScript.Factory factory = () -> new AnalysisPredicateScript() {
            @Override
            public boolean execute(Token token) {
                return token.getPosition() < 2 || token.getPosition() > 4;
            }
        };

        @SuppressWarnings("unchecked")
        ScriptService scriptService = new ScriptService(indexSettings, Collections.emptyMap(), Collections.emptyMap()) {
            @Override
            public <FactoryType> FactoryType compile(Script script, ScriptContext<FactoryType> context) {
                assertEquals(context, AnalysisPredicateScript.CONTEXT);
                assertEquals(new Script("my_script"), script);
                return (FactoryType) factory;
            }
        };

        CommonAnalysisPlugin plugin = new CommonAnalysisPlugin();
        plugin.createComponents(null, null, null, null, scriptService, null, null, null, null, null, null);
        AnalysisModule module = new AnalysisModule(TestEnvironment.newEnvironment(settings), Collections.singletonList(plugin));

        IndexAnalyzers analyzers = module.getAnalysisRegistry().build(idxSettings);

        try (NamedAnalyzer analyzer = analyzers.get("myAnalyzer")) {
            assertNotNull(analyzer);
            assertAnalyzesTo(analyzer, "Oh what a wonderful thing to be", new String[] { "Oh", "what", "to", "be" });
        }

    }

}
