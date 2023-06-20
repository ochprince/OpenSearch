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

package com.colasoft.opensearch.search.backpressure.stats;

import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.search.backpressure.settings.SearchBackpressureMode;
import com.colasoft.opensearch.test.AbstractWireSerializingTestCase;

public class SearchBackpressureStatsTests extends AbstractWireSerializingTestCase<SearchBackpressureStats> {
    @Override
    protected Writeable.Reader<SearchBackpressureStats> instanceReader() {
        return SearchBackpressureStats::new;
    }

    @Override
    protected SearchBackpressureStats createTestInstance() {
        return randomInstance();
    }

    public static SearchBackpressureStats randomInstance() {
        return new SearchBackpressureStats(
            SearchTaskStatsTests.randomInstance(),
            SearchShardTaskStatsTests.randomInstance(),
            randomFrom(SearchBackpressureMode.DISABLED, SearchBackpressureMode.MONITOR_ONLY, SearchBackpressureMode.ENFORCED)
        );
    }
}
