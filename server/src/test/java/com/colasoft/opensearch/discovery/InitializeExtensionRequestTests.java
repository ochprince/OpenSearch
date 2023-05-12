/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package com.colasoft.opensearch.discovery;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.BytesStreamInput;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.transport.TransportAddress;
import com.colasoft.opensearch.extensions.DiscoveryExtensionNode;
import com.colasoft.opensearch.extensions.ExtensionDependency;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

public class InitializeExtensionRequestTests extends OpenSearchTestCase {

    public void testInitializeExtensionRequest() throws Exception {
        String expectedUniqueId = "test uniqueid";
        Version expectedVersion = Version.fromString("2.0.0");
        ExtensionDependency expectedDependency = new ExtensionDependency(expectedUniqueId, expectedVersion);
        DiscoveryExtensionNode expectedExtensionNode = new DiscoveryExtensionNode(
            "firstExtension",
            "uniqueid1",
            new TransportAddress(InetAddress.getByName("127.0.0.0"), 9300),
            new HashMap<>(),
            Version.CURRENT,
            Version.CURRENT,
            List.of(expectedDependency)
        );
        DiscoveryNode expectedSourceNode = new DiscoveryNode(
            "sourceNode",
            "uniqueid2",
            new TransportAddress(InetAddress.getByName("127.0.0.0"), 1000),
            new HashMap<>(),
            DiscoveryNodeRole.BUILT_IN_ROLES,
            Version.CURRENT
        );

        InitializeExtensionRequest initializeExtensionRequest = new InitializeExtensionRequest(expectedSourceNode, expectedExtensionNode);
        assertEquals(expectedExtensionNode, initializeExtensionRequest.getExtension());
        assertEquals(expectedSourceNode, initializeExtensionRequest.getSourceNode());

        try (BytesStreamOutput out = new BytesStreamOutput()) {
            initializeExtensionRequest.writeTo(out);
            out.flush();
            try (BytesStreamInput in = new BytesStreamInput(BytesReference.toBytes(out.bytes()))) {
                initializeExtensionRequest = new InitializeExtensionRequest(in);

                assertEquals(expectedExtensionNode, initializeExtensionRequest.getExtension());
                assertEquals(expectedSourceNode, initializeExtensionRequest.getSourceNode());
            }
        }
    }
}
