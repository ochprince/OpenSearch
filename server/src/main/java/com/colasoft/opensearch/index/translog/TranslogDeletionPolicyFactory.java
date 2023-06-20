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

import com.colasoft.opensearch.index.IndexSettings;
import com.colasoft.opensearch.index.seqno.RetentionLeases;

import java.util.function.Supplier;

/**
 * Factory to instantiate a translog deletion policy
 *
 * @opensearch.api
 */
@FunctionalInterface
public interface TranslogDeletionPolicyFactory {
    TranslogDeletionPolicy create(IndexSettings settings, Supplier<RetentionLeases> supplier);
}
