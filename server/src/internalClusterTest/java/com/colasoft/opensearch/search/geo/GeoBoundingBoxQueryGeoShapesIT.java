/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.geo;

import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

public class GeoBoundingBoxQueryGeoShapesIT extends AbstractGeoBoundingBoxQueryIT {

    @Override
    public XContentBuilder addGeoMapping(XContentBuilder parentMapping) throws IOException {
        parentMapping = parentMapping.startObject("location").field("type", "geo_shape");
        if (randomBoolean()) {
            parentMapping.field("strategy", "recursive");
        }
        return parentMapping.endObject();
    }
}
