/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.get;

import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeReadRequest;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

import static com.colasoft.opensearch.action.ValidateActions.addValidationError;

/**
 * Get Decommissioned attribute request
 *
 * @opensearch.internal
 */
public class GetDecommissionStateRequest extends ClusterManagerNodeReadRequest<GetDecommissionStateRequest> {

    private String attributeName;

    public GetDecommissionStateRequest() {}

    /**
     * Constructs a new get decommission state request with given attribute name
     *
     * @param attributeName name of the attribute
     */
    public GetDecommissionStateRequest(String attributeName) {
        this.attributeName = attributeName;
    }

    public GetDecommissionStateRequest(StreamInput in) throws IOException {
        super(in);
        attributeName = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(attributeName);
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;
        if (attributeName == null || Strings.isEmpty(attributeName)) {
            validationException = addValidationError("attribute name is missing", validationException);
        }
        return validationException;
    }

    /**
     * Sets attribute name
     *
     * @param attributeName attribute name
     * @return this request
     */
    public GetDecommissionStateRequest attributeName(String attributeName) {
        this.attributeName = attributeName;
        return this;
    }

    /**
     * Returns attribute name
     *
     * @return attributeName name of attribute
     */
    public String attributeName() {
        return this.attributeName;
    }
}
