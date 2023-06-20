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
import com.colasoft.opensearch.search.backpressure.trackers.CpuUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.ElapsedTimeTracker;
import com.colasoft.opensearch.search.backpressure.trackers.HeapUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.TaskResourceUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.TaskResourceUsageTrackerType;
import com.colasoft.opensearch.test.AbstractWireSerializingTestCase;

import java.util.Map;

public class SearchShardTaskStatsTests extends AbstractWireSerializingTestCase<SearchShardTaskStats> {
    @Override
    protected Writeable.Reader<SearchShardTaskStats> instanceReader() {
        return SearchShardTaskStats::new;
    }

    @Override
    protected SearchShardTaskStats createTestInstance() {
        return randomInstance();
    }

    public static SearchShardTaskStats randomInstance() {
        Map<TaskResourceUsageTrackerType, TaskResourceUsageTracker.Stats> resourceUsageTrackerStats = Map.of(
            TaskResourceUsageTrackerType.CPU_USAGE_TRACKER,
            new CpuUsageTracker.Stats(randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong()),
            TaskResourceUsageTrackerType.HEAP_USAGE_TRACKER,
            new HeapUsageTracker.Stats(randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong()),
            TaskResourceUsageTrackerType.ELAPSED_TIME_TRACKER,
            new ElapsedTimeTracker.Stats(randomNonNegativeLong(), randomNonNegativeLong(), randomNonNegativeLong())
        );

        return new SearchShardTaskStats(randomNonNegativeLong(), randomNonNegativeLong(), resourceUsageTrackerStats);
    }
}
