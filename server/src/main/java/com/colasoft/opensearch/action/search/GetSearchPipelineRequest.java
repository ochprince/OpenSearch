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
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeReadRequest;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Objects;

/**
 * Request to get search pipelines
 *
 * @opensearch.internal
 */
public class GetSearchPipelineRequest extends ClusterManagerNodeReadRequest<GetSearchPipelineRequest> {
    private final String[] ids;

    public GetSearchPipelineRequest(String... ids) {
        this.ids = Objects.requireNonNull(ids);
    }

    public GetSearchPipelineRequest() {
        ids = Strings.EMPTY_ARRAY;
    }

    public GetSearchPipelineRequest(StreamInput in) throws IOException {
        super(in);
        ids = in.readStringArray();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(ids);
    }

    public String[] getIds() {
        return ids;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
