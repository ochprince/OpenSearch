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
package org.opensearch.cluster.coordination;

import org.opensearch.OpenSearchException;
import org.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * Thrown when failing to publish a cluster state. See {@link ClusterStatePublisher} for more details.
 */
public class FailedToCommitClusterStateException extends OpenSearchException {

    public FailedToCommitClusterStateException(StreamInput in) throws IOException {
        super(in);
    }

    public FailedToCommitClusterStateException(String msg, Object... args) {
        super(msg, args);
    }

    public FailedToCommitClusterStateException(String msg, Throwable cause, Object... args) {
        super(msg, cause, args);
    }
}
