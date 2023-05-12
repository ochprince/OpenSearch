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

package com.colasoft.opensearch.search.aggregations.bucket.histogram;

import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.Rounding;
import com.colasoft.opensearch.search.aggregations.Aggregator;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories;
import com.colasoft.opensearch.search.aggregations.BucketOrder;
import com.colasoft.opensearch.search.aggregations.CardinalityUpperBound;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

/**
 * Aggregator supplier interface for date_histogram agg
 *
 * @opensearch.internal
 */
@FunctionalInterface
public interface DateHistogramAggregationSupplier {
    Aggregator build(
        String name,
        AggregatorFactories factories,
        Rounding rounding,
        Rounding.Prepared preparedRounding,
        BucketOrder order,
        boolean keyed,
        long minDocCount,
        @Nullable LongBounds extendedBounds,
        @Nullable LongBounds hardBounds,
        ValuesSourceConfig valuesSourceConfig,
        SearchContext aggregationContext,
        Aggregator parent,
        CardinalityUpperBound cardinality,
        Map<String, Object> metadata
    ) throws IOException;
}
