/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete;

import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * Response from deleting weights for weighted round-robin search routing policy.
 *
 * @opensearch.internal
 */
public class ClusterDeleteWeightedRoutingResponse extends AcknowledgedResponse {

    ClusterDeleteWeightedRoutingResponse(StreamInput in) throws IOException {
        super(in);
    }

    public ClusterDeleteWeightedRoutingResponse(boolean acknowledged) {
        super(acknowledged);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);

    }
}
