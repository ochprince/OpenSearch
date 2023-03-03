/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.coordination;

import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;

/**
 * Service to block the master node
 *
 * @opensearch.internal
 * @deprecated As of 2.2, because supporting inclusive language, replaced by {@link NoClusterManagerBlockService}
 */
@Deprecated
public class NoMasterBlockService extends NoClusterManagerBlockService {

    public NoMasterBlockService(Settings settings, ClusterSettings clusterSettings) {
        super(settings, clusterSettings);
    }

}
