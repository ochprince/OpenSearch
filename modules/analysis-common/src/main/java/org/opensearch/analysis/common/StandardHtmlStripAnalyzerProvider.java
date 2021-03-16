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

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.CharArraySet;
import org.opensearch.Version;
import org.opensearch.common.logging.DeprecationLogger;
import org.elasticsearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;
import org.elasticsearch.index.analysis.Analysis;

public class StandardHtmlStripAnalyzerProvider extends AbstractIndexAnalyzerProvider<StandardHtmlStripAnalyzer> {

    private static final DeprecationLogger DEPRECATION_LOGGER =
            DeprecationLogger.getLogger(StandardHtmlStripAnalyzerProvider.class);

    private final StandardHtmlStripAnalyzer analyzer;

    /**
     * @deprecated in 6.5, can not create in 7.0, and we remove this in 8.0
     */
    @Deprecated
    StandardHtmlStripAnalyzerProvider(IndexSettings indexSettings, Environment env,  String name, Settings settings) {
        super(indexSettings, name, settings);
        final CharArraySet defaultStopwords = CharArraySet.EMPTY_SET;
        CharArraySet stopWords = Analysis.parseStopWords(env, settings, defaultStopwords);
        analyzer = new StandardHtmlStripAnalyzer(stopWords);
        analyzer.setVersion(version);
        if (indexSettings.getIndexVersionCreated().onOrAfter(Version.V_7_0_0)) {
            throw new IllegalArgumentException("[standard_html_strip] analyzer is not supported for new indices, " +
                "use a custom analyzer using [standard] tokenizer and [html_strip] char_filter, plus [lowercase] filter");
        } else {
            DEPRECATION_LOGGER.deprecate("standard_html_strip_deprecation",
                "Deprecated analyzer [standard_html_strip] used, " +
                    "replace it with a custom analyzer using [standard] tokenizer and [html_strip] char_filter, plus [lowercase] filter");
        }
    }

    @Override
    public StandardHtmlStripAnalyzer get() {
        return this.analyzer;
    }
}
