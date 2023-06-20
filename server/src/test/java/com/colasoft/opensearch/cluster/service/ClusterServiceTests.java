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

package com.colasoft.opensearch.cluster.service;

import org.junit.After;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.threadpool.TestThreadPool;

import static org.hamcrest.Matchers.equalTo;

public class ClusterServiceTests extends OpenSearchTestCase {
    private final TestThreadPool threadPool = new TestThreadPool(ClusterServiceTests.class.getName());

    @After
    public void terminateThreadPool() {
        terminate(threadPool);
    }

    public void testDeprecatedGetMasterServiceBWC() {
        try (
            ClusterService clusterService = new ClusterService(
                Settings.EMPTY,
                new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS),
                threadPool
            )
        ) {
            MasterService masterService = clusterService.getMasterService();
            ClusterManagerService clusterManagerService = clusterService.getClusterManagerService();
            assertThat(masterService, equalTo(clusterManagerService));
        }
    }
}
