/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.decommission.awareness.put;

import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;
import com.colasoft.opensearch.cluster.decommission.DecommissionAttribute;
import com.colasoft.opensearch.common.unit.TimeValue;

/**
 * Register decommission request builder
 *
 * @opensearch.internal
 */
public class DecommissionRequestBuilder extends ClusterManagerNodeOperationRequestBuilder<
    DecommissionRequest,
    DecommissionResponse,
    DecommissionRequestBuilder> {

    public DecommissionRequestBuilder(OpenSearchClient client, ActionType<DecommissionResponse> action, DecommissionRequest request) {
        super(client, action, request);
    }

    /**
     * @param decommissionAttribute decommission attribute
     * @return current object
     */
    public DecommissionRequestBuilder setDecommissionedAttribute(DecommissionAttribute decommissionAttribute) {
        request.setDecommissionAttribute(decommissionAttribute);
        return this;
    }

    public DecommissionRequestBuilder setDelayTimeOut(TimeValue delayTimeOut) {
        request.setDelayTimeout(delayTimeOut);
        return this;
    }

    public DecommissionRequestBuilder setNoDelay(boolean noDelay) {
        request.setNoDelay(noDelay);
        return this;
    }

    /**
     * Sets request id for decommission request
     *
     * @param requestID for decommission request
     * @return current object
     */
    public DecommissionRequestBuilder requestID(String requestID) {
        request.setRequestID(requestID);
        return this;
    }
}
