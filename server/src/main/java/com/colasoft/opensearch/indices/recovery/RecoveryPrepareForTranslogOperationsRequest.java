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

package com.colasoft.opensearch.indices.recovery;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.index.shard.ShardId;

import java.io.IOException;

/**
 * Request to prepare cluster for translog operations
 *
 * @opensearch.internal
 */
class RecoveryPrepareForTranslogOperationsRequest extends RecoveryTransportRequest {

    private final long recoveryId;
    private final ShardId shardId;
    private final int totalTranslogOps;

    RecoveryPrepareForTranslogOperationsRequest(long recoveryId, long requestSeqNo, ShardId shardId, int totalTranslogOps) {
        super(requestSeqNo);
        this.recoveryId = recoveryId;
        this.shardId = shardId;
        this.totalTranslogOps = totalTranslogOps;
    }

    RecoveryPrepareForTranslogOperationsRequest(StreamInput in) throws IOException {
        super(in);
        recoveryId = in.readLong();
        shardId = new ShardId(in);
        totalTranslogOps = in.readVInt();
        if (in.getVersion().before(LegacyESVersion.V_7_4_0)) {
            in.readBoolean(); // was fileBasedRecovery
        }
    }

    public long recoveryId() {
        return this.recoveryId;
    }

    public ShardId shardId() {
        return shardId;
    }

    public int totalTranslogOps() {
        return totalTranslogOps;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(recoveryId);
        shardId.writeTo(out);
        out.writeVInt(totalTranslogOps);
        if (out.getVersion().before(LegacyESVersion.V_7_4_0)) {
            out.writeBoolean(true); // was fileBasedRecovery
        }
    }
}
