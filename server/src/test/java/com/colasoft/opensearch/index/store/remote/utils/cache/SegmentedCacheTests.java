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

package com.colasoft.opensearch.index.store.remote.utils.cache;

public class SegmentedCacheTests extends RefCountedCacheTestCase {
    public SegmentedCacheTests() {
        super(
            SegmentedCache.<String, Long>builder().capacity(CAPACITY).weigher(value -> value).listener(n -> {}).concurrencyLevel(1).build()
        );
    }
}
