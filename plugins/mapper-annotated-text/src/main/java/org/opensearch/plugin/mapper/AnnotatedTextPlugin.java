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

package org.opensearch.plugin.mapper;

import org.opensearch.index.mapper.Mapper;
import org.opensearch.index.mapper.annotatedtext.AnnotatedTextFieldMapper;
import org.opensearch.plugins.MapperPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.search.fetch.subphase.highlight.AnnotatedTextHighlighter;
import org.opensearch.search.fetch.subphase.highlight.Highlighter;

import java.util.Collections;
import java.util.Map;

public class AnnotatedTextPlugin extends Plugin implements MapperPlugin, SearchPlugin {

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        return Collections.singletonMap(AnnotatedTextFieldMapper.CONTENT_TYPE, AnnotatedTextFieldMapper.PARSER);
    }

    @Override
    public Map<String, Highlighter> getHighlighters() {
        return Collections.singletonMap(AnnotatedTextHighlighter.NAME, new AnnotatedTextHighlighter());
    }
}
