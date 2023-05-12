/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.geo;

import org.junit.Before;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

/** Tests geo_distance queries on geo_point field types */
public class GeoDistanceQueryGeoPointsIT extends AbstractGeoDistanceIT {

    @Before
    public void setupTestIndex() throws IOException {
        indexSetup();
    }

    @Override
    public XContentBuilder addGeoMapping(XContentBuilder parentMapping) throws IOException {
        return parentMapping.startObject("location").field("type", "geo_point").endObject();
    }
}
