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

package org.opensearch.search.fetch;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.search.SearchPhaseResult;
import org.opensearch.search.SearchShardTarget;
import org.opensearch.search.query.QuerySearchResult;

import java.io.IOException;

public final class ScrollQueryFetchSearchResult extends SearchPhaseResult {

    private final QueryFetchSearchResult result;

    public ScrollQueryFetchSearchResult(StreamInput in) throws IOException {
        super(in);
        SearchShardTarget searchShardTarget = new SearchShardTarget(in);
        result = new QueryFetchSearchResult(in);
        setSearchShardTarget(searchShardTarget);
    }

    public ScrollQueryFetchSearchResult(QueryFetchSearchResult result, SearchShardTarget shardTarget) {
        this.result = result;
        setSearchShardTarget(shardTarget);
    }

    public QueryFetchSearchResult result() {
        return result;
    }

    @Override
    public void setSearchShardTarget(SearchShardTarget shardTarget) {
        super.setSearchShardTarget(shardTarget);
        result.setSearchShardTarget(shardTarget);
    }

    @Override
    public void setShardIndex(int shardIndex) {
        super.setShardIndex(shardIndex);
        result.setShardIndex(shardIndex);
    }

    @Override
    public QuerySearchResult queryResult() {
        return result.queryResult();
    }

    @Override
    public FetchSearchResult fetchResult() {
        return result.fetchResult();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        getSearchShardTarget().writeTo(out);
        result.writeTo(out);
    }
}
