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

package com.colasoft.opensearch.search.aggregations.pipeline;

import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;

/**
 * Aggregation Builder for stats_bucket agg
 *
 * @opensearch.internal
 */
public class StatsBucketPipelineAggregationBuilder extends BucketMetricsPipelineAggregationBuilder<StatsBucketPipelineAggregationBuilder> {
    public static final String NAME = "stats_bucket";

    public StatsBucketPipelineAggregationBuilder(String name, String bucketsPath) {
        super(name, NAME, new String[] { bucketsPath });
    }

    /**
     * Read from a stream.
     */
    public StatsBucketPipelineAggregationBuilder(StreamInput in) throws IOException {
        super(in, NAME);
    }

    @Override
    protected void innerWriteTo(StreamOutput out) throws IOException {
        // Do nothing, no extra state to write to stream
    }

    @Override
    protected PipelineAggregator createInternal(Map<String, Object> metadata) {
        return new StatsBucketPipelineAggregator(name, bucketsPaths, gapPolicy(), formatter(), metadata);
    }

    @Override
    protected XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        return builder;
    }

    public static final PipelineAggregator.Parser PARSER = new BucketMetricsParser() {
        @Override
        protected StatsBucketPipelineAggregationBuilder buildFactory(
            String pipelineAggregatorName,
            String bucketsPath,
            Map<String, Object> params
        ) {
            return new StatsBucketPipelineAggregationBuilder(pipelineAggregatorName, bucketsPath);
        }
    };

    @Override
    public String getWriteableName() {
        return NAME;
    }
}
