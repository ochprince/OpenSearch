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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.index;

import org.apache.lucene.tests.index.AssertingDirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.index.engine.EngineFactory;
import com.colasoft.opensearch.plugins.EnginePlugin;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.engine.MockEngineFactory;
import com.colasoft.opensearch.test.engine.MockEngineSupport;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A plugin to use {@link MockEngineFactory}.
 *
 * Subclasses may override the reader wrapper used.
 */
public class MockEngineFactoryPlugin extends Plugin implements EnginePlugin {

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(MockEngineSupport.DISABLE_FLUSH_ON_CLOSE, MockEngineSupport.WRAP_READER_RATIO);
    }

    @Override
    public Optional<EngineFactory> getEngineFactory(final IndexSettings indexSettings) {
        return Optional.of(new MockEngineFactory(getReaderWrapperClass()));
    }

    protected Class<? extends FilterDirectoryReader> getReaderWrapperClass() {
        return AssertingDirectoryReader.class;
    }
}
