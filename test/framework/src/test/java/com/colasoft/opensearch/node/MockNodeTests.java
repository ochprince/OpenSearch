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

package com.colasoft.opensearch.node;

import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.common.util.MockBigArrays;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.search.MockSearchService;
import com.colasoft.opensearch.search.SearchService;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.MockHttpTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MockNodeTests extends OpenSearchTestCase {
    /**
     * Test that we add the appropriate mock services when their plugins are added. This is a very heavy test for a testing component but
     * we've broken it in the past so it is important.
     */
    public void testComponentsMockedByMarkerPlugins() throws IOException {
        Settings settings = Settings.builder() // All these are required or MockNode will fail to build.
            .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir())
            .put("transport.type", getTestTransportType())
            .build();
        List<Class<? extends Plugin>> plugins = new ArrayList<>();
        plugins.add(getTestTransportPlugin());
        plugins.add(MockHttpTransport.TestPlugin.class);
        boolean useMockBigArrays = randomBoolean();
        boolean useMockSearchService = randomBoolean();
        if (useMockBigArrays) {
            plugins.add(NodeMocksPlugin.class);
        }
        if (useMockSearchService) {
            plugins.add(MockSearchService.TestPlugin.class);
        }
        try (MockNode node = new MockNode(settings, plugins)) {
            BigArrays bigArrays = node.injector().getInstance(BigArrays.class);
            SearchService searchService = node.injector().getInstance(SearchService.class);
            if (useMockBigArrays) {
                assertSame(bigArrays.getClass(), MockBigArrays.class);
            } else {
                assertSame(bigArrays.getClass(), BigArrays.class);
            }
            if (useMockSearchService) {
                assertSame(searchService.getClass(), MockSearchService.class);
            } else {
                assertSame(searchService.getClass(), SearchService.class);
            }
        }
    }
}
