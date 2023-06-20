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

import com.colasoft.opensearch.action.ActionType;

/**
 *  Delete decommission state action.
 *
 * @opensearch.internal
 */
public class DeleteDecommissionStateAction extends ActionType<DeleteDecommissionStateResponse> {
    public static final DeleteDecommissionStateAction INSTANCE = new DeleteDecommissionStateAction();
    public static final String NAME = "cluster:admin/decommission/awareness/delete";

    private DeleteDecommissionStateAction() {
        super(NAME, DeleteDecommissionStateResponse::new);
    }
}
