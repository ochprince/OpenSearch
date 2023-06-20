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

package com.colasoft.opensearch.extensions;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.common.transport.TransportAddress;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;

public class DiscoveryExtensionNodeTests extends OpenSearchTestCase {

    public void testExtensionNode() throws UnknownHostException {
        DiscoveryExtensionNode extensionNode = new DiscoveryExtensionNode(
            "firstExtension",
            "extensionUniqueId1",
            new TransportAddress(InetAddress.getByName("127.0.0.0"), 9300),
            new HashMap<String, String>(),
            Version.CURRENT,
            // minimum compatible version
            Version.CURRENT,
            Collections.emptyList()
        );
        assertTrue(Version.CURRENT.onOrAfter(extensionNode.getMinimumCompatibleVersion()));
    }

    public void testIncompatibleExtensionNode() throws UnknownHostException {
        expectThrows(
            OpenSearchException.class,
            () -> new DiscoveryExtensionNode(
                "firstExtension",
                "extensionUniqueId1",
                new TransportAddress(InetAddress.getByName("127.0.0.0"), 9300),
                new HashMap<String, String>(),
                Version.fromString("3.0.0"),
                // minimum compatible version
                Version.fromString("3.99.0"),
                Collections.emptyList()
            )
        );
    }
}
