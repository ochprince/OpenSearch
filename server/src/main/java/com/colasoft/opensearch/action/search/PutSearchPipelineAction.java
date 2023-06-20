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

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;

/**
 * Action type to put a new search pipeline
 *
 * @opensearch.internal
 */
public class PutSearchPipelineAction extends ActionType<AcknowledgedResponse> {
    public static final PutSearchPipelineAction INSTANCE = new PutSearchPipelineAction();
    public static final String NAME = "cluster:admin/search/pipeline/put";

    public PutSearchPipelineAction() {
        super(NAME, AcknowledgedResponse::new);
    }
}
