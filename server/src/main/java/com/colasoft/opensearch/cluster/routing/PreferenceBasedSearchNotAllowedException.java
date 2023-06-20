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

package com.colasoft.opensearch.cluster.routing;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.rest.RestStatus;

import java.io.IOException;

/**
 * Thrown to disallow preference based search with strict weighted shard routing. See {@link WeightedRoutingService}
 * * for more details.
 *
 * @opensearch.internal
 */
public class PreferenceBasedSearchNotAllowedException extends OpenSearchException {

    public PreferenceBasedSearchNotAllowedException(StreamInput in) throws IOException {
        super(in);
    }

    public PreferenceBasedSearchNotAllowedException(String msg, Object... args) {
        super(msg, args);
    }

    @Override
    public RestStatus status() {
        return RestStatus.FORBIDDEN;
    }
}
