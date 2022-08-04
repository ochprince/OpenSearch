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

package org.opensearch.cluster.ack;

import org.opensearch.common.unit.TimeValue;

/**
 * Identifies a cluster state update request with acknowledgement support
 *
 * @opensearch.internal
 */
public interface AckedRequest {

    /**
     * Returns the acknowledgement timeout
     */
    TimeValue ackTimeout();

    /**
     * Returns the timeout for the request to be completed on the cluster-manager node
     * @deprecated As of 2.2, because supporting inclusive language, replaced by {@link #clusterManagerNodeTimeout()}
     */
    @Deprecated
    default TimeValue masterNodeTimeout() {
        throw new UnsupportedOperationException("Must be overridden");
    }

    /**
     * Returns the timeout for the request to be completed on the cluster-manager node
     */
    // TODO: Remove default implementation after removing the deprecated masterNodeTimeout()
    default TimeValue clusterManagerNodeTimeout() {
        return masterNodeTimeout();
    }
}
