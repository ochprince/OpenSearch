/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.indices.replication;

import com.colasoft.opensearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Segment Replication stats information request builder.
 *
 * @opensearch.internal
 */
public class SegmentReplicationStatsRequestBuilder extends BroadcastOperationRequestBuilder<
    SegmentReplicationStatsRequest,
    SegmentReplicationStatsResponse,
    SegmentReplicationStatsRequestBuilder> {

    public SegmentReplicationStatsRequestBuilder(OpenSearchClient client, SegmentReplicationStatsAction action) {
        super(client, action, new SegmentReplicationStatsRequest());
    }

    public SegmentReplicationStatsRequestBuilder setDetailed(boolean detailed) {
        request.detailed(detailed);
        return this;
    }

    public SegmentReplicationStatsRequestBuilder setActiveOnly(boolean activeOnly) {
        request.activeOnly(activeOnly);
        return this;
    }

    public SegmentReplicationStatsRequestBuilder shards(String... indices) {
        request.shards(indices);
        return this;
    }

}
