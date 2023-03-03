/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.index.store;

import org.apache.lucene.store.Directory;
import com.colasoft.opensearch.common.blobstore.BlobContainer;
import com.colasoft.opensearch.common.blobstore.BlobPath;
import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.shard.ShardPath;
import com.colasoft.opensearch.plugins.IndexStorePlugin;
import com.colasoft.opensearch.repositories.RepositoriesService;
import com.colasoft.opensearch.repositories.Repository;
import com.colasoft.opensearch.repositories.RepositoryMissingException;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreRepository;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Factory for a remote store directory
 *
 * @opensearch.internal
 */
public class RemoteSegmentStoreDirectoryFactory implements IndexStorePlugin.RemoteDirectoryFactory {

    private final Supplier<RepositoriesService> repositoriesService;

    public RemoteSegmentStoreDirectoryFactory(Supplier<RepositoriesService> repositoriesService) {
        this.repositoriesService = repositoriesService;
    }

    @Override
    public Directory newDirectory(String repositoryName, IndexSettings indexSettings, ShardPath path) throws IOException {
        try (Repository repository = repositoriesService.get().repository(repositoryName)) {
            assert repository instanceof BlobStoreRepository : "repository should be instance of BlobStoreRepository";
            BlobPath commonBlobPath = ((BlobStoreRepository) repository).basePath();
            commonBlobPath = commonBlobPath.add(indexSettings.getIndex().getUUID())
                .add(String.valueOf(path.getShardId().getId()))
                .add("segments");

            RemoteDirectory dataDirectory = createRemoteDirectory(repository, commonBlobPath, "data");
            RemoteDirectory metadataDirectory = createRemoteDirectory(repository, commonBlobPath, "metadata");

            return new RemoteSegmentStoreDirectory(dataDirectory, metadataDirectory);
        } catch (RepositoryMissingException e) {
            throw new IllegalArgumentException("Repository should be created before creating index with remote_store enabled setting", e);
        }
    }

    private RemoteDirectory createRemoteDirectory(Repository repository, BlobPath commonBlobPath, String extention) {
        BlobPath extendedPath = commonBlobPath.add(extention);
        BlobContainer dataBlobContainer = ((BlobStoreRepository) repository).blobStore().blobContainer(extendedPath);
        return new RemoteDirectory(dataBlobContainer);
    }
}
