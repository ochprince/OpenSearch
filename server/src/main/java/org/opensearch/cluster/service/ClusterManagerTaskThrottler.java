/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.cluster.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.Version;
import org.opensearch.cluster.ClusterStateTaskExecutor;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * This class does throttling on task submission to cluster manager node, it uses throttling key defined in various executors
 * as key for throttling. Throttling will be performed over task executor's class level, different task types have different executors class.
 *
 * Set specific setting to for setting the threshold of throttling of particular task type.
 * e.g : Set "cluster_manager.throttling.thresholds.put_mapping" to set throttling limit of "put mapping" tasks,
 *       Set it to default value(-1) to disable the throttling for this task type.
 */
public class ClusterManagerTaskThrottler implements TaskBatcherListener {
    private static final Logger logger = LogManager.getLogger(ClusterManagerTaskThrottler.class);
    public static final ThrottlingKey DEFAULT_THROTTLING_KEY = new ThrottlingKey("default-task-key", false);

    public static final Setting<Settings> THRESHOLD_SETTINGS = Setting.groupSetting(
        "cluster_manager.throttling.thresholds.",
        Setting.Property.Dynamic,
        Setting.Property.NodeScope
    );

    protected Map<String, ThrottlingKey> THROTTLING_TASK_KEYS = new ConcurrentHashMap<>();

    private final int MIN_THRESHOLD_VALUE = -1; // Disabled throttling
    private final ClusterManagerTaskThrottlerListener clusterManagerTaskThrottlerListener;

    private final ConcurrentMap<String, Long> tasksCount;
    private final ConcurrentMap<String, Long> tasksThreshold;
    private final Supplier<Version> minNodeVersionSupplier;

    public ClusterManagerTaskThrottler(
        final ClusterSettings clusterSettings,
        final Supplier<Version> minNodeVersionSupplier,
        final ClusterManagerTaskThrottlerListener clusterManagerTaskThrottlerListener
    ) {
        clusterSettings.addSettingsUpdateConsumer(THRESHOLD_SETTINGS, this::updateSetting, this::validateSetting);
        this.minNodeVersionSupplier = minNodeVersionSupplier;
        this.clusterManagerTaskThrottlerListener = clusterManagerTaskThrottlerListener;
        tasksCount = new ConcurrentHashMap<>(128); // setting initial capacity so each task will land in different segment
        tasksThreshold = new ConcurrentHashMap<>(128); // setting initial capacity so each task will land in different segment
    }

    /**
     * To configure a new task for throttling,
     * * Register task to cluster service with task key,
     * * override getClusterManagerThrottlingKey method with above task key in task executor.
     * * Verify that throttled tasks would be retried from data nodes
     *
     * Added retry mechanism in TransportClusterManagerNodeAction, so it would be retried for customer generated tasks.
     *
     * If tasks are not getting retried then we can register with false flag, so user won't be able to configure threshold limits for it.
     */
    protected ThrottlingKey registerClusterManagerTask(String taskKey, boolean throttlingEnabled) {
        ThrottlingKey throttlingKey = new ThrottlingKey(taskKey, throttlingEnabled);
        if (THROTTLING_TASK_KEYS.containsKey(taskKey)) {
            throw new IllegalArgumentException("There is already a Throttling key registered with same name: " + taskKey);
        }
        THROTTLING_TASK_KEYS.put(taskKey, throttlingKey);
        return throttlingKey;
    }

    /**
     * Class to store the throttling key for the tasks of cluster manager
     */
    public static class ThrottlingKey {
        private String taskThrottlingKey;
        private boolean throttlingEnabled;

        /**
         * Class for throttling key of tasks
         *
         * @param taskThrottlingKey - throttling key for task
         * @param throttlingEnabled - if throttling is enabled or not i.e. data node is performing retry over throttling exception or not.
         */
        private ThrottlingKey(String taskThrottlingKey, boolean throttlingEnabled) {
            this.taskThrottlingKey = taskThrottlingKey;
            this.throttlingEnabled = throttlingEnabled;
        }

        public String getTaskThrottlingKey() {
            return taskThrottlingKey;
        }

        public boolean isThrottlingEnabled() {
            return throttlingEnabled;
        }
    }

    void validateSetting(final Settings settings) {
        if (minNodeVersionSupplier.get().compareTo(Version.V_2_4_0) < 0) {
            throw new IllegalArgumentException("All the nodes in cluster should be on version later than or equal to 2.4.0");
        }
        Map<String, Settings> groups = settings.getAsGroups();
        for (String key : groups.keySet()) {
            if (!THROTTLING_TASK_KEYS.containsKey(key)) {
                throw new IllegalArgumentException("Cluster manager task throttling is not configured for given task type: " + key);
            }
            if (!THROTTLING_TASK_KEYS.get(key).isThrottlingEnabled()) {
                throw new IllegalArgumentException("Throttling is not enabled for given task type: " + key);
            }
            int threshold = groups.get(key).getAsInt("value", MIN_THRESHOLD_VALUE);
            if (threshold < MIN_THRESHOLD_VALUE) {
                throw new IllegalArgumentException("Provide positive integer for limit or -1 for disabling throttling");
            }
        }
    }

    void updateSetting(final Settings settings) {
        Map<String, Settings> groups = settings.getAsGroups();
        for (String key : groups.keySet()) {
            updateLimit(key, groups.get(key).getAsInt("value", MIN_THRESHOLD_VALUE));
        }
    }

    void updateLimit(final String taskKey, final int limit) {
        assert limit >= MIN_THRESHOLD_VALUE;
        if (limit == MIN_THRESHOLD_VALUE) {
            tasksThreshold.remove(taskKey);
        } else {
            tasksThreshold.put(taskKey, (long) limit);
        }
    }

    Long getThrottlingLimit(final String taskKey) {
        return tasksThreshold.get(taskKey);
    }

    @Override
    public void onBeginSubmit(List<? extends TaskBatcher.BatchedTask> tasks) {
        ThrottlingKey clusterManagerThrottlingKey = ((ClusterStateTaskExecutor<Object>) tasks.get(0).batchingKey)
            .getClusterManagerThrottlingKey();
        tasksCount.putIfAbsent(clusterManagerThrottlingKey.getTaskThrottlingKey(), 0L);
        tasksCount.computeIfPresent(clusterManagerThrottlingKey.getTaskThrottlingKey(), (key, count) -> {
            int size = tasks.size();
            if (clusterManagerThrottlingKey.isThrottlingEnabled()) {
                Long threshold = tasksThreshold.get(clusterManagerThrottlingKey.getTaskThrottlingKey());
                if (threshold != null && (count + size > threshold)) {
                    clusterManagerTaskThrottlerListener.onThrottle(clusterManagerThrottlingKey.getTaskThrottlingKey(), size);
                    logger.warn(
                        "Throwing Throttling Exception for [{}]. Trying to add [{}] tasks to queue, limit is set to [{}]",
                        clusterManagerThrottlingKey.getTaskThrottlingKey(),
                        tasks.size(),
                        threshold
                    );
                    throw new ClusterManagerThrottlingException(
                        "Throttling Exception : Limit exceeded for " + clusterManagerThrottlingKey.getTaskThrottlingKey()
                    );
                }
            }
            return count + size;
        });
    }

    @Override
    public void onSubmitFailure(List<? extends TaskBatcher.BatchedTask> tasks) {
        reduceTaskCount(tasks);
    }

    /**
     * Tasks will be removed from the queue before processing, so here we will reduce the count of tasks.
     *
     * @param tasks list of tasks which will be executed.
     */
    @Override
    public void onBeginProcessing(List<? extends TaskBatcher.BatchedTask> tasks) {
        reduceTaskCount(tasks);
    }

    @Override
    public void onTimeout(List<? extends TaskBatcher.BatchedTask> tasks) {
        reduceTaskCount(tasks);
    }

    private void reduceTaskCount(List<? extends TaskBatcher.BatchedTask> tasks) {
        String masterTaskKey = ((ClusterStateTaskExecutor<Object>) tasks.get(0).batchingKey).getClusterManagerThrottlingKey()
            .getTaskThrottlingKey();
        tasksCount.computeIfPresent(masterTaskKey, (key, count) -> count - tasks.size());
    }
}
