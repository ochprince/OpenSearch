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

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete;

import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * Request for deleting decommission request.
 *
 * @opensearch.internal
 */
public class DeleteDecommissionStateRequest extends ClusterManagerNodeRequest<DeleteDecommissionStateRequest> {

    public DeleteDecommissionStateRequest() {}

    public DeleteDecommissionStateRequest(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
