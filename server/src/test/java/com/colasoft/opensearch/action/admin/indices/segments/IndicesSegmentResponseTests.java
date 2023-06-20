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

package com.colasoft.opensearch.action.admin.indices.segments;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardRoutingState;
import com.colasoft.opensearch.cluster.routing.TestShardRouting;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.index.engine.Segment;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.util.Collections;

import static com.colasoft.opensearch.common.xcontent.XContentFactory.jsonBuilder;

public class IndicesSegmentResponseTests extends OpenSearchTestCase {

    public void testToXContentSerialiationWithSortedFields() throws Exception {
        ShardRouting shardRouting = TestShardRouting.newShardRouting("foo", 0, "node_id", true, ShardRoutingState.STARTED);
        Segment segment = new Segment("my");

        SortField sortField = new SortField("foo", SortField.Type.STRING);
        sortField.setMissingValue(SortField.STRING_LAST);
        segment.segmentSort = new Sort(sortField);

        ShardSegments shardSegments = new ShardSegments(shardRouting, Collections.singletonList(segment));
        IndicesSegmentResponse response = new IndicesSegmentResponse(
            new ShardSegments[] { shardSegments },
            1,
            1,
            0,
            Collections.emptyList()
        );
        try (XContentBuilder builder = jsonBuilder()) {
            response.toXContent(builder, ToXContent.EMPTY_PARAMS);
        }
    }
}
