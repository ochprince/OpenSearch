/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.colasoft.opensearch.index.reindex.spi;

import java.util.Optional;
import org.apache.http.HttpRequestInterceptor;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.index.reindex.ReindexRequest;

public interface ReindexRestInterceptorProvider {
    /**
     * @param request Reindex request.
     * @param threadContext Current thread context.
     * @return HttpRequestInterceptor object.
     */
    Optional<HttpRequestInterceptor> getRestInterceptor(ReindexRequest request, ThreadContext threadContext);
}
