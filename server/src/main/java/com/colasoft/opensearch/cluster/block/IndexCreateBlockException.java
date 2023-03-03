/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.block;

import com.colasoft.opensearch.common.io.stream.StreamInput;

import java.io.IOException;
import java.util.Set;

/**
 * Internal exception on obtaining an index create block
 *
 * @opensearch.internal
 */
public class IndexCreateBlockException extends ClusterBlockException {

    public IndexCreateBlockException(Set<ClusterBlock> globalLevelBlocks) {
        super(globalLevelBlocks);
    }

    public IndexCreateBlockException(StreamInput in) throws IOException {
        super(in);
    }
}
