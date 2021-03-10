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

package org.elasticsearch.action.admin.indices.forcemerge;

import org.opensearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractBroadcastResponseTestCase;
import org.opensearch.action.admin.indices.forcemerge.ForceMergeResponse;

import java.util.List;

public class ForceMergeResponseTests extends AbstractBroadcastResponseTestCase<ForceMergeResponse> {
    @Override
    protected ForceMergeResponse createTestInstance(int totalShards, int successfulShards, int failedShards,
                                                    List<DefaultShardOperationFailedException> failures) {
        return new ForceMergeResponse(totalShards, successfulShards, failedShards, failures);
    }

    @Override
    protected ForceMergeResponse doParseInstance(XContentParser parser) {
        return ForceMergeResponse.fromXContent(parser);
    }
}
