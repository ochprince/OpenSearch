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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.client.slm;

import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.Objects;

public class GetSnapshotLifecycleStatsResponse implements ToXContentObject {

    private final SnapshotLifecycleStats stats;

    public GetSnapshotLifecycleStatsResponse(SnapshotLifecycleStats stats) {
        this.stats = stats;
    }

    public SnapshotLifecycleStats getStats() {
        return this.stats;
    }

    public static GetSnapshotLifecycleStatsResponse fromXContent(XContentParser parser) throws IOException {
        return new GetSnapshotLifecycleStatsResponse(SnapshotLifecycleStats.parse(parser));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return stats.toXContent(builder, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetSnapshotLifecycleStatsResponse other = (GetSnapshotLifecycleStatsResponse) o;
        return Objects.equals(this.stats, other.stats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.stats);
    }
}
