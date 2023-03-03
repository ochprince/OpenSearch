/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.service;

import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.threadpool.ThreadPool;

/**
 * Main Cluster Manager Node Service
 *
 * @opensearch.internal
 */
public class ClusterManagerService extends MasterService {
    public ClusterManagerService(Settings settings, ClusterSettings clusterSettings, ThreadPool threadPool) {
        super(settings, clusterSettings, threadPool);
    }
}
