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

package org.opensearch.index.fielddata;

import org.opensearch.common.geo.GeoPoint;

import java.io.IOException;

final class SingletonMultiGeoPointValues extends MultiGeoPointValues {

    private final GeoPointValues in;

    SingletonMultiGeoPointValues(GeoPointValues in) {
        this.in = in;
    }

    @Override
    public boolean advanceExact(int doc) throws IOException {
        return in.advanceExact(doc);
    }

    @Override
    public int docValueCount() {
        return 1;
    }

    @Override
    public GeoPoint nextValue() {
        return in.geoPointValue();
    }

    GeoPointValues getGeoPointValues() {
        return in;
    }
}
