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

package com.colasoft.opensearch.action.search;

import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.util.concurrent.AtomicArray;
import com.colasoft.opensearch.search.fetch.QueryFetchSearchResult;
import com.colasoft.opensearch.search.fetch.ScrollQueryFetchSearchResult;
import com.colasoft.opensearch.search.internal.InternalScrollSearchRequest;
import com.colasoft.opensearch.transport.Transport;

import java.util.function.BiFunction;

/**
 * Async action for a search scroll query then fetch
 *
 * @opensearch.internal
 */
final class SearchScrollQueryAndFetchAsyncAction extends SearchScrollAsyncAction<ScrollQueryFetchSearchResult> {

    private final SearchTask task;
    private final AtomicArray<QueryFetchSearchResult> queryFetchResults;

    SearchScrollQueryAndFetchAsyncAction(
        Logger logger,
        ClusterService clusterService,
        SearchTransportService searchTransportService,
        SearchPhaseController searchPhaseController,
        SearchScrollRequest request,
        SearchTask task,
        ParsedScrollId scrollId,
        ActionListener<SearchResponse> listener
    ) {
        super(scrollId, logger, clusterService.state().nodes(), listener, searchPhaseController, request, searchTransportService);
        this.task = task;
        this.queryFetchResults = new AtomicArray<>(scrollId.getContext().length);
    }

    @Override
    protected void executeInitialPhase(
        Transport.Connection connection,
        InternalScrollSearchRequest internalRequest,
        SearchActionListener<ScrollQueryFetchSearchResult> searchActionListener
    ) {
        searchTransportService.sendExecuteScrollFetch(connection, internalRequest, task, searchActionListener);
    }

    @Override
    protected SearchPhase moveToNextPhase(BiFunction<String, String, DiscoveryNode> clusterNodeLookup) {
        return sendResponsePhase(searchPhaseController.reducedScrollQueryPhase(queryFetchResults.asList()), queryFetchResults);
    }

    @Override
    protected void onFirstPhaseResult(int shardId, ScrollQueryFetchSearchResult result) {
        queryFetchResults.setOnce(shardId, result.result());
    }
}
