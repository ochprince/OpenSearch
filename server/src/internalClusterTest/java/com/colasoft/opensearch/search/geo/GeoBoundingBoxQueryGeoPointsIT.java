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

package com.colasoft.opensearch.search.geo;

import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

public class GeoBoundingBoxQueryGeoPointsIT extends AbstractGeoBoundingBoxQueryIT {

    @Override
    public XContentBuilder addGeoMapping(XContentBuilder parentMapping) throws IOException {
        return parentMapping.startObject("location").field("type", "geo_point").endObject();
    }
}
