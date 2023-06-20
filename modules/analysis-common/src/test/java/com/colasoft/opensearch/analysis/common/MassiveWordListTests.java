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

package com.colasoft.opensearch.analysis.common;

import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchSingleNodeTestCase;

import java.util.Collection;
import java.util.Collections;

public class MassiveWordListTests extends OpenSearchSingleNodeTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Collections.singleton(CommonAnalysisPlugin.class);
    }

    public void testCreateIndexWithMassiveWordList() {
        String[] wordList = new String[100000];
        for (int i = 0; i < wordList.length; i++) {
            wordList[i] = "hello world";
        }
        client().admin()
            .indices()
            .prepareCreate("test")
            .setSettings(
                Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("analysis.analyzer.test_analyzer.type", "custom")
                    .put("analysis.analyzer.test_analyzer.tokenizer", "standard")
                    .putList("analysis.analyzer.test_analyzer.filter", "dictionary_decompounder", "lowercase")
                    .put("analysis.filter.dictionary_decompounder.type", "dictionary_decompounder")
                    .putList("analysis.filter.dictionary_decompounder.word_list", wordList)
            )
            .get();
    }
}
