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

package com.colasoft.opensearch.action.fieldcaps;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.IndicesRequest;
import com.colasoft.opensearch.action.OriginalIndices;
import com.colasoft.opensearch.action.support.IndicesOptions;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.index.query.QueryBuilder;
import com.colasoft.opensearch.index.shard.ShardId;

import java.io.IOException;
import java.util.Objects;

/**
 * Transport Request for Retrieving Field Capabilities for an Index
 *
 * @opensearch.internal
 */
public class FieldCapabilitiesIndexRequest extends ActionRequest implements IndicesRequest {

    public static final IndicesOptions INDICES_OPTIONS = IndicesOptions.strictSingleIndexNoExpandForbidClosed();

    private final String index;
    private final String[] fields;
    private final OriginalIndices originalIndices;
    private final QueryBuilder indexFilter;
    private final long nowInMillis;

    private ShardId shardId;

    // For serialization
    FieldCapabilitiesIndexRequest(StreamInput in) throws IOException {
        super(in);
        shardId = in.readOptionalWriteable(ShardId::new);
        index = in.readOptionalString();
        fields = in.readStringArray();
        originalIndices = OriginalIndices.readOriginalIndices(in);
        indexFilter = in.getVersion().onOrAfter(LegacyESVersion.V_7_9_0) ? in.readOptionalNamedWriteable(QueryBuilder.class) : null;
        nowInMillis = in.getVersion().onOrAfter(LegacyESVersion.V_7_9_0) ? in.readLong() : 0L;
    }

    FieldCapabilitiesIndexRequest(
        String[] fields,
        String index,
        OriginalIndices originalIndices,
        QueryBuilder indexFilter,
        long nowInMillis
    ) {
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException("specified fields can't be null or empty");
        }
        this.index = Objects.requireNonNull(index);
        this.fields = fields;
        this.originalIndices = originalIndices;
        this.indexFilter = indexFilter;
        this.nowInMillis = nowInMillis;
    }

    public String[] fields() {
        return fields;
    }

    @Override
    public String[] indices() {
        return originalIndices.indices();
    }

    @Override
    public IndicesOptions indicesOptions() {
        return originalIndices.indicesOptions();
    }

    public String index() {
        return index;
    }

    public QueryBuilder indexFilter() {
        return indexFilter;
    }

    public ShardId shardId() {
        return shardId;
    }

    public long nowInMillis() {
        return nowInMillis;
    }

    FieldCapabilitiesIndexRequest shardId(ShardId shardId) {
        this.shardId = shardId;
        return this;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalWriteable(shardId);
        out.writeOptionalString(index);
        out.writeStringArray(fields);
        OriginalIndices.writeOriginalIndices(originalIndices, out);
        if (out.getVersion().onOrAfter(LegacyESVersion.V_7_9_0)) {
            out.writeOptionalNamedWriteable(indexFilter);
            out.writeLong(nowInMillis);
        }
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }
}
