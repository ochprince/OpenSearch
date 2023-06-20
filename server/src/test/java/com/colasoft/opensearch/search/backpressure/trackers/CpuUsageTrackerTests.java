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

package com.colasoft.opensearch.search.backpressure.trackers;

import com.colasoft.opensearch.action.search.SearchShardTask;
import com.colasoft.opensearch.action.search.SearchTask;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.search.backpressure.settings.SearchBackpressureSettings;
import com.colasoft.opensearch.search.backpressure.settings.SearchShardTaskSettings;
import com.colasoft.opensearch.search.backpressure.settings.SearchTaskSettings;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.tasks.TaskCancellation;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.util.Optional;

import static com.colasoft.opensearch.search.backpressure.SearchBackpressureTestHelpers.createMockTaskWithResourceStats;

public class CpuUsageTrackerTests extends OpenSearchTestCase {
    private static final SearchBackpressureSettings mockSettings = new SearchBackpressureSettings(
        Settings.builder()
            .put(SearchShardTaskSettings.SETTING_CPU_TIME_MILLIS_THRESHOLD.getKey(), 15)  // 15 ms
            .put(SearchTaskSettings.SETTING_CPU_TIME_MILLIS_THRESHOLD.getKey(), 25)   // 25 ms
            .build(),
        new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS)
    );

    public void testSearchTaskEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchTask.class, 100000000, 200);
        CpuUsageTracker tracker = new CpuUsageTracker(mockSettings.getSearchTaskSettings()::getCpuTimeNanosThreshold);

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertTrue(reason.isPresent());
        assertEquals(1, reason.get().getCancellationScore());
        assertEquals("cpu usage exceeded [100ms >= 25ms]", reason.get().getMessage());
    }

    public void testSearchShardTaskEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchShardTask.class, 200000000, 200);
        CpuUsageTracker tracker = new CpuUsageTracker(mockSettings.getSearchShardTaskSettings()::getCpuTimeNanosThreshold);

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertTrue(reason.isPresent());
        assertEquals(1, reason.get().getCancellationScore());
        assertEquals("cpu usage exceeded [200ms >= 15ms]", reason.get().getMessage());
    }

    public void testNotEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchShardTask.class, 5000000, 200);
        CpuUsageTracker tracker = new CpuUsageTracker(mockSettings.getSearchShardTaskSettings()::getCpuTimeNanosThreshold);

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertFalse(reason.isPresent());
    }
}
