/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search;

import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.OpenSearchExecutors;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.search.query.ConcurrentQueryPhaseSearcher;
import com.colasoft.opensearch.search.query.QueryPhaseSearcher;
import com.colasoft.opensearch.threadpool.ExecutorBuilder;
import com.colasoft.opensearch.threadpool.FixedExecutorBuilder;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The experimental plugin which implements the concurrent search over Apache Lucene segments.
 */
public class ConcurrentSegmentSearchPlugin extends Plugin implements SearchPlugin {
    private static final String INDEX_SEARCHER = "index_searcher";

    /**
     * Default constructor
     */
    public ConcurrentSegmentSearchPlugin() {}

    @Override
    public Optional<QueryPhaseSearcher> getQueryPhaseSearcher() {
        return Optional.of(new ConcurrentQueryPhaseSearcher());
    }

    @Override
    public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        final int allocatedProcessors = OpenSearchExecutors.allocatedProcessors(settings);
        return Collections.singletonList(
            new FixedExecutorBuilder(settings, INDEX_SEARCHER, allocatedProcessors, 1000, "thread_pool." + INDEX_SEARCHER)
        );
    }

    @Override
    public Optional<ExecutorServiceProvider> getIndexSearcherExecutorProvider() {
        return Optional.of((ThreadPool threadPool) -> threadPool.executor(INDEX_SEARCHER));
    }
}
