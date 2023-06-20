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

package com.colasoft.opensearch.cluster.decommission;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.rest.RestStatus;

import java.io.IOException;

/**
 * This exception is thrown whenever a failure occurs in decommission request @{@link DecommissionService}
 *
 * @opensearch.internal
 */

public class DecommissioningFailedException extends OpenSearchException {

    private final DecommissionAttribute decommissionAttribute;

    public DecommissioningFailedException(DecommissionAttribute decommissionAttribute, String msg) {
        this(decommissionAttribute, msg, null);
    }

    public DecommissioningFailedException(DecommissionAttribute decommissionAttribute, String msg, Throwable cause) {
        super("[" + (decommissionAttribute == null ? "_na" : decommissionAttribute.toString()) + "] " + msg, cause);
        this.decommissionAttribute = decommissionAttribute;
    }

    public DecommissioningFailedException(StreamInput in) throws IOException {
        super(in);
        decommissionAttribute = new DecommissionAttribute(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        decommissionAttribute.writeTo(out);
    }

    /**
     * Returns decommission attribute
     *
     * @return decommission attribute
     */
    public DecommissionAttribute decommissionAttribute() {
        return decommissionAttribute;
    }

    @Override
    public RestStatus status() {
        return RestStatus.BAD_REQUEST;
    }
}
