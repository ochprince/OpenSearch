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

package com.colasoft.opensearch.search.aggregations.bucket.range;

import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;

import java.io.IOException;
import java.util.Map;

/**
 * Aggregation Factory for date_range agg
 *
 * @opensearch.internal
 */
public class DateRangeAggregatorFactory extends AbstractRangeAggregatorFactory<RangeAggregator.Range> {

    public DateRangeAggregatorFactory(
        String name,
        ValuesSourceConfig config,
        RangeAggregator.Range[] ranges,
        boolean keyed,
        InternalRange.Factory<?, ?> rangeFactory,
        QueryShardContext queryShardContext,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder,
        Map<String, Object> metadata
    ) throws IOException {
        super(
            name,
            DateRangeAggregationBuilder.REGISTRY_KEY,
            config,
            ranges,
            keyed,
            rangeFactory,
            queryShardContext,
            parent,
            subFactoriesBuilder,
            metadata
        );
    }

}
