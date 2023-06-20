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

package com.colasoft.opensearch.geo.tests.common;

import com.colasoft.opensearch.geo.search.aggregations.bucket.geogrid.InternalGeoGrid;
import com.colasoft.opensearch.geo.search.aggregations.metrics.InternalGeoBounds;

public class AggregationInspectionHelper {

    public static boolean hasValue(InternalGeoBounds agg) {
        return (agg.topLeft() == null && agg.bottomRight() == null) == false;
    }

    public static boolean hasValue(InternalGeoGrid<?> agg) {
        return agg.getBuckets().stream().anyMatch(bucket -> bucket.getDocCount() > 0);
    }
}
