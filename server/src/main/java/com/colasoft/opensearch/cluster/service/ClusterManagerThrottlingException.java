/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.cluster.service;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.common.io.stream.StreamInput;

import java.io.IOException;

/**
 * Exception raised from cluster manager node due to task throttling.
 */
public class ClusterManagerThrottlingException extends OpenSearchException {

    public ClusterManagerThrottlingException(String msg, Object... args) {
        super(msg, args);
    }

    public ClusterManagerThrottlingException(StreamInput in) throws IOException {
        super(in);
    }
}
