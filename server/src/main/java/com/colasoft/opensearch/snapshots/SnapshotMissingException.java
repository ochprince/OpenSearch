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
 *    http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.snapshots;

import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.rest.RestStatus;

import java.io.IOException;

/**
 * Thrown if requested snapshot doesn't exist
 *
 * @opensearch.internal
 */
public class SnapshotMissingException extends SnapshotException {

    public SnapshotMissingException(final String repositoryName, final SnapshotId snapshotId, final Throwable cause) {
        super(repositoryName, snapshotId, "is missing", cause);
    }

    public SnapshotMissingException(final String repositoryName, final String snapshotName) {
        super(repositoryName, snapshotName, "is missing");
    }

    public SnapshotMissingException(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    public RestStatus status() {
        return RestStatus.NOT_FOUND;
    }
}
