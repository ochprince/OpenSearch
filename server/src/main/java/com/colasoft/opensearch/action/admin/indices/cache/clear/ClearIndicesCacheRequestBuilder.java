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

package com.colasoft.opensearch.action.admin.indices.cache.clear;

import com.colasoft.opensearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Transport request builder for clearing cache
 *
 * @opensearch.internal
 */
public class ClearIndicesCacheRequestBuilder extends BroadcastOperationRequestBuilder<
    ClearIndicesCacheRequest,
    ClearIndicesCacheResponse,
    ClearIndicesCacheRequestBuilder> {

    public ClearIndicesCacheRequestBuilder(OpenSearchClient client, ClearIndicesCacheAction action) {
        super(client, action, new ClearIndicesCacheRequest());
    }

    public ClearIndicesCacheRequestBuilder setQueryCache(boolean queryCache) {
        request.queryCache(queryCache);
        return this;
    }

    public ClearIndicesCacheRequestBuilder setRequestCache(boolean requestCache) {
        request.requestCache(requestCache);
        return this;
    }

    public ClearIndicesCacheRequestBuilder setFieldDataCache(boolean fieldDataCache) {
        request.fieldDataCache(fieldDataCache);
        return this;
    }

    public ClearIndicesCacheRequestBuilder setFields(String... fields) {
        request.fields(fields);
        return this;
    }

}
