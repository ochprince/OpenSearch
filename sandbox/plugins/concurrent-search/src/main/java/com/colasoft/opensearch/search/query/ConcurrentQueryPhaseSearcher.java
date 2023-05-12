/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.query;

import static com.colasoft.opensearch.search.query.TopDocsCollectorContext.createTopDocsCollectorContext;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.Query;
import com.colasoft.opensearch.search.internal.ContextIndexSearcher;
import com.colasoft.opensearch.search.internal.SearchContext;
import com.colasoft.opensearch.search.profile.query.ProfileCollectorManager;
import com.colasoft.opensearch.search.query.QueryPhase.DefaultQueryPhaseSearcher;
import com.colasoft.opensearch.search.query.QueryPhase.TimeExceededException;

/**
 * The implementation of the {@link QueryPhaseSearcher} which attempts to use concurrent
 * search of Apache Lucene segments if it has been enabled.
 */
public class ConcurrentQueryPhaseSearcher extends DefaultQueryPhaseSearcher {
    private static final Logger LOGGER = LogManager.getLogger(ConcurrentQueryPhaseSearcher.class);

    /**
     * Default constructor
     */
    public ConcurrentQueryPhaseSearcher() {}

    @Override
    protected boolean searchWithCollector(
        SearchContext searchContext,
        ContextIndexSearcher searcher,
        Query query,
        LinkedList<QueryCollectorContext> collectors,
        boolean hasFilterCollector,
        boolean hasTimeout
    ) throws IOException {
        boolean couldUseConcurrentSegmentSearch = allowConcurrentSegmentSearch(searcher);

        // TODO: support aggregations
        if (searchContext.aggregations() != null) {
            couldUseConcurrentSegmentSearch = false;
            LOGGER.debug("Unable to use concurrent search over index segments (experimental): aggregations are present");
        }

        if (couldUseConcurrentSegmentSearch) {
            LOGGER.debug("Using concurrent search over index segments (experimental)");
            return searchWithCollectorManager(searchContext, searcher, query, collectors, hasFilterCollector, hasTimeout);
        } else {
            return super.searchWithCollector(searchContext, searcher, query, collectors, hasFilterCollector, hasTimeout);
        }
    }

    private static boolean searchWithCollectorManager(
        SearchContext searchContext,
        ContextIndexSearcher searcher,
        Query query,
        LinkedList<QueryCollectorContext> collectorContexts,
        boolean hasFilterCollector,
        boolean timeoutSet
    ) throws IOException {
        // create the top docs collector last when the other collectors are known
        final TopDocsCollectorContext topDocsFactory = createTopDocsCollectorContext(searchContext, hasFilterCollector);
        // add the top docs collector, the first collector context in the chain
        collectorContexts.addFirst(topDocsFactory);

        final QuerySearchResult queryResult = searchContext.queryResult();
        final CollectorManager<?, ReduceableSearchResult> collectorManager;

        // TODO: support aggregations in concurrent segment search flow
        if (searchContext.aggregations() != null) {
            throw new UnsupportedOperationException("The concurrent segment search does not support aggregations yet");
        }

        if (searchContext.getProfilers() != null) {
            final ProfileCollectorManager<? extends Collector, ReduceableSearchResult> profileCollectorManager =
                QueryCollectorManagerContext.createQueryCollectorManagerWithProfiler(collectorContexts);
            searchContext.getProfilers().getCurrentQueryProfiler().setCollector(profileCollectorManager);
            collectorManager = profileCollectorManager;
        } else {
            // Create multi collector manager instance
            collectorManager = QueryCollectorManagerContext.createMultiCollectorManager(collectorContexts);
        }

        try {
            final ReduceableSearchResult result = searcher.search(query, collectorManager);
            result.reduce(queryResult);
        } catch (EarlyTerminatingCollector.EarlyTerminationException e) {
            queryResult.terminatedEarly(true);
        } catch (TimeExceededException e) {
            assert timeoutSet : "TimeExceededException thrown even though timeout wasn't set";
            if (searchContext.request().allowPartialSearchResults() == false) {
                // Can't rethrow TimeExceededException because not serializable
                throw new QueryPhaseExecutionException(searchContext.shardTarget(), "Time exceeded");
            }
            queryResult.searchTimedOut(true);
        }
        if (searchContext.terminateAfter() != SearchContext.DEFAULT_TERMINATE_AFTER && queryResult.terminatedEarly() == null) {
            queryResult.terminatedEarly(false);
        }

        return topDocsFactory.shouldRescore();
    }

    private static boolean allowConcurrentSegmentSearch(final ContextIndexSearcher searcher) {
        return (searcher.getExecutor() != null);
    }

}
