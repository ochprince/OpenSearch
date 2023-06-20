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

package com.colasoft.opensearch.indices.recovery;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.index.store.StoreFileMetadata;

/**
 * Writes a partial file chunk to the target store.
 *
 * @opensearch.internal
 */
@FunctionalInterface
public interface FileChunkWriter {

    void writeFileChunk(
        StoreFileMetadata fileMetadata,
        long position,
        BytesReference content,
        boolean lastChunk,
        int totalTranslogOps,
        ActionListener<Void> listener
    );

    default void cancel() {}
}
