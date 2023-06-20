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

package com.colasoft.opensearch.cluster.node;

public class DiscoveryNodeRoleGenerator {

    public static DiscoveryNodeRole createDynamicRole(String roleName) {
        return new DiscoveryNodeRole.DynamicRole(roleName, roleName, false);
    }
}
