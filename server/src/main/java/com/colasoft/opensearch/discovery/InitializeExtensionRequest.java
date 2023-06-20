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

package com.colasoft.opensearch.discovery;

import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.extensions.DiscoveryExtensionNode;
import com.colasoft.opensearch.transport.TransportRequest;

import java.io.IOException;
import java.util.Objects;

/**
 * InitializeExtensionRequest to intialize plugin
 *
 * @opensearch.internal
 */
public class InitializeExtensionRequest extends TransportRequest {
    private final DiscoveryNode sourceNode;
    private final DiscoveryExtensionNode extension;

    public InitializeExtensionRequest(DiscoveryNode sourceNode, DiscoveryExtensionNode extension) {
        this.sourceNode = sourceNode;
        this.extension = extension;
    }

    public InitializeExtensionRequest(StreamInput in) throws IOException {
        super(in);
        sourceNode = new DiscoveryNode(in);
        extension = new DiscoveryExtensionNode(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        sourceNode.writeTo(out);
        extension.writeTo(out);
    }

    public DiscoveryNode getSourceNode() {
        return sourceNode;
    }

    public DiscoveryExtensionNode getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return "InitializeExtensionsRequest{" + "sourceNode=" + sourceNode + ", extension=" + extension + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitializeExtensionRequest that = (InitializeExtensionRequest) o;
        return Objects.equals(sourceNode, that.sourceNode) && Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNode, extension);
    }
}
