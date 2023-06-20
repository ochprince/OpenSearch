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

package com.colasoft.opensearch.plugin.analysis.nori;

import org.apache.lucene.analysis.Analyzer;
import com.colasoft.opensearch.index.analysis.AnalyzerProvider;
import com.colasoft.opensearch.index.analysis.NoriAnalyzerProvider;
import com.colasoft.opensearch.index.analysis.NoriNumberFilterFactory;
import com.colasoft.opensearch.index.analysis.NoriPartOfSpeechStopFilterFactory;
import com.colasoft.opensearch.index.analysis.NoriReadingFormFilterFactory;
import com.colasoft.opensearch.index.analysis.NoriTokenizerFactory;
import com.colasoft.opensearch.index.analysis.TokenFilterFactory;
import com.colasoft.opensearch.index.analysis.TokenizerFactory;
import com.colasoft.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import com.colasoft.opensearch.plugins.AnalysisPlugin;
import com.colasoft.opensearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class AnalysisNoriPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisProvider<TokenFilterFactory>> extra = new HashMap<>();
        extra.put("nori_part_of_speech", NoriPartOfSpeechStopFilterFactory::new);
        extra.put("nori_readingform", NoriReadingFormFilterFactory::new);
        extra.put("nori_number", NoriNumberFilterFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisProvider<TokenizerFactory>> getTokenizers() {
        return singletonMap("nori_tokenizer", NoriTokenizerFactory::new);
    }

    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return singletonMap("nori", NoriAnalyzerProvider::new);
    }
}
