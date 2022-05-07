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

package org.opensearch.index.query;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.opensearch.common.ParseField;
import org.opensearch.common.ParsingException;
import org.opensearch.common.geo.ShapeRelation;
import org.opensearch.common.geo.SpatialStrategy;
import org.opensearch.common.geo.builders.ShapeBuilder;
import org.opensearch.common.geo.parsers.ShapeParser;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.geometry.Geometry;
import org.opensearch.index.mapper.GeoShapeQueryable;
import org.opensearch.index.mapper.MappedFieldType;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Derived {@link AbstractGeometryQueryBuilder} that builds a lat, lon GeoShape Query. It
 * can be applied to any {@link MappedFieldType} that implements {@link GeoShapeQueryable}.
 *
 * GeoJson and WKT shape definitions are supported
 *
 * @opensearch.internal
 */
public class GeoShapeQueryBuilder extends AbstractGeometryQueryBuilder<GeoShapeQueryBuilder> {
    public static final String NAME = "geo_shape";
    protected static final ParseField STRATEGY_FIELD = new ParseField("strategy");

    private SpatialStrategy strategy;

    /**
     * Creates a new GeoShapeQueryBuilder whose Query will be against the given
     * field name using the given Shape
     *
     * @param fieldName
     *            Name of the field that will be queried
     * @param shape
     *            Shape used in the Query
     */
    public GeoShapeQueryBuilder(String fieldName, Geometry shape) {
        super(fieldName, shape);
    }

    /**
     * Creates a new GeoShapeQueryBuilder whose Query will be against the given
     * field name using the given Shape
     *
     * @param fieldName
     *            Name of the field that will be queried
     * @param shape
     *            Shape used in the Query
     *
     * @deprecated use {@link #GeoShapeQueryBuilder(String, Geometry)} instead
     */
    @Deprecated
    public GeoShapeQueryBuilder(String fieldName, ShapeBuilder shape) {
        super(fieldName, shape);
    }

    public GeoShapeQueryBuilder(String fieldName, Supplier<Geometry> shapeSupplier, String indexedShapeId) {
        super(fieldName, shapeSupplier, indexedShapeId);
    }

    /**
     * Creates a new GeoShapeQueryBuilder whose Query will be against the given
     * field name and will use the Shape found with the given ID
     *
     * @param fieldName
     *            Name of the field that will be filtered
     * @param indexedShapeId
     *            ID of the indexed Shape that will be used in the Query
     */
    public GeoShapeQueryBuilder(String fieldName, String indexedShapeId) {
        super(fieldName, indexedShapeId);
    }

    public GeoShapeQueryBuilder(StreamInput in) throws IOException {
        super(in);
        strategy = in.readOptionalWriteable(SpatialStrategy::readFromStream);
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        super.doWriteTo(out);
        out.writeOptionalWriteable(strategy);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    /**
     * Sets the relation of query shape and indexed shape.
     *
     * @param relation relation of the shapes
     * @return this
     */
    public GeoShapeQueryBuilder relation(ShapeRelation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("No Shape Relation defined");
        }
        if (SpatialStrategy.TERM.equals(strategy) && relation != ShapeRelation.INTERSECTS) {
            throw new IllegalArgumentException(
                "current strategy ["
                    + strategy.getStrategyName()
                    + "] only supports relation ["
                    + ShapeRelation.INTERSECTS.getRelationName()
                    + "] found relation ["
                    + relation.getRelationName()
                    + "]"
            );
        }
        this.relation = relation;
        return this;
    }

    /**
     * Defines which spatial strategy will be used for building the geo shape
     * Query. When not set, the strategy that will be used will be the one that
     * is associated with the geo shape field in the mappings.
     *
     * @param strategy
     *            The spatial strategy to use for building the geo shape Query
     * @return this
     */
    public GeoShapeQueryBuilder strategy(SpatialStrategy strategy) {
        if (strategy != null && strategy == SpatialStrategy.TERM && relation != ShapeRelation.INTERSECTS) {
            throw new IllegalArgumentException(
                "strategy ["
                    + strategy.getStrategyName()
                    + "] only supports relation ["
                    + ShapeRelation.INTERSECTS.getRelationName()
                    + "] found relation ["
                    + relation.getRelationName()
                    + "]"
            );
        }
        this.strategy = strategy;
        return this;
    }

    /**
     * @return The spatial strategy to use for building the geo shape Query
     */
    public SpatialStrategy strategy() {
        return strategy;
    }

    @Override
    public void doShapeQueryXContent(XContentBuilder builder, Params params) throws IOException {
        if (strategy != null) {
            builder.field(STRATEGY_FIELD.getPreferredName(), strategy.getStrategyName());
        }
    }

    @Override
    protected GeoShapeQueryBuilder newShapeQueryBuilder(String fieldName, Geometry shape) {
        return new GeoShapeQueryBuilder(fieldName, shape);
    }

    @Override
    protected GeoShapeQueryBuilder newShapeQueryBuilder(String fieldName, Supplier<Geometry> shapeSupplier, String indexedShapeId) {
        return new GeoShapeQueryBuilder(fieldName, shapeSupplier, indexedShapeId);
    }

    @Override
    public Query buildShapeQuery(QueryShardContext context, MappedFieldType fieldType) {
        if ((fieldType instanceof GeoShapeQueryable) == false) {
            throw new QueryShardException(
                context,
                "Field [" + fieldName + "] is of unsupported type [" + fieldType.typeName() + "] for [" + NAME + "] query"
            );
        }
        final GeoShapeQueryable ft = (GeoShapeQueryable) fieldType;
        return new ConstantScoreQuery(ft.geoShapeQuery(shape, fieldName, strategy, relation, context));
    }

    @Override
    protected boolean doEquals(GeoShapeQueryBuilder other) {
        return super.doEquals((AbstractGeometryQueryBuilder) other) && Objects.equals(strategy, other.strategy);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(super.doHashCode(), strategy);
    }

    @Override
    protected GeoShapeQueryBuilder doRewrite(QueryRewriteContext queryRewriteContext) throws IOException {
        GeoShapeQueryBuilder builder = (GeoShapeQueryBuilder) super.doRewrite(queryRewriteContext);
        builder.strategy(strategy);
        return builder;
    }

    private static class ParsedGeoShapeQueryParams extends ParsedGeometryQueryParams {
        SpatialStrategy strategy;

        @Override
        protected boolean parseXContentField(XContentParser parser) throws IOException {
            SpatialStrategy strategy;
            if (SHAPE_FIELD.match(parser.currentName(), parser.getDeprecationHandler())) {
                this.shape = ShapeParser.parse(parser);
                return true;
            } else if (STRATEGY_FIELD.match(parser.currentName(), parser.getDeprecationHandler())) {
                String strategyName = parser.text();
                strategy = SpatialStrategy.fromString(strategyName);
                if (strategy == null) {
                    throw new ParsingException(parser.getTokenLocation(), "Unknown strategy [" + strategyName + " ]");
                } else {
                    this.strategy = strategy;
                }
                return true;
            }
            return false;
        }
    }

    public static GeoShapeQueryBuilder fromXContent(XContentParser parser) throws IOException {
        ParsedGeoShapeQueryParams pgsqp = (ParsedGeoShapeQueryParams) AbstractGeometryQueryBuilder.parsedParamsFromXContent(
            parser,
            new ParsedGeoShapeQueryParams()
        );

        GeoShapeQueryBuilder builder;

        if (pgsqp.shape != null) {
            builder = new GeoShapeQueryBuilder(pgsqp.fieldName, pgsqp.shape);
        } else {
            builder = new GeoShapeQueryBuilder(pgsqp.fieldName, pgsqp.id);
        }

        if (pgsqp.index != null) {
            builder.indexedShapeIndex(pgsqp.index);
        }

        if (pgsqp.shapePath != null) {
            builder.indexedShapePath(pgsqp.shapePath);
        }

        if (pgsqp.shapeRouting != null) {
            builder.indexedShapeRouting(pgsqp.shapeRouting);
        }

        if (pgsqp.relation != null) {
            builder.relation(pgsqp.relation);
        }

        if (pgsqp.strategy != null) {
            builder.strategy(pgsqp.strategy);
        }

        if (pgsqp.queryName != null) {
            builder.queryName(pgsqp.queryName);
        }

        builder.boost(pgsqp.boost);
        builder.ignoreUnmapped(pgsqp.ignoreUnmapped);
        return builder;
    }
}
