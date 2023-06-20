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

package com.colasoft.opensearch.index.store.remote.filecache;

import com.colasoft.opensearch.common.breaker.CircuitBreaker;
import com.colasoft.opensearch.common.cache.RemovalReason;
import com.colasoft.opensearch.index.store.remote.utils.cache.SegmentedCache;
import com.colasoft.opensearch.index.store.remote.file.OnDemandBlockSnapshotIndexInput;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.colasoft.opensearch.ExceptionsHelper.catchAsRuntimeException;

/**
 * File Cache (FC) is introduced to solve the problem that the local disk cannot hold
 * the entire dataset on remote store. It maintains a node level view of index files with priorities,
 * caching only those index files needed by queries. The file with the lowest priority
 * (Least Recently Used) in the FC is replaced first.
 *
 * <p>The two main interfaces of FC are put and get. When a new file index input is added
 * to the file cache, the file will be added at cache head, which means it has the highest
 * priority.
 * <p> The get function does not add file to cache, but it promotes the priority
 * of a given file (since it makes it the most recently used).
 *
 * <p>Once file cache reaches its capacity, it starts evictions. Eviction removes the file
 * items from cache tail and triggers a callback to clean up the file from disk. The
 * cleanup process also includes closing file’s descriptor.
 *
 * @opensearch.internal
 */
public class FileCacheFactory {

    public static FileCache createConcurrentLRUFileCache(long capacity, CircuitBreaker circuitBreaker) {
        return createFileCache(createDefaultBuilder().capacity(capacity).build(), circuitBreaker);
    }

    public static FileCache createConcurrentLRUFileCache(long capacity, int concurrencyLevel, CircuitBreaker circuitBreaker) {
        return createFileCache(createDefaultBuilder().capacity(capacity).concurrencyLevel(concurrencyLevel).build(), circuitBreaker);
    }

    private static FileCache createFileCache(SegmentedCache<Path, CachedIndexInput> segmentedCache, CircuitBreaker circuitBreaker) {
        /*
         * Since OnDemandBlockSnapshotIndexInput.Builder.DEFAULT_BLOCK_SIZE is not overridden then it will be upper bound for max IndexInput
         * size on disk. A single IndexInput size should always be more than a single segment in segmented cache. A FileCache capacity might
         * be defined with large capacity (> IndexInput block size) but due to segmentation and concurrency factor, that capacity is
         * distributed equally across segments.
         */
        if (segmentedCache.getPerSegmentCapacity() <= OnDemandBlockSnapshotIndexInput.Builder.DEFAULT_BLOCK_SIZE) {
            throw new IllegalStateException("FileSystem Cache per segment capacity is less than single IndexInput default block size");
        }
        return new FileCache(segmentedCache, circuitBreaker);
    }

    private static SegmentedCache.Builder<Path, CachedIndexInput> createDefaultBuilder() {
        return SegmentedCache.<Path, CachedIndexInput>builder()
            // use length in bytes as the weight of the file item
            .weigher(CachedIndexInput::length)
            .listener((removalNotification) -> {
                RemovalReason removalReason = removalNotification.getRemovalReason();
                CachedIndexInput value = removalNotification.getValue();
                Path key = removalNotification.getKey();
                if (removalReason != RemovalReason.REPLACED) {
                    catchAsRuntimeException(value::close);
                    catchAsRuntimeException(() -> Files.deleteIfExists(key));
                }
            });
    }

}
