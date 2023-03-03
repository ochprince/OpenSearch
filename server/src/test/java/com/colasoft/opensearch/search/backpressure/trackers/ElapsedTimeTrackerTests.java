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

public class ElapsedTimeTrackerTests extends OpenSearchTestCase {

    private static final SearchBackpressureSettings mockSettings = new SearchBackpressureSettings(
        Settings.builder()
            .put(SearchShardTaskSettings.SETTING_ELAPSED_TIME_MILLIS_THRESHOLD.getKey(), 100) // 100 ms
            .put(SearchTaskSettings.SETTING_ELAPSED_TIME_MILLIS_THRESHOLD.getKey(), 150)   // 150 ms
            .build(),
        new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS)
    );

    public void testSearchTaskEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchTask.class, 1, 1, 0);
        ElapsedTimeTracker tracker = new ElapsedTimeTracker(
            mockSettings.getSearchTaskSettings()::getElapsedTimeNanosThreshold,
            () -> 150000000
        );

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertTrue(reason.isPresent());
        assertEquals(1, reason.get().getCancellationScore());
        assertEquals("elapsed time exceeded [150ms >= 150ms]", reason.get().getMessage());
    }

    public void testSearchShardTaskEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchShardTask.class, 1, 1, 0);
        ElapsedTimeTracker tracker = new ElapsedTimeTracker(
            mockSettings.getSearchShardTaskSettings()::getElapsedTimeNanosThreshold,
            () -> 200000000
        );

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertTrue(reason.isPresent());
        assertEquals(1, reason.get().getCancellationScore());
        assertEquals("elapsed time exceeded [200ms >= 100ms]", reason.get().getMessage());
    }

    public void testNotEligibleForCancellation() {
        Task task = createMockTaskWithResourceStats(SearchShardTask.class, 1, 1, 150000000);
        ElapsedTimeTracker tracker = new ElapsedTimeTracker(
            mockSettings.getSearchShardTaskSettings()::getElapsedTimeNanosThreshold,
            () -> 200000000
        );

        Optional<TaskCancellation.Reason> reason = tracker.checkAndMaybeGetCancellationReason(task);
        assertFalse(reason.isPresent());
    }
}
