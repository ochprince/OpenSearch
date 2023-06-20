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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.index.shard;

import org.apache.lucene.index.IndexCommit;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.ClusterStateUpdateTask;
import com.colasoft.opensearch.cluster.metadata.IndexMetadata;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.cluster.metadata.RepositoryMetadata;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.component.AbstractLifecycleComponent;
import com.colasoft.opensearch.index.mapper.MapperService;
import com.colasoft.opensearch.index.snapshots.IndexShardSnapshotStatus;
import com.colasoft.opensearch.index.store.Store;
import com.colasoft.opensearch.repositories.IndexId;
import com.colasoft.opensearch.repositories.IndexMetaDataGenerations;
import com.colasoft.opensearch.repositories.Repository;
import com.colasoft.opensearch.repositories.RepositoryData;
import com.colasoft.opensearch.repositories.RepositoryShardId;
import com.colasoft.opensearch.repositories.ShardGenerations;
import com.colasoft.opensearch.snapshots.SnapshotId;
import com.colasoft.opensearch.snapshots.SnapshotInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static com.colasoft.opensearch.repositories.RepositoryData.EMPTY_REPO_GEN;

/** A dummy repository for testing which just needs restore overridden */
public abstract class RestoreOnlyRepository extends AbstractLifecycleComponent implements Repository {
    private final String indexName;

    public RestoreOnlyRepository(String indexName) {
        this.indexName = indexName;
    }

    @Override
    protected void doStart() {}

    @Override
    protected void doStop() {}

    @Override
    protected void doClose() {}

    @Override
    public RepositoryMetadata getMetadata() {
        return null;
    }

    @Override
    public SnapshotInfo getSnapshotInfo(SnapshotId snapshotId) {
        return null;
    }

    @Override
    public Metadata getSnapshotGlobalMetadata(SnapshotId snapshotId) {
        return null;
    }

    @Override
    public IndexMetadata getSnapshotIndexMetaData(RepositoryData repositoryData, SnapshotId snapshotId, IndexId index) {
        return null;
    }

    @Override
    public void getRepositoryData(ActionListener<RepositoryData> listener) {
        final IndexId indexId = new IndexId(indexName, "blah");
        listener.onResponse(
            new RepositoryData(
                EMPTY_REPO_GEN,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonMap(indexId, emptyList()),
                ShardGenerations.EMPTY,
                IndexMetaDataGenerations.EMPTY
            )
        );
    }

    @Override
    public void initializeSnapshot(SnapshotId snapshotId, List<IndexId> indices, Metadata metadata) {}

    @Override
    public void finalizeSnapshot(
        ShardGenerations shardGenerations,
        long repositoryStateId,
        Metadata clusterMetadata,
        SnapshotInfo snapshotInfo,
        Version repositoryMetaVersion,
        Function<ClusterState, ClusterState> stateTransformer,
        ActionListener<RepositoryData> listener
    ) {
        listener.onResponse(null);
    }

    @Override
    public void deleteSnapshots(
        Collection<SnapshotId> snapshotIds,
        long repositoryStateId,
        Version repositoryMetaVersion,
        ActionListener<RepositoryData> listener
    ) {
        listener.onResponse(null);
    }

    @Override
    public long getSnapshotThrottleTimeInNanos() {
        return 0;
    }

    @Override
    public long getRestoreThrottleTimeInNanos() {
        return 0;
    }

    @Override
    public String startVerification() {
        return null;
    }

    @Override
    public void endVerification(String verificationToken) {}

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public void snapshotShard(
        Store store,
        MapperService mapperService,
        SnapshotId snapshotId,
        IndexId indexId,
        IndexCommit snapshotIndexCommit,
        String shardStateIdentifier,
        IndexShardSnapshotStatus snapshotStatus,
        Version repositoryMetaVersion,
        Map<String, Object> userMetadata,
        ActionListener<String> listener
    ) {}

    @Override
    public IndexShardSnapshotStatus getShardSnapshotStatus(SnapshotId snapshotId, IndexId indexId, ShardId shardId) {
        return null;
    }

    @Override
    public void verify(String verificationToken, DiscoveryNode localNode) {}

    @Override
    public void updateState(final ClusterState state) {}

    @Override
    public void executeConsistentStateUpdate(
        Function<RepositoryData, ClusterStateUpdateTask> createUpdateTask,
        String source,
        Consumer<Exception> onFailure
    ) {
        throw new UnsupportedOperationException("Unsupported for restore-only repository");
    }

    @Override
    public void cloneShardSnapshot(
        SnapshotId source,
        SnapshotId target,
        RepositoryShardId repositoryShardId,
        String shardGeneration,
        ActionListener<String> listener
    ) {
        throw new UnsupportedOperationException("Unsupported for restore-only repository");
    }
}
