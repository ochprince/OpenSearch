/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.StepListener;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.transport.RemoteClusterService;

import java.util.Set;
import java.util.function.BiFunction;

/**
 * Helper class for common search functions
 */
public class SearchUtils {

    public SearchUtils() {}

    /**
     * Get connection lookup listener for list of clusters passed
     */
    public static ActionListener<BiFunction<String, String, DiscoveryNode>> getConnectionLookupListener(
        RemoteClusterService remoteClusterService,
        ClusterState state,
        Set<String> clusters
    ) {
        final StepListener<BiFunction<String, String, DiscoveryNode>> lookupListener = new StepListener<>();

        if (clusters.isEmpty()) {
            lookupListener.onResponse((cluster, nodeId) -> state.getNodes().get(nodeId));
        } else {
            remoteClusterService.collectNodes(clusters, lookupListener);
        }
        return lookupListener;
    }
}
