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

package com.colasoft.opensearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.plugin.analysis.ukrainian.AnalysisUkrainianPlugin;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;

public class SimpleUkrainianAnalyzerTests extends OpenSearchTestCase {

    public void testBasicUsage() throws Exception {
        testAnalyzer("чергу", "черга");
        testAnalyzer("рухається", "рухатися");
        testAnalyzer("колу", "кола", "коло", "кіл");
        testAnalyzer("Ця п'єса у свою чергу рухається по колу.", "п'єса", "черга", "рухатися", "кола", "коло", "кіл");
    }

    private static void testAnalyzer(String source, String... expected_terms) throws IOException {
        TestAnalysis analysis = createTestAnalysis(new Index("test", "_na_"), Settings.EMPTY, new AnalysisUkrainianPlugin());
        Analyzer analyzer = analysis.indexAnalyzers.get("ukrainian").analyzer();
        TokenStream ts = analyzer.tokenStream("test", source);
        CharTermAttribute term1 = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        for (String expected : expected_terms) {
            assertThat(ts.incrementToken(), equalTo(true));
            assertThat(term1.toString(), equalTo(expected));
        }
        assertThat(ts.incrementToken(), equalTo(false));
    }

}
