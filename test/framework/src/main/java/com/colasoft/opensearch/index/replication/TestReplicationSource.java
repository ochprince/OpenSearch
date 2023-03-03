/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.index.replication;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.index.store.Store;
import com.colasoft.opensearch.index.store.StoreFileMetadata;
import com.colasoft.opensearch.indices.replication.CheckpointInfoResponse;
import com.colasoft.opensearch.indices.replication.GetSegmentFilesResponse;
import com.colasoft.opensearch.indices.replication.SegmentReplicationSource;
import com.colasoft.opensearch.indices.replication.checkpoint.ReplicationCheckpoint;

import java.util.List;

/**
 * This class is used by unit tests implementing SegmentReplicationSource
 */
public abstract class TestReplicationSource implements SegmentReplicationSource {

    @Override
    public abstract void getCheckpointMetadata(
        long replicationId,
        ReplicationCheckpoint checkpoint,
        ActionListener<CheckpointInfoResponse> listener
    );

    @Override
    public abstract void getSegmentFiles(
        long replicationId,
        ReplicationCheckpoint checkpoint,
        List<StoreFileMetadata> filesToFetch,
        Store store,
        ActionListener<GetSegmentFilesResponse> listener
    );

    @Override
    public String getDescription() {
        return "This is a test description";
    }
}
