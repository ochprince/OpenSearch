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

package com.colasoft.opensearch.extensions;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportResponseHandler;

/**
 * Response handler for {@link UpdateSettingsRequest}
 *
 * @opensearch.internal
 */
public class UpdateSettingsResponseHandler implements TransportResponseHandler<AcknowledgedResponse> {
    private static final Logger logger = LogManager.getLogger(UpdateSettingsResponseHandler.class);

    @Override
    public AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    public void handleResponse(AcknowledgedResponse response) {
        logger.info("response {}", response.getStatus());
        if (!response.getStatus()) {
            handleException(new TransportException("Request was not completed successfully"));
        }
    }

    @Override
    public void handleException(TransportException exp) {
        logger.error(new ParameterizedMessage("UpdateSettingsRequest failed"), exp);
    }

    @Override
    public String executor() {
        return ThreadPool.Names.GENERIC;
    }
}
