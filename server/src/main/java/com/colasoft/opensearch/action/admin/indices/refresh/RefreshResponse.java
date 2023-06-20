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

package com.colasoft.opensearch.action.admin.indices.refresh;

import com.colasoft.opensearch.action.support.DefaultShardOperationFailedException;
import com.colasoft.opensearch.action.support.broadcast.BroadcastResponse;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.core.xcontent.ConstructingObjectParser;
import com.colasoft.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The response of a refresh action.
 *
 * @opensearch.internal
 */
public class RefreshResponse extends BroadcastResponse {

    private static final ConstructingObjectParser<RefreshResponse, Void> PARSER = new ConstructingObjectParser<>("refresh", true, arg -> {
        BroadcastResponse response = (BroadcastResponse) arg[0];
        return new RefreshResponse(
            response.getTotalShards(),
            response.getSuccessfulShards(),
            response.getFailedShards(),
            Arrays.asList(response.getShardFailures())
        );
    });

    static {
        declareBroadcastFields(PARSER);
    }

    RefreshResponse(StreamInput in) throws IOException {
        super(in);
    }

    RefreshResponse(int totalShards, int successfulShards, int failedShards, List<DefaultShardOperationFailedException> shardFailures) {
        super(totalShards, successfulShards, failedShards, shardFailures);
    }

    public static RefreshResponse fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }
}
