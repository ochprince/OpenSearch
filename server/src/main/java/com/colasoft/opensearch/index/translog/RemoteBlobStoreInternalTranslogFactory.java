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

package com.colasoft.opensearch.index.translog;

import com.colasoft.opensearch.repositories.RepositoriesService;
import com.colasoft.opensearch.repositories.Repository;
import com.colasoft.opensearch.repositories.RepositoryMissingException;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreRepository;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Translog Factory for the remotefs  translog {@link RemoteFsTranslog}
 *
 * @opensearch.internal
 */
public class RemoteBlobStoreInternalTranslogFactory implements TranslogFactory {

    private final Repository repository;

    private final ThreadPool threadPool;

    public RemoteBlobStoreInternalTranslogFactory(
        Supplier<RepositoriesService> repositoriesServiceSupplier,
        ThreadPool threadPool,
        String repositoryName
    ) {
        Repository repository;
        try {
            repository = repositoriesServiceSupplier.get().repository(repositoryName);
        } catch (RepositoryMissingException ex) {
            throw new IllegalArgumentException("Repository should be created before creating index with remote_store enabled setting", ex);
        }
        this.repository = repository;
        this.threadPool = threadPool;
    }

    @Override
    public Translog newTranslog(
        TranslogConfig config,
        String translogUUID,
        TranslogDeletionPolicy deletionPolicy,
        LongSupplier globalCheckpointSupplier,
        LongSupplier primaryTermSupplier,
        LongConsumer persistedSequenceNumberConsumer,
        BooleanSupplier primaryModeSupplier
    ) throws IOException {

        assert repository instanceof BlobStoreRepository : "repository should be instance of BlobStoreRepository";
        BlobStoreRepository blobStoreRepository = ((BlobStoreRepository) repository);
        return new RemoteFsTranslog(
            config,
            translogUUID,
            deletionPolicy,
            globalCheckpointSupplier,
            primaryTermSupplier,
            persistedSequenceNumberConsumer,
            blobStoreRepository,
            threadPool,
            primaryModeSupplier
        );
    }
}
