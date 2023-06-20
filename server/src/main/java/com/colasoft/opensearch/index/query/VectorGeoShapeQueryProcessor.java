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

package com.colasoft.opensearch.index.query;

import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.geo.GeoEncodingUtils;
import org.apache.lucene.geo.LatLonGeometry;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.common.geo.GeoLineDecomposer;
import com.colasoft.opensearch.common.geo.GeoPolygonDecomposer;
import com.colasoft.opensearch.common.geo.GeoShapeUtils;
import com.colasoft.opensearch.common.geo.ShapeRelation;
import com.colasoft.opensearch.geometry.Circle;
import com.colasoft.opensearch.geometry.Geometry;
import com.colasoft.opensearch.geometry.GeometryCollection;
import com.colasoft.opensearch.geometry.GeometryVisitor;
import com.colasoft.opensearch.geometry.Line;
import com.colasoft.opensearch.geometry.LinearRing;
import com.colasoft.opensearch.geometry.MultiLine;
import com.colasoft.opensearch.geometry.MultiPoint;
import com.colasoft.opensearch.geometry.MultiPolygon;
import com.colasoft.opensearch.geometry.Point;
import com.colasoft.opensearch.geometry.Polygon;
import com.colasoft.opensearch.geometry.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Query processor for Lucene 6 LatLonShape queries
 *
 * @opensearch.internal
 */
public class VectorGeoShapeQueryProcessor {

    public Query geoShapeQuery(Geometry shape, String fieldName, ShapeRelation relation, QueryShardContext context) {
        // CONTAINS queries are not supported by VECTOR strategy for indices created before version 7.5.0 (Lucene 8.3.0)
        if (relation == ShapeRelation.CONTAINS && context.indexVersionCreated().before(LegacyESVersion.V_7_5_0)) {
            throw new QueryShardException(context, ShapeRelation.CONTAINS + " query relation not supported for Field [" + fieldName + "].");
        }
        // wrap geoQuery as a ConstantScoreQuery
        return getVectorQueryFromShape(shape, fieldName, relation, context);
    }

    private Query getVectorQueryFromShape(Geometry queryShape, String fieldName, ShapeRelation relation, QueryShardContext context) {
        final LuceneGeometryCollector visitor = new LuceneGeometryCollector(fieldName, context);
        queryShape.visit(visitor);
        final List<LatLonGeometry> geometries = visitor.geometries();
        if (geometries.size() == 0) {
            return new MatchNoDocsQuery();
        }
        return LatLonShape.newGeometryQuery(
            fieldName,
            relation.getLuceneRelation(),
            geometries.toArray(new LatLonGeometry[geometries.size()])
        );
    }

    /**
     * Geometry collector for LatLonShape indexing types
     *
     * @opensearch.internal
     */
    private static class LuceneGeometryCollector implements GeometryVisitor<Void, RuntimeException> {
        private final List<LatLonGeometry> geometries = new ArrayList<>();
        private final String name;
        private final QueryShardContext context;

        private LuceneGeometryCollector(String name, QueryShardContext context) {
            this.name = name;
            this.context = context;
        }

        List<LatLonGeometry> geometries() {
            return geometries;
        }

        @Override
        public Void visit(Circle circle) {
            if (circle.isEmpty() == false) {
                geometries.add(GeoShapeUtils.toLuceneCircle(circle));
            }
            return null;
        }

        @Override
        public Void visit(GeometryCollection<?> collection) {
            for (Geometry shape : collection) {
                shape.visit(this);
            }
            return null;
        }

        @Override
        public Void visit(com.colasoft.opensearch.geometry.Line line) {
            if (line.isEmpty() == false) {
                List<com.colasoft.opensearch.geometry.Line> collector = new ArrayList<>();
                GeoLineDecomposer.decomposeLine(line, collector);
                collectLines(collector);
            }
            return null;
        }

        @Override
        public Void visit(LinearRing ring) {
            throw new QueryShardException(context, "Field [" + name + "] found and unsupported shape LinearRing");
        }

        @Override
        public Void visit(MultiLine multiLine) {
            List<com.colasoft.opensearch.geometry.Line> collector = new ArrayList<>();
            GeoLineDecomposer.decomposeMultiLine(multiLine, collector);
            collectLines(collector);
            return null;
        }

        @Override
        public Void visit(MultiPoint multiPoint) {
            for (Point point : multiPoint) {
                visit(point);
            }
            return null;
        }

        @Override
        public Void visit(MultiPolygon multiPolygon) {
            if (multiPolygon.isEmpty() == false) {
                List<com.colasoft.opensearch.geometry.Polygon> collector = new ArrayList<>();
                GeoPolygonDecomposer.decomposeMultiPolygon(multiPolygon, true, collector);
                collectPolygons(collector);
            }
            return null;
        }

        @Override
        public Void visit(Point point) {
            if (point.isEmpty() == false) {
                // points are a special "shape" case: for queries we need to quantize since the Lucene
                // tessellator doesn't do anything with them.
                // todo this is a sandy lucene experience so we should investigate fixing this upstream
                double quantizedLat = GeoEncodingUtils.decodeLatitude(GeoEncodingUtils.encodeLatitude(point.getLat()));
                double quantizedLon = GeoEncodingUtils.decodeLongitude(GeoEncodingUtils.encodeLongitude(point.getLon()));
                geometries.add(new org.apache.lucene.geo.Point(quantizedLat, quantizedLon));
            }
            return null;

        }

        @Override
        public Void visit(com.colasoft.opensearch.geometry.Polygon polygon) {
            if (polygon.isEmpty() == false) {
                List<com.colasoft.opensearch.geometry.Polygon> collector = new ArrayList<>();
                GeoPolygonDecomposer.decomposePolygon(polygon, true, collector);
                collectPolygons(collector);
            }
            return null;
        }

        @Override
        public Void visit(Rectangle r) {
            if (r.isEmpty() == false) {
                geometries.add(GeoShapeUtils.toLuceneRectangle(r));
            }
            return null;
        }

        private void collectLines(List<com.colasoft.opensearch.geometry.Line> geometryLines) {
            for (Line line : geometryLines) {
                geometries.add(GeoShapeUtils.toLuceneLine(line));
            }
        }

        private void collectPolygons(List<com.colasoft.opensearch.geometry.Polygon> geometryPolygons) {
            for (Polygon polygon : geometryPolygons) {
                geometries.add(GeoShapeUtils.toLucenePolygon(polygon));
            }
        }
    }
}
