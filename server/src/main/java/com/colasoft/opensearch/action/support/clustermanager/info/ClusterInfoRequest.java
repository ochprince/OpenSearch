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

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.action.support.clustermanager.info;

import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.IndicesRequest;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeReadRequest;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

/**
 * Transport request for cluster information
 *
 * @opensearch.internal
 */
public abstract class ClusterInfoRequest<Request extends ClusterInfoRequest<Request>> extends ClusterManagerNodeReadRequest<Request>
    implements
        IndicesRequest.Replaceable {

    private String[] indices = Strings.EMPTY_ARRAY;

    private IndicesOptions indicesOptions = IndicesOptions.strictExpandOpen();

    public ClusterInfoRequest() {}

    public ClusterInfoRequest(StreamInput in) throws IOException {
        super(in);
        indices = in.readStringArray();
        if (in.getVersion().before(Version.V_2_0_0)) {
            in.readStringArray();
        }
        indicesOptions = IndicesOptions.readIndicesOptions(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeStringArray(indices);
        if (out.getVersion().before(Version.V_2_0_0)) {
            out.writeStringArray(Strings.EMPTY_ARRAY);
        }
        indicesOptions.writeIndicesOptions(out);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Request indices(String... indices) {
        this.indices = indices;
        return (Request) this;
    }

    @SuppressWarnings("unchecked")
    public Request indicesOptions(IndicesOptions indicesOptions) {
        this.indicesOptions = indicesOptions;
        return (Request) this;
    }

    @Override
    public String[] indices() {
        return indices;
    }

    @Override
    public IndicesOptions indicesOptions() {
        return indicesOptions;
    }

    @Override
    public boolean includeDataStreams() {
        return true;
    }
}
