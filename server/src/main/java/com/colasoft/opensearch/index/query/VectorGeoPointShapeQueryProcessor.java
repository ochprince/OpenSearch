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

package com.colasoft.opensearch.index.query;

import org.apache.lucene.document.LatLonDocValuesField;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexOrDocValuesQuery;
import org.apache.lucene.search.Query;
import com.colasoft.opensearch.common.geo.GeoPolygonDecomposer;
import com.colasoft.opensearch.common.geo.GeoShapeType;
import com.colasoft.opensearch.common.geo.GeoShapeUtils;
import com.colasoft.opensearch.common.geo.ShapeRelation;
import com.colasoft.opensearch.geometry.Circle;
import com.colasoft.opensearch.geometry.Geometry;
import com.colasoft.opensearch.geometry.GeometryCollection;
import com.colasoft.opensearch.geometry.GeometryVisitor;
import com.colasoft.opensearch.geometry.LinearRing;
import com.colasoft.opensearch.geometry.MultiLine;
import com.colasoft.opensearch.geometry.MultiPoint;
import com.colasoft.opensearch.geometry.MultiPolygon;
import com.colasoft.opensearch.geometry.Point;
import com.colasoft.opensearch.geometry.Polygon;
import com.colasoft.opensearch.geometry.Rectangle;
import com.colasoft.opensearch.geometry.ShapeType;
import com.colasoft.opensearch.index.mapper.GeoPointFieldMapper;
import com.colasoft.opensearch.index.mapper.MappedFieldType;

import java.util.ArrayList;

/**
 * Query processor for lucene 6 LatLonPoint queries
 *
 * @opensearch.internal
 */
public class VectorGeoPointShapeQueryProcessor {

    public Query geoShapeQuery(Geometry shape, String fieldName, ShapeRelation relation, QueryShardContext context) {
        validateIsGeoPointFieldType(fieldName, context);
        // geo points only support intersects
        if (relation != ShapeRelation.INTERSECTS) {
            throw new QueryShardException(context, relation + " query relation not supported for Field [" + fieldName + "].");
        }
        // wrap geoQuery as a ConstantScoreQuery
        return getVectorQueryFromShape(shape, fieldName, relation, context);
    }

    private void validateIsGeoPointFieldType(String fieldName, QueryShardContext context) {
        MappedFieldType fieldType = context.fieldMapper(fieldName);
        if (fieldType instanceof GeoPointFieldMapper.GeoPointFieldType == false) {
            throw new QueryShardException(
                context,
                "Expected "
                    + GeoPointFieldMapper.CONTENT_TYPE
                    + " field type for Field ["
                    + fieldName
                    + "] but found "
                    + fieldType.typeName()
            );
        }
    }

    protected Query getVectorQueryFromShape(Geometry queryShape, String fieldName, ShapeRelation relation, QueryShardContext context) {
        ShapeVisitor shapeVisitor = new ShapeVisitor(context, fieldName, relation);
        return queryShape.visit(shapeVisitor);
    }

    private class ShapeVisitor implements GeometryVisitor<Query, RuntimeException> {
        QueryShardContext context;
        MappedFieldType fieldType;
        String fieldName;
        ShapeRelation relation;

        ShapeVisitor(QueryShardContext context, String fieldName, ShapeRelation relation) {
            this.context = context;
            this.fieldType = context.fieldMapper(fieldName);
            this.fieldName = fieldName;
            this.relation = relation;
        }

        @Override
        public Query visit(Circle circle) {
            Query query = LatLonPoint.newDistanceQuery(fieldName, circle.getLat(), circle.getLon(), circle.getRadiusMeters());
            if (fieldType.hasDocValues()) {
                Query dvQuery = LatLonDocValuesField.newSlowDistanceQuery(
                    fieldName,
                    circle.getLat(),
                    circle.getLon(),
                    circle.getRadiusMeters()
                );
                query = new IndexOrDocValuesQuery(query, dvQuery);
            }
            return query;
        }

        @Override
        public Query visit(GeometryCollection<?> collection) {
            BooleanQuery.Builder bqb = new BooleanQuery.Builder();
            visit(bqb, collection);
            return bqb.build();
        }

        private void visit(BooleanQuery.Builder bqb, GeometryCollection<?> collection) {
            BooleanClause.Occur occur = BooleanClause.Occur.FILTER;
            for (Geometry shape : collection) {
                bqb.add(shape.visit(this), occur);
            }
        }

        @Override
        public Query visit(com.colasoft.opensearch.geometry.Line line) {
            throw new QueryShardException(context, "Field [" + fieldName + "] does not support " + GeoShapeType.LINESTRING + " queries");
        }

        @Override
        // don't think this is called directly
        public Query visit(LinearRing ring) {
            throw new QueryShardException(context, "Field [" + fieldName + "] does not support " + ShapeType.LINEARRING + " queries");
        }

        @Override
        public Query visit(MultiLine multiLine) {
            throw new QueryShardException(
                context,
                "Field [" + fieldName + "] does not support " + GeoShapeType.MULTILINESTRING + " queries"
            );
        }

        @Override
        public Query visit(MultiPoint multiPoint) {
            throw new QueryShardException(context, "Field [" + fieldName + "] does not support " + GeoShapeType.MULTIPOINT + " queries");
        }

        // helper for visit(MultiPolygon multiPolygon) and visit(Polygon polygon)
        private Query visit(ArrayList<Polygon> collector) {
            org.apache.lucene.geo.Polygon[] lucenePolygons = new org.apache.lucene.geo.Polygon[collector.size()];
            for (int i = 0; i < collector.size(); i++) {
                lucenePolygons[i] = GeoShapeUtils.toLucenePolygon(collector.get(i));
            }
            Query query = LatLonPoint.newPolygonQuery(fieldName, lucenePolygons);
            if (fieldType.hasDocValues()) {
                Query dvQuery = LatLonDocValuesField.newSlowPolygonQuery(fieldName, lucenePolygons);
                query = new IndexOrDocValuesQuery(query, dvQuery);
            }
            return query;
        }

        @Override
        public Query visit(MultiPolygon multiPolygon) {
            ArrayList<com.colasoft.opensearch.geometry.Polygon> collector = new ArrayList<>();
            GeoPolygonDecomposer.decomposeMultiPolygon(multiPolygon, true, collector);
            return visit(collector);
        }

        @Override
        public Query visit(Point point) {
            // not currently supported
            throw new QueryShardException(context, "Field [" + fieldName + "] does not support " + GeoShapeType.POINT + " queries");
        }

        @Override
        public Query visit(Polygon polygon) {
            ArrayList<com.colasoft.opensearch.geometry.Polygon> collector = new ArrayList<>();
            GeoPolygonDecomposer.decomposePolygon(polygon, true, collector);
            return visit(collector);
        }

        @Override
        public Query visit(Rectangle r) {
            Query query = LatLonPoint.newBoxQuery(fieldName, r.getMinY(), r.getMaxY(), r.getMinX(), r.getMaxX());
            if (fieldType.hasDocValues()) {
                Query dvQuery = LatLonDocValuesField.newSlowBoxQuery(fieldName, r.getMinY(), r.getMaxY(), r.getMinX(), r.getMaxX());
                query = new IndexOrDocValuesQuery(query, dvQuery);
            }
            return query;
        }
    }
}
