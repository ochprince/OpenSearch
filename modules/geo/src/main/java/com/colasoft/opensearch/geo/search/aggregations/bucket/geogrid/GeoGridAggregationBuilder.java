/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The ColaSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

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

package com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.geo.GeoBoundingBox;
import com.colasoft.opensearch.common.geo.GeoPoint;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ObjectParser;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.aggregations.AggregatorFactories.Builder;
import com.colasoft.opensearch.search.aggregations.AggregatorFactory;
import com.colasoft.opensearch.search.aggregations.bucket.BucketUtils;
import com.colasoft.opensearch.search.aggregations.support.CoreValuesSourceType;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregationBuilder;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceAggregatorFactory;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceType;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base Aggregation Builder for geogrid aggs
 *
 * @opensearch.internal
 */
public abstract class GeoGridAggregationBuilder extends ValuesSourceAggregationBuilder<GeoGridAggregationBuilder> {
    /* recognized field names in JSON */
    static final ParseField FIELD_PRECISION = new ParseField("precision");
    static final ParseField FIELD_SIZE = new ParseField("size");
    static final ParseField FIELD_SHARD_SIZE = new ParseField("shard_size");

    protected int precision;
    protected int requiredSize;
    protected int shardSize;
    private GeoBoundingBox geoBoundingBox = new GeoBoundingBox(new GeoPoint(Double.NaN, Double.NaN), new GeoPoint(Double.NaN, Double.NaN));

    /**
     * A precision parser
     *
     * @opensearch.internal
     */
    @FunctionalInterface
    protected interface PrecisionParser {
        int parse(XContentParser parser) throws IOException;
    }

    public static <T extends GeoGridAggregationBuilder> ObjectParser<T, String> createParser(
        String name,
        PrecisionParser precisionParser,
        Function<String, T> ctor
    ) {
        ObjectParser<T, String> parser = ObjectParser.fromBuilder(name, ctor);
        ValuesSourceAggregationBuilder.declareFields(parser, false, false, false);
        parser.declareField(
            (p, builder, context) -> builder.precision(precisionParser.parse(p)),
            FIELD_PRECISION,
            ObjectParser.ValueType.INT
        );
        parser.declareInt(GeoGridAggregationBuilder::size, FIELD_SIZE);
        parser.declareInt(GeoGridAggregationBuilder::shardSize, FIELD_SHARD_SIZE);
        parser.declareField(
            (p, builder, context) -> { builder.setGeoBoundingBox(GeoBoundingBox.parseBoundingBox(p)); },
            GeoBoundingBox.BOUNDS_FIELD,
            ObjectParser.ValueType.OBJECT
        );
        return parser;
    }

    public GeoGridAggregationBuilder(String name) {
        super(name);
    }

    protected GeoGridAggregationBuilder(GeoGridAggregationBuilder clone, Builder factoriesBuilder, Map<String, Object> metadata) {
        super(clone, factoriesBuilder, metadata);
        this.precision = clone.precision;
        this.requiredSize = clone.requiredSize;
        this.shardSize = clone.shardSize;
        this.geoBoundingBox = clone.geoBoundingBox;
    }

    /**
     * Read from a stream.
     */
    public GeoGridAggregationBuilder(StreamInput in) throws IOException {
        super(in);
        precision = in.readVInt();
        requiredSize = in.readVInt();
        shardSize = in.readVInt();
        if (in.getVersion().onOrAfter(LegacyESVersion.V_7_6_0)) {
            geoBoundingBox = new GeoBoundingBox(in);
        }
    }

    @Override
    protected ValuesSourceType defaultValueSourceType() {
        return CoreValuesSourceType.GEOPOINT;
    }

    @Override
    protected void innerWriteTo(StreamOutput out) throws IOException {
        out.writeVInt(precision);
        out.writeVInt(requiredSize);
        out.writeVInt(shardSize);
        if (out.getVersion().onOrAfter(LegacyESVersion.V_7_6_0)) {
            geoBoundingBox.writeTo(out);
        }
    }

    /**
     * method to validate and set the precision value
     * @param precision the precision to set for the aggregation
     * @return the {@link GeoGridAggregationBuilder} builder
     */
    public abstract GeoGridAggregationBuilder precision(int precision);

    /**
     * Creates a new instance of the {@link ValuesSourceAggregatorFactory}-derived class specific to the geo aggregation.
     */
    protected abstract ValuesSourceAggregatorFactory createFactory(
        String name,
        ValuesSourceConfig config,
        int precision,
        int requiredSize,
        int shardSize,
        GeoBoundingBox geoBoundingBox,
        QueryShardContext queryShardContext,
        AggregatorFactory parent,
        Builder subFactoriesBuilder,
        Map<String, Object> metadata
    ) throws IOException;

    public int precision() {
        return precision;
    }

    public GeoGridAggregationBuilder size(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("[size] must be greater than 0. Found [" + size + "] in [" + name + "]");
        }
        this.requiredSize = size;
        return this;
    }

    public int size() {
        return requiredSize;
    }

    public GeoGridAggregationBuilder shardSize(int shardSize) {
        if (shardSize <= 0) {
            throw new IllegalArgumentException("[shardSize] must be greater than 0. Found [" + shardSize + "] in [" + name + "]");
        }
        this.shardSize = shardSize;
        return this;
    }

    public int shardSize() {
        return shardSize;
    }

    public GeoGridAggregationBuilder setGeoBoundingBox(GeoBoundingBox geoBoundingBox) {
        this.geoBoundingBox = geoBoundingBox;
        // no validation done here, similar to geo_bounding_box query behavior.
        return this;
    }

    public GeoBoundingBox geoBoundingBox() {
        return geoBoundingBox;
    }

    @Override
    public final BucketCardinality bucketCardinality() {
        return BucketCardinality.MANY;
    }

    @Override
    protected ValuesSourceAggregatorFactory innerBuild(
        QueryShardContext queryShardContext,
        ValuesSourceConfig config,
        AggregatorFactory parent,
        Builder subFactoriesBuilder
    ) throws IOException {
        int shardSize = this.shardSize;

        int requiredSize = this.requiredSize;

        if (shardSize < 0) {
            // Use default heuristic to avoid any wrong-ranking caused by
            // distributed counting
            shardSize = BucketUtils.suggestShardSideQueueSize(requiredSize);
        }

        if (requiredSize <= 0 || shardSize <= 0) {
            throw new OpenSearchException(
                "parameters [required_size] and [shard_size] must be > 0 in " + getType() + " aggregation [" + name + "]."
            );
        }

        if (shardSize < requiredSize) {
            shardSize = requiredSize;
        }
        return createFactory(
            name,
            config,
            precision,
            requiredSize,
            shardSize,
            geoBoundingBox,
            queryShardContext,
            parent,
            subFactoriesBuilder,
            metadata
        );
    }

    @Override
    protected XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(FIELD_PRECISION.getPreferredName(), precision);
        builder.field(FIELD_SIZE.getPreferredName(), requiredSize);
        if (shardSize > -1) {
            builder.field(FIELD_SHARD_SIZE.getPreferredName(), shardSize);
        }
        if (geoBoundingBox.isUnbounded() == false) {
            geoBoundingBox.toXContent(builder, params);
        }
        return builder;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (super.equals(obj) == false) return false;
        GeoGridAggregationBuilder other = (GeoGridAggregationBuilder) obj;
        return precision == other.precision
            && requiredSize == other.requiredSize
            && shardSize == other.shardSize
            && Objects.equals(geoBoundingBox, other.geoBoundingBox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), precision, requiredSize, shardSize, geoBoundingBox);
    }
}
