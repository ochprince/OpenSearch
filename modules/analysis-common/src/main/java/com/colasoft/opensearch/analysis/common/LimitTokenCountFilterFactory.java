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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountFilter;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.analysis.AbstractTokenFilterFactory;

public class LimitTokenCountFilterFactory extends AbstractTokenFilterFactory {

    static final int DEFAULT_MAX_TOKEN_COUNT = 1;
    static final boolean DEFAULT_CONSUME_ALL_TOKENS = false;

    private final int maxTokenCount;
    private final boolean consumeAllTokens;

    LimitTokenCountFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name, settings);
        this.maxTokenCount = settings.getAsInt("max_token_count", DEFAULT_MAX_TOKEN_COUNT);
        this.consumeAllTokens = settings.getAsBoolean("consume_all_tokens", DEFAULT_CONSUME_ALL_TOKENS);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new LimitTokenCountFilter(tokenStream, maxTokenCount, consumeAllTokens);
    }
}
