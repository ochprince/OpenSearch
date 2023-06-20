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

package com.colasoft.opensearch.search.pipeline;

import com.colasoft.opensearch.action.search.SearchRequest;
import com.colasoft.opensearch.action.search.SearchResponse;

/**
 * Interface for a search pipeline processor that modifies a search response.
 */
public interface SearchResponseProcessor extends Processor {
    SearchResponse processResponse(SearchRequest request, SearchResponse response) throws Exception;
}
