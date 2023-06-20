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

package com.colasoft.opensearch.index.query.plugin;

import com.colasoft.opensearch.index.query.BoolQueryBuilder;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collection;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertHitCount;

public class CustomQueryParserIT extends OpenSearchIntegTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(DummyQueryParserPlugin.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createIndex("test");
        ensureGreen();
        client().prepareIndex("index").setId("1").setSource("field", "value").get();
        refresh();
    }

    @Override
    protected int numberOfShards() {
        return cluster().numDataNodes();
    }

    public void testCustomDummyQuery() {
        assertHitCount(client().prepareSearch("index").setQuery(new DummyQueryBuilder()).get(), 1L);
    }

    public void testCustomDummyQueryWithinBooleanQuery() {
        assertHitCount(client().prepareSearch("index").setQuery(new BoolQueryBuilder().must(new DummyQueryBuilder())).get(), 1L);
    }
}
