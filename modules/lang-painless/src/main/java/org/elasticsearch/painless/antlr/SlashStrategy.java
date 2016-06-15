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

package org.elasticsearch.painless.antlr;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;

/**
 * Utility to figure out if a {@code /} is division or the start of a regex literal.
 */
public class SlashStrategy {
    public static boolean slashIsRegex(TokenFactory<?> factory) {
        StashingTokenFactory<?> stashingFactory = (StashingTokenFactory<?>) factory;
        Token lastToken = stashingFactory.getLastToken();
        if (lastToken == null) {
            return true;
        }
        switch (lastToken.getType()) {
        case PainlessLexer.RBRACE:
        case PainlessLexer.RP:
        case PainlessLexer.OCTAL:
        case PainlessLexer.HEX:
        case PainlessLexer.INTEGER:
        case PainlessLexer.DECIMAL:
        case PainlessLexer.ID:
        case PainlessLexer.DOTINTEGER:
        case PainlessLexer.DOTID:
            return false;
        default:
            return true;
        }
    }
}
