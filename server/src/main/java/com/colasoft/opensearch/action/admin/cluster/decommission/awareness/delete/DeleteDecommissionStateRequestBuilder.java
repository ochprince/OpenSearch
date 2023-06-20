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

import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Builder for Delete decommission request.
 *
 * @opensearch.internal
 */
public class DeleteDecommissionStateRequestBuilder extends ClusterManagerNodeOperationRequestBuilder<
    DeleteDecommissionStateRequest,
    DeleteDecommissionStateResponse,
    DeleteDecommissionStateRequestBuilder> {

    public DeleteDecommissionStateRequestBuilder(OpenSearchClient client, DeleteDecommissionStateAction action) {
        super(client, action, new DeleteDecommissionStateRequest());
    }
}
