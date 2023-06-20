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

import com.colasoft.opensearch.common.collect.MapBuilder;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.search.backpressure.trackers.CpuUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.ElapsedTimeTracker;
import com.colasoft.opensearch.search.backpressure.trackers.HeapUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.TaskResourceUsageTracker;
import com.colasoft.opensearch.search.backpressure.trackers.TaskResourceUsageTrackerType;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Stats related to cancelled SearchShardTasks.
 */
public class SearchShardTaskStats implements ToXContentObject, Writeable {
    private final long cancellationCount;
    private final long limitReachedCount;
    private final Map<TaskResourceUsageTrackerType, TaskResourceUsageTracker.Stats> resourceUsageTrackerStats;

    public SearchShardTaskStats(
        long cancellationCount,
        long limitReachedCount,
        Map<TaskResourceUsageTrackerType, TaskResourceUsageTracker.Stats> resourceUsageTrackerStats
    ) {
        this.cancellationCount = cancellationCount;
        this.limitReachedCount = limitReachedCount;
        this.resourceUsageTrackerStats = resourceUsageTrackerStats;
    }

    public SearchShardTaskStats(StreamInput in) throws IOException {
        this.cancellationCount = in.readVLong();
        this.limitReachedCount = in.readVLong();

        MapBuilder<TaskResourceUsageTrackerType, TaskResourceUsageTracker.Stats> builder = new MapBuilder<>();
        builder.put(TaskResourceUsageTrackerType.CPU_USAGE_TRACKER, in.readOptionalWriteable(CpuUsageTracker.Stats::new));
        builder.put(TaskResourceUsageTrackerType.HEAP_USAGE_TRACKER, in.readOptionalWriteable(HeapUsageTracker.Stats::new));
        builder.put(TaskResourceUsageTrackerType.ELAPSED_TIME_TRACKER, in.readOptionalWriteable(ElapsedTimeTracker.Stats::new));
        this.resourceUsageTrackerStats = builder.immutableMap();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();

        builder.startObject("resource_tracker_stats");
        for (Map.Entry<TaskResourceUsageTrackerType, TaskResourceUsageTracker.Stats> entry : resourceUsageTrackerStats.entrySet()) {
            builder.field(entry.getKey().getName(), entry.getValue());
        }
        builder.endObject();

        builder.startObject("cancellation_stats")
            .field("cancellation_count", cancellationCount)
            .field("cancellation_limit_reached_count", limitReachedCount)
            .endObject();

        return builder.endObject();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVLong(cancellationCount);
        out.writeVLong(limitReachedCount);

        out.writeOptionalWriteable(resourceUsageTrackerStats.get(TaskResourceUsageTrackerType.CPU_USAGE_TRACKER));
        out.writeOptionalWriteable(resourceUsageTrackerStats.get(TaskResourceUsageTrackerType.HEAP_USAGE_TRACKER));
        out.writeOptionalWriteable(resourceUsageTrackerStats.get(TaskResourceUsageTrackerType.ELAPSED_TIME_TRACKER));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchShardTaskStats that = (SearchShardTaskStats) o;
        return cancellationCount == that.cancellationCount
            && limitReachedCount == that.limitReachedCount
            && resourceUsageTrackerStats.equals(that.resourceUsageTrackerStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cancellationCount, limitReachedCount, resourceUsageTrackerStats);
    }
}
