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

package com.colasoft.opensearch.search.aggregations.metrics;

import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.aggregations.Aggregator;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.CardinalityUpperBound;
import com.colasoft.opensearch.search.aggregations.support.CoreValuesSourceType;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceRegistry;
import com.colasoft.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * This factory is used to generate both TDigest and HDRHisto aggregators, depending
 * on the selected method
 *
 * @opensearch.internal
 */
class PercentilesAggregatorFactory extends ValuesSourceAggregatorFactory {

    private final double[] percents;
    private final PercentilesConfig percentilesConfig;
    private final boolean keyed;

    static void registerAggregators(ValuesSourceRegistry.Builder builder) {
        builder.register(
            PercentilesAggregationBuilder.REGISTRY_KEY,
            Arrays.asList(CoreValuesSourceType.NUMERIC, CoreValuesSourceType.DATE, CoreValuesSourceType.BOOLEAN),
            (name, valuesSource, context, parent, percents, percentilesConfig, keyed, formatter, metadata) -> percentilesConfig
                .createPercentilesAggregator(name, valuesSource, context, parent, percents, keyed, formatter, metadata),
            true
        );
    }

    PercentilesAggregatorFactory(
        String name,
        ValuesSourceConfig config,
        double[] percents,
        PercentilesConfig percentilesConfig,
        boolean keyed,
        QueryShardContext queryShardContext,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, config, queryShardContext, parent, subFactoriesBuilder, metadata);
        this.percents = percents;
        this.percentilesConfig = percentilesConfig;
        this.keyed = keyed;
    }

    @Override
    protected Aggregator createUnmapped(SearchContext searchContext, Aggregator parent, Map<String, Object> metadata) throws IOException {

        return percentilesConfig.createPercentilesAggregator(name, null, searchContext, parent, percents, keyed, config.format(), metadata);
    }

    @Override
    protected Aggregator doCreateInternal(
        SearchContext searchContext,
        Aggregator parent,
        CardinalityUpperBound bucketCardinality,
        Map<String, Object> metadata
    ) throws IOException {
        return queryShardContext.getValuesSourceRegistry()
            .getAggregator(PercentilesAggregationBuilder.REGISTRY_KEY, config)
            .build(name, config.getValuesSource(), searchContext, parent, percents, percentilesConfig, keyed, config.format(), metadata);
    }
}
