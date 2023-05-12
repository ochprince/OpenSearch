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

package com.colasoft.opensearch.index.store;

import com.colasoft.opensearch.ExceptionsHelper;
import com.colasoft.opensearch.action.admin.cluster.allocation.ClusterAllocationExplainResponse;
import com.colasoft.opensearch.action.index.IndexRequestBuilder;
import com.colasoft.opensearch.action.search.SearchPhaseExecutionException;
import com.colasoft.opensearch.cluster.routing.UnassignedInfo;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.ByteSizeUnit;
import com.colasoft.opensearch.common.unit.ByteSizeValue;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.MockEngineFactoryPlugin;
import com.colasoft.opensearch.index.translog.TestTranslog;
import com.colasoft.opensearch.index.translog.TranslogCorruptedException;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;
import com.colasoft.opensearch.test.InternalTestCluster;
import com.colasoft.opensearch.test.engine.MockEngineSupport;
import com.colasoft.opensearch.test.transport.MockTransportService;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static com.colasoft.opensearch.index.query.QueryBuilders.matchAllQuery;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test for corrupted translog files
 */
@OpenSearchIntegTestCase.ClusterScope(scope = OpenSearchIntegTestCase.Scope.SUITE, numDataNodes = 0)
public class CorruptedTranslogIT extends OpenSearchIntegTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(MockTransportService.TestPlugin.class, MockEngineFactoryPlugin.class);
    }

    public void testCorruptTranslogFiles() throws Exception {
        internalCluster().startNode(Settings.EMPTY);

        assertAcked(
            prepareCreate("test").setSettings(
                Settings.builder()
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 0)
                    .put("index.refresh_interval", "-1")
                    .put(MockEngineSupport.DISABLE_FLUSH_ON_CLOSE.getKey(), true) // never flush - always recover from translog
                    .put(IndexSettings.INDEX_TRANSLOG_FLUSH_THRESHOLD_SIZE_SETTING.getKey(), new ByteSizeValue(1, ByteSizeUnit.PB))
            )
        );

        // Index some documents
        IndexRequestBuilder[] builders = new IndexRequestBuilder[scaledRandomIntBetween(100, 1000)];
        for (int i = 0; i < builders.length; i++) {
            builders[i] = client().prepareIndex("test").setSource("foo", "bar");
        }

        indexRandom(false, false, false, Arrays.asList(builders));

        final Path translogPath = internalCluster().getInstance(IndicesService.class)
            .indexService(resolveIndex("test"))
            .getShard(0)
            .shardPath()
            .resolveTranslog();

        internalCluster().fullRestart(new InternalTestCluster.RestartCallback() {
            @Override
            public void onAllNodesStopped() throws Exception {
                TestTranslog.corruptRandomTranslogFile(logger, random(), translogPath);
            }
        });

        assertBusy(() -> {
            final ClusterAllocationExplainResponse allocationExplainResponse = client().admin()
                .cluster()
                .prepareAllocationExplain()
                .setIndex("test")
                .setShard(0)
                .setPrimary(true)
                .get();
            final UnassignedInfo unassignedInfo = allocationExplainResponse.getExplanation().getUnassignedInfo();
            assertThat(unassignedInfo, not(nullValue()));
            final Throwable cause = ExceptionsHelper.unwrap(unassignedInfo.getFailure(), TranslogCorruptedException.class);
            assertThat(cause, not(nullValue()));
            assertThat(cause.getMessage(), containsString(translogPath.toString()));
        });

        assertThat(
            expectThrows(SearchPhaseExecutionException.class, () -> client().prepareSearch("test").setQuery(matchAllQuery()).get())
                .getMessage(),
            containsString("all shards failed")
        );

    }

}
