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

package org.opensearch.search.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.opensearch.lucene.queries.MinDocQuery;
import org.opensearch.lucene.queries.SearchAfterSortedDocQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.opensearch.action.search.SearchShardTask;
import org.opensearch.common.Booleans;
import org.opensearch.common.lucene.Lucene;
import org.opensearch.common.lucene.search.TopDocsAndMaxScore;
import org.opensearch.common.util.concurrent.QueueResizingOpenSearchThreadPoolExecutor;
import org.opensearch.search.DocValueFormat;
import org.opensearch.search.SearchContextSourcePrinter;
import org.opensearch.search.SearchService;
import org.opensearch.search.aggregations.AggregationPhase;
import org.opensearch.search.internal.ContextIndexSearcher;
import org.opensearch.search.internal.ScrollContext;
import org.opensearch.search.internal.SearchContext;
import org.opensearch.search.profile.ProfileShardResult;
import org.opensearch.search.profile.SearchProfileShardResults;
import org.opensearch.search.profile.query.InternalProfileCollector;
import org.opensearch.search.rescore.RescorePhase;
import org.opensearch.search.sort.SortAndFormats;
import org.opensearch.search.suggest.SuggestPhase;
import org.opensearch.tasks.TaskCancelledException;
import org.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static org.opensearch.search.query.QueryCollectorContext.createEarlyTerminationCollectorContext;
import static org.opensearch.search.query.QueryCollectorContext.createFilteredCollectorContext;
import static org.opensearch.search.query.QueryCollectorContext.createMinScoreCollectorContext;
import static org.opensearch.search.query.QueryCollectorContext.createMultiCollectorContext;
import static org.opensearch.search.query.TopDocsCollectorContext.createTopDocsCollectorContext;

/**
 * Query phase of a search request, used to run the query and get back from each shard information about the matching documents
 * (document ids and score or sort criteria) so that matches can be reduced on the coordinating node
 */
public class QueryPhase {
    private static final Logger LOGGER = LogManager.getLogger(QueryPhase.class);
    // TODO: remove this property
    public static final boolean SYS_PROP_REWRITE_SORT = Booleans.parseBoolean(System.getProperty("opensearch.search.rewrite_sort", "true"));
    public static final QueryPhaseSearcher DEFAULT_QUERY_PHASE_SEARCHER = new DefaultQueryPhaseSearcher();

    private final QueryPhaseSearcher queryPhaseSearcher;
    private final AggregationPhase aggregationPhase;
    private final SuggestPhase suggestPhase;
    private final RescorePhase rescorePhase;

    public QueryPhase() {
        this(DEFAULT_QUERY_PHASE_SEARCHER);
    }

    public QueryPhase(QueryPhaseSearcher queryPhaseSearcher) {
        this.queryPhaseSearcher = Objects.requireNonNull(queryPhaseSearcher, "QueryPhaseSearcher is required");
        this.aggregationPhase = new AggregationPhase();
        this.suggestPhase = new SuggestPhase();
        this.rescorePhase = new RescorePhase();
    }

    public void preProcess(SearchContext context) {
        final Runnable cancellation;
        if (context.lowLevelCancellation()) {
            cancellation = context.searcher().addQueryCancellation(() -> {
                SearchShardTask task = context.getTask();
                if (task != null && task.isCancelled()) {
                    throw new TaskCancelledException("cancelled task with reason: " + task.getReasonCancelled());
                }
            });
        } else {
            cancellation = null;
        }
        try {
            context.preProcess(true);
        } finally {
            if (cancellation != null) {
                context.searcher().removeQueryCancellation(cancellation);
            }
        }
    }

    public void execute(SearchContext searchContext) throws QueryPhaseExecutionException {
        if (searchContext.hasOnlySuggest()) {
            suggestPhase.execute(searchContext);
            searchContext.queryResult()
                .topDocs(
                    new TopDocsAndMaxScore(new TopDocs(new TotalHits(0, TotalHits.Relation.EQUAL_TO), Lucene.EMPTY_SCORE_DOCS), Float.NaN),
                    new DocValueFormat[0]
                );
            return;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{}", new SearchContextSourcePrinter(searchContext));
        }

        // Pre-process aggregations as late as possible. In the case of a DFS_Q_T_F
        // request, preProcess is called on the DFS phase phase, this is why we pre-process them
        // here to make sure it happens during the QUERY phase
        aggregationPhase.preProcess(searchContext);
        boolean rescore = executeInternal(searchContext, queryPhaseSearcher);

        if (rescore) { // only if we do a regular search
            rescorePhase.execute(searchContext);
        }
        suggestPhase.execute(searchContext);
        aggregationPhase.execute(searchContext);

        if (searchContext.getProfilers() != null) {
            ProfileShardResult shardResults = SearchProfileShardResults.buildShardResults(
                searchContext.getProfilers(),
                searchContext.request()
            );
            searchContext.queryResult().profileResults(shardResults);
        }
    }

    /**
     * In a package-private method so that it can be tested without having to
     * wire everything (mapperService, etc.)
     * @return whether the rescoring phase should be executed
     */
    static boolean executeInternal(SearchContext searchContext) throws QueryPhaseExecutionException {
        return executeInternal(searchContext, QueryPhase.DEFAULT_QUERY_PHASE_SEARCHER);
    }

    /**
     * In a package-private method so that it can be tested without having to
     * wire everything (mapperService, etc.)
     * @return whether the rescoring phase should be executed
     */
    static boolean executeInternal(SearchContext searchContext, QueryPhaseSearcher queryPhaseSearcher) throws QueryPhaseExecutionException {
        final ContextIndexSearcher searcher = searchContext.searcher();
        final IndexReader reader = searcher.getIndexReader();
        QuerySearchResult queryResult = searchContext.queryResult();
        queryResult.searchTimedOut(false);
        try {
            queryResult.from(searchContext.from());
            queryResult.size(searchContext.size());
            Query query = searchContext.query();
            assert query == searcher.rewrite(query); // already rewritten

            final ScrollContext scrollContext = searchContext.scrollContext();
            if (scrollContext != null) {
                if (scrollContext.totalHits == null) {
                    // first round
                    assert scrollContext.lastEmittedDoc == null;
                    // there is not much that we can optimize here since we want to collect all
                    // documents in order to get the total number of hits

                } else {
                    final ScoreDoc after = scrollContext.lastEmittedDoc;
                    if (returnsDocsInOrder(query, searchContext.sort())) {
                        // now this gets interesting: since we sort in index-order, we can directly
                        // skip to the desired doc
                        if (after != null) {
                            query = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST)
                                .add(new MinDocQuery(after.doc + 1), BooleanClause.Occur.FILTER)
                                .build();
                        }
                        // ... and stop collecting after ${size} matches
                        searchContext.terminateAfter(searchContext.size());
                    } else if (canEarlyTerminate(reader, searchContext.sort())) {
                        // now this gets interesting: since the search sort is a prefix of the index sort, we can directly
                        // skip to the desired doc
                        if (after != null) {
                            query = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST)
                                .add(new SearchAfterSortedDocQuery(searchContext.sort().sort, (FieldDoc) after), BooleanClause.Occur.FILTER)
                                .build();
                        }
                    }
                }
            }

            final LinkedList<QueryCollectorContext> collectors = new LinkedList<>();
            // whether the chain contains a collector that filters documents
            boolean hasFilterCollector = false;
            if (searchContext.terminateAfter() != SearchContext.DEFAULT_TERMINATE_AFTER) {
                // add terminate_after before the filter collectors
                // it will only be applied on documents accepted by these filter collectors
                collectors.add(createEarlyTerminationCollectorContext(searchContext.terminateAfter()));
                // this collector can filter documents during the collection
                hasFilterCollector = true;
            }
            if (searchContext.parsedPostFilter() != null) {
                // add post filters before aggregations
                // it will only be applied to top hits
                collectors.add(createFilteredCollectorContext(searcher, searchContext.parsedPostFilter().query()));
                // this collector can filter documents during the collection
                hasFilterCollector = true;
            }
            if (searchContext.queryCollectorManagers().isEmpty() == false) {
                // plug in additional collectors, like aggregations
                collectors.add(createMultiCollectorContext(searchContext.queryCollectorManagers().values()));
            }
            if (searchContext.minimumScore() != null) {
                // apply the minimum score after multi collector so we filter aggs as well
                collectors.add(createMinScoreCollectorContext(searchContext.minimumScore()));
                // this collector can filter documents during the collection
                hasFilterCollector = true;
            }

            boolean timeoutSet = scrollContext == null
                && searchContext.timeout() != null
                && searchContext.timeout().equals(SearchService.NO_TIMEOUT) == false;

            final Runnable timeoutRunnable;
            if (timeoutSet) {
                final long startTime = searchContext.getRelativeTimeInMillis();
                final long timeout = searchContext.timeout().millis();
                final long maxTime = startTime + timeout;
                timeoutRunnable = searcher.addQueryCancellation(() -> {
                    final long time = searchContext.getRelativeTimeInMillis();
                    if (time > maxTime) {
                        throw new TimeExceededException();
                    }
                });
            } else {
                timeoutRunnable = null;
            }

            if (searchContext.lowLevelCancellation()) {
                searcher.addQueryCancellation(() -> {
                    SearchShardTask task = searchContext.getTask();
                    if (task != null && task.isCancelled()) {
                        throw new TaskCancelledException("cancelled task with reason: " + task.getReasonCancelled());
                    }
                });
            }

            try {
                boolean shouldRescore = queryPhaseSearcher.searchWith(
                    searchContext,
                    searcher,
                    query,
                    collectors,
                    hasFilterCollector,
                    timeoutSet
                );

                ExecutorService executor = searchContext.indexShard().getThreadPool().executor(ThreadPool.Names.SEARCH);
                if (executor instanceof QueueResizingOpenSearchThreadPoolExecutor) {
                    QueueResizingOpenSearchThreadPoolExecutor rExecutor = (QueueResizingOpenSearchThreadPoolExecutor) executor;
                    queryResult.nodeQueueSize(rExecutor.getCurrentQueueSize());
                    queryResult.serviceTimeEWMA((long) rExecutor.getTaskExecutionEWMA());
                }

                return shouldRescore;
            } finally {
                // Search phase has finished, no longer need to check for timeout
                // otherwise aggregation phase might get cancelled.
                if (timeoutRunnable != null) {
                    searcher.removeQueryCancellation(timeoutRunnable);
                }
            }
        } catch (Exception e) {
            throw new QueryPhaseExecutionException(searchContext.shardTarget(), "Failed to execute main query", e);
        }
    }

    private static boolean searchWithCollector(
        SearchContext searchContext,
        ContextIndexSearcher searcher,
        Query query,
        LinkedList<QueryCollectorContext> collectors,
        boolean hasFilterCollector,
        boolean timeoutSet
    ) throws IOException {
        // create the top docs collector last when the other collectors are known
        final TopDocsCollectorContext topDocsFactory = createTopDocsCollectorContext(searchContext, hasFilterCollector);
        // add the top docs collector, the first collector context in the chain
        collectors.addFirst(topDocsFactory);

        final Collector queryCollector;
        if (searchContext.getProfilers() != null) {
            InternalProfileCollector profileCollector = QueryCollectorContext.createQueryCollectorWithProfiler(collectors);
            searchContext.getProfilers().getCurrentQueryProfiler().setCollector(profileCollector);
            queryCollector = profileCollector;
        } else {
            queryCollector = QueryCollectorContext.createQueryCollector(collectors);
        }
        QuerySearchResult queryResult = searchContext.queryResult();
        try {
            searcher.search(query, queryCollector);
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
        for (QueryCollectorContext ctx : collectors) {
            ctx.postProcess(queryResult);
        }
        return topDocsFactory.shouldRescore();
    }

    /**
     * Returns true if the provided <code>query</code> returns docs in index order (internal doc ids).
     * @param query The query to execute
     * @param sf The query sort
     */
    private static boolean returnsDocsInOrder(Query query, SortAndFormats sf) {
        if (sf == null || Sort.RELEVANCE.equals(sf.sort)) {
            // sort by score
            // queries that return constant scores will return docs in index
            // order since Lucene tie-breaks on the doc id
            return query.getClass() == ConstantScoreQuery.class || query.getClass() == MatchAllDocsQuery.class;
        } else {
            return Sort.INDEXORDER.equals(sf.sort);
        }
    }

    /**
     * Returns whether collection within the provided <code>reader</code> can be early-terminated if it sorts
     * with <code>sortAndFormats</code>.
     **/
    private static boolean canEarlyTerminate(IndexReader reader, SortAndFormats sortAndFormats) {
        if (sortAndFormats == null || sortAndFormats.sort == null) {
            return false;
        }
        final Sort sort = sortAndFormats.sort;
        for (LeafReaderContext ctx : reader.leaves()) {
            Sort indexSort = ctx.reader().getMetaData().getSort();
            if (indexSort == null || Lucene.canEarlyTerminate(sort, indexSort) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * The exception being raised when search timeout is reached.
     */
    public static class TimeExceededException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Default {@link QueryPhaseSearcher} implementation which delegates to the {@link QueryPhase}.
     */
    public static class DefaultQueryPhaseSearcher implements QueryPhaseSearcher {
        /**
         * Please use {@link QueryPhase#DEFAULT_QUERY_PHASE_SEARCHER}
         */
        protected DefaultQueryPhaseSearcher() {}

        @Override
        public boolean searchWith(
            SearchContext searchContext,
            ContextIndexSearcher searcher,
            Query query,
            LinkedList<QueryCollectorContext> collectors,
            boolean hasFilterCollector,
            boolean hasTimeout
        ) throws IOException {
            return searchWithCollector(searchContext, searcher, query, collectors, hasFilterCollector, hasTimeout);
        }

        protected boolean searchWithCollector(
            SearchContext searchContext,
            ContextIndexSearcher searcher,
            Query query,
            LinkedList<QueryCollectorContext> collectors,
            boolean hasFilterCollector,
            boolean hasTimeout
        ) throws IOException {
            return QueryPhase.searchWithCollector(searchContext, searcher, query, collectors, hasFilterCollector, hasTimeout);
        }
    }
}
