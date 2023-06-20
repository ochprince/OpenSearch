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

import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.support.master.AcknowledgedRequest;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Objects;

/**
 * Request to delete a search pipeline
 *
 * @opensearch.internal
 */
public class DeleteSearchPipelineRequest extends AcknowledgedRequest<DeleteSearchPipelineRequest> {
    private String id;

    public DeleteSearchPipelineRequest(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public DeleteSearchPipelineRequest() {}

    public DeleteSearchPipelineRequest(StreamInput in) throws IOException {
        super(in);
        id = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
