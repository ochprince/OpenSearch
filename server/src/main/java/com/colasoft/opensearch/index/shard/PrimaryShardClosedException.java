/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.colasoft.opensearch.index.shard;

import java.io.IOException;

import com.colasoft.opensearch.common.io.stream.StreamInput;

/**
 * Exception to indicate failures are caused due to the closure of the primary
 * shard.
 *
 * @opensearch.internal
 */
public class PrimaryShardClosedException extends IndexShardClosedException {
    public PrimaryShardClosedException(ShardId shardId) {
        super(shardId, "Primary closed");
    }

    public PrimaryShardClosedException(StreamInput in) throws IOException {
        super(in);
    }
}
