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

package com.colasoft.opensearch.search;

import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.indices.breaker.CircuitBreakerService;
import com.colasoft.opensearch.node.MockNode;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.fetch.FetchPhase;
import com.colasoft.opensearch.search.internal.ReaderContext;
import com.colasoft.opensearch.search.query.QueryPhase;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MockSearchService extends SearchService {
    /**
     * Marker plugin used by {@link MockNode} to enable {@link MockSearchService}.
     */
    public static class TestPlugin extends Plugin {}

    private static final Map<ReaderContext, Throwable> ACTIVE_SEARCH_CONTEXTS = new ConcurrentHashMap<>();

    private Consumer<ReaderContext> onPutContext = context -> {};

    /** Throw an {@link AssertionError} if there are still in-flight contexts. */
    public static void assertNoInFlightContext() {
        final Map<ReaderContext, Throwable> copy = new HashMap<>(ACTIVE_SEARCH_CONTEXTS);
        if (copy.isEmpty() == false) {
            throw new AssertionError(
                "There are still ["
                    + copy.size()
                    + "] in-flight contexts. The first one's creation site is listed as the cause of this exception.",
                copy.values().iterator().next()
            );
        }
    }

    /**
     * Add an active search context to the list of tracked contexts. Package private for testing.
     */
    static void addActiveContext(ReaderContext context) {
        ACTIVE_SEARCH_CONTEXTS.put(context, new RuntimeException(context.toString()));
    }

    /**
     * Clear an active search context from the list of tracked contexts. Package private for testing.
     */
    static void removeActiveContext(ReaderContext context) {
        ACTIVE_SEARCH_CONTEXTS.remove(context);
    }

    public MockSearchService(
        ClusterService clusterService,
        IndicesService indicesService,
        ThreadPool threadPool,
        ScriptService scriptService,
        BigArrays bigArrays,
        QueryPhase queryPhase,
        FetchPhase fetchPhase,
        CircuitBreakerService circuitBreakerService
    ) {
        super(
            clusterService,
            indicesService,
            threadPool,
            scriptService,
            bigArrays,
            queryPhase,
            fetchPhase,
            null,
            circuitBreakerService,
            null
        );
    }

    @Override
    protected void putReaderContext(ReaderContext context) {
        onPutContext.accept(context);
        addActiveContext(context);
        super.putReaderContext(context);
    }

    @Override
    protected ReaderContext removeReaderContext(long id) {
        final ReaderContext removed = super.removeReaderContext(id);
        if (removed != null) {
            removeActiveContext(removed);
        }
        return removed;
    }

    public void setOnPutContext(Consumer<ReaderContext> onPutContext) {
        this.onPutContext = onPutContext;
    }
}
