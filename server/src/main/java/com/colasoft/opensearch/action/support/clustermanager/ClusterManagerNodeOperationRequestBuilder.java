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

package com.colasoft.opensearch.action.support.clustermanager;

import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.ActionRequestBuilder;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.client.OpenSearchClient;
import com.colasoft.opensearch.common.unit.TimeValue;

/**
 * Base request builder for cluster-manager node operations
 *
 * @opensearch.internal
 */
public abstract class ClusterManagerNodeOperationRequestBuilder<
    Request extends ClusterManagerNodeRequest<Request>,
    Response extends ActionResponse,
    RequestBuilder extends ClusterManagerNodeOperationRequestBuilder<Request, Response, RequestBuilder>> extends ActionRequestBuilder<
        Request,
        Response> {

    protected ClusterManagerNodeOperationRequestBuilder(OpenSearchClient client, ActionType<Response> action, Request request) {
        super(client, action, request);
    }

    /**
     * Sets the cluster-manager node timeout in case the cluster-manager has not yet been discovered.
     */
    @SuppressWarnings("unchecked")
    public final RequestBuilder setClusterManagerNodeTimeout(TimeValue timeout) {
        request.clusterManagerNodeTimeout(timeout);
        return (RequestBuilder) this;
    }

    /**
     * Sets the cluster-manager node timeout in case the cluster-manager has not yet been discovered.
     *
     * @deprecated As of 2.1, because supporting inclusive language, replaced by {@link #setClusterManagerNodeTimeout(TimeValue)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public final RequestBuilder setMasterNodeTimeout(TimeValue timeout) {
        return setClusterManagerNodeTimeout(timeout);
    }

    /**
     * Sets the cluster-manager node timeout in case the cluster-manager has not yet been discovered.
     */
    @SuppressWarnings("unchecked")
    public final RequestBuilder setClusterManagerNodeTimeout(String timeout) {
        request.clusterManagerNodeTimeout(timeout);
        return (RequestBuilder) this;
    }

    /**
     * Sets the cluster-manager node timeout in case the cluster-manager has not yet been discovered.
     *
     * @deprecated As of 2.1, because supporting inclusive language, replaced by {@link #setClusterManagerNodeTimeout(String)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public final RequestBuilder setMasterNodeTimeout(String timeout) {
        return setClusterManagerNodeTimeout(timeout);
    }
}
