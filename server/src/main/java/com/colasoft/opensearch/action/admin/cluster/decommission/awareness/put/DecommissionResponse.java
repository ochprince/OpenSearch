/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put;

import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.ToXContentObject;

import java.io.IOException;

/**
 * Response for decommission request
 *
 * @opensearch.internal
 */
public class DecommissionResponse extends AcknowledgedResponse implements ToXContentObject {

    public DecommissionResponse(StreamInput in) throws IOException {
        super(in);
    }

    public DecommissionResponse(boolean acknowledged) {
        super(acknowledged);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }
}
