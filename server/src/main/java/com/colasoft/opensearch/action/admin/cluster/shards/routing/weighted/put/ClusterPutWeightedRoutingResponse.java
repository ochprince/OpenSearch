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

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.put;

import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * Response from updating weights for weighted round-robin search routing policy.
 *
 * @opensearch.internal
 */
public class ClusterPutWeightedRoutingResponse extends AcknowledgedResponse {
    public ClusterPutWeightedRoutingResponse(boolean acknowledged) {
        super(acknowledged);
    }

    public ClusterPutWeightedRoutingResponse(StreamInput in) throws IOException {
        super(in);
    }
}
