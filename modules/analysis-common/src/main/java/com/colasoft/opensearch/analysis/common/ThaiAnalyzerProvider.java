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

import org.apache.lucene.analysis.th.ThaiAnalyzer;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.analysis.AbstractIndexAnalyzerProvider;
import com.colasoft.opensearch.index.analysis.Analysis;

public class ThaiAnalyzerProvider extends AbstractIndexAnalyzerProvider<ThaiAnalyzer> {

    private final ThaiAnalyzer analyzer;

    ThaiAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        analyzer = new ThaiAnalyzer(Analysis.parseStopWords(env, settings, ThaiAnalyzer.getDefaultStopSet()));
    }

    @Override
    public ThaiAnalyzer get() {
        return this.analyzer;
    }
}
