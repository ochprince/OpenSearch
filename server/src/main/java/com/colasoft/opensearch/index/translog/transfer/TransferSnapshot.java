/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.index.translog.transfer;

import com.colasoft.opensearch.index.translog.transfer.FileSnapshot.TransferFileSnapshot;
import com.colasoft.opensearch.index.translog.transfer.FileSnapshot.CheckpointFileSnapshot;
import com.colasoft.opensearch.index.translog.transfer.FileSnapshot.TranslogFileSnapshot;

import java.util.Set;

/**
 * The snapshot of the generational translog and checkpoint files and it's corresponding metadata that is transferred
 * to the {@link TransferService}
 *
 * @opensearch.internal
 */
public interface TransferSnapshot {

    /**
     * The snapshot of the checkpoint generational files
     * @return the set of {@link CheckpointFileSnapshot}
     */
    Set<TransferFileSnapshot> getCheckpointFileSnapshots();

    /**
     * The snapshot of the translog generational files
     * @return the set of {@link TranslogFileSnapshot}
     */
    Set<TransferFileSnapshot> getTranslogFileSnapshots();

    /**
     * The translog transfer metadata of this {@link TransferSnapshot}
     * @return the translog transfer metadata
     */
    TranslogTransferMetadata getTranslogTransferMetadata();
}
