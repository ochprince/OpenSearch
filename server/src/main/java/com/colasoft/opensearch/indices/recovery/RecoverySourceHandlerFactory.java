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

import com.colasoft.opensearch.index.shard.IndexShard;

/**
 * Factory that supplies {@link RecoverySourceHandler}.
 *
 * @opensearch.internal
 */
public class RecoverySourceHandlerFactory {

    public static RecoverySourceHandler create(
        IndexShard shard,
        RecoveryTargetHandler recoveryTarget,
        StartRecoveryRequest request,
        RecoverySettings recoverySettings
    ) {
        boolean isReplicaRecoveryWithRemoteTranslog = request.isPrimaryRelocation() == false && shard.isRemoteTranslogEnabled();
        if (isReplicaRecoveryWithRemoteTranslog) {
            return new RemoteStorePeerRecoverySourceHandler(
                shard,
                recoveryTarget,
                shard.getThreadPool(),
                request,
                Math.toIntExact(recoverySettings.getChunkSize().getBytes()),
                recoverySettings.getMaxConcurrentFileChunks(),
                recoverySettings.getMaxConcurrentOperations()
            );
        } else {
            return new LocalStorePeerRecoverySourceHandler(
                shard,
                recoveryTarget,
                shard.getThreadPool(),
                request,
                Math.toIntExact(recoverySettings.getChunkSize().getBytes()),
                recoverySettings.getMaxConcurrentFileChunks(),
                recoverySettings.getMaxConcurrentOperations()
            );
        }
    }
}
