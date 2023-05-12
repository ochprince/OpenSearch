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

package com.colasoft.opensearch.action.admin.cluster.repositories.delete;

import com.colasoft.opensearch.action.support.master.AcknowledgedRequestBuilder;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.client.OpenSearchClient;

/**
 * Builder for unregister repository request
 *
 * @opensearch.internal
 */
public class DeleteRepositoryRequestBuilder extends AcknowledgedRequestBuilder<
    DeleteRepositoryRequest,
    AcknowledgedResponse,
    DeleteRepositoryRequestBuilder> {

    /**
     * Constructs unregister repository request builder
     */
    public DeleteRepositoryRequestBuilder(OpenSearchClient client, DeleteRepositoryAction action) {
        super(client, action, new DeleteRepositoryRequest());
    }

    /**
     * Constructs unregister repository request builder with specified repository name
     */
    public DeleteRepositoryRequestBuilder(OpenSearchClient client, DeleteRepositoryAction action, String name) {
        super(client, action, new DeleteRepositoryRequest(name));
    }

    /**
     * Sets the repository name
     *
     * @param name the repository name
     */
    public DeleteRepositoryRequestBuilder setName(String name) {
        request.name(name);
        return this;
    }
}
