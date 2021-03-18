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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.IdentityEncoder;
import org.apache.lucene.analysis.payloads.IntegerEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

public class DelimitedPayloadTokenFilterFactory extends AbstractTokenFilterFactory {

    public static final char DEFAULT_DELIMITER = '|';
    public static final PayloadEncoder DEFAULT_ENCODER = new FloatEncoder();

    static final String ENCODING = "encoding";
    static final String DELIMITER = "delimiter";

    private final char delimiter;
    private final PayloadEncoder encoder;

    DelimitedPayloadTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        String delimiterConf = settings.get(DELIMITER);
        if (delimiterConf != null) {
            delimiter = delimiterConf.charAt(0);
        } else {
            delimiter = DEFAULT_DELIMITER;
        }

        if (settings.get(ENCODING) != null) {
            if (settings.get(ENCODING).equals("float")) {
                encoder = new FloatEncoder();
            } else if (settings.get(ENCODING).equals("int")) {
                encoder = new IntegerEncoder();
            } else if (settings.get(ENCODING).equals("identity")) {
                encoder = new IdentityEncoder();
            } else {
                encoder = DEFAULT_ENCODER;
            }
        } else {
            encoder = DEFAULT_ENCODER;
        }
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new DelimitedPayloadTokenFilter(tokenStream, delimiter, encoder);
    }

}
