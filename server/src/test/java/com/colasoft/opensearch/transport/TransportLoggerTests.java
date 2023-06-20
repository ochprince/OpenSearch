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

package com.colasoft.opensearch.transport;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsAction;
import com.colasoft.opensearch.action.admin.cluster.stats.ClusterStatsRequest;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.MockLogAppender;
import com.colasoft.opensearch.test.junit.annotations.TestLogging;

import java.io.IOException;

import static org.mockito.Mockito.mock;

@TestLogging(value = "com.colasoft.opensearch.transport.TransportLogger:trace", reason = "to ensure we log network events on TRACE level")
public class TransportLoggerTests extends OpenSearchTestCase {
    public void testLoggingHandler() throws Exception {
        try (MockLogAppender appender = MockLogAppender.createForLoggers(LogManager.getLogger(TransportLogger.class))) {
            final String writePattern = ".*\\[length: \\d+"
                + ", request id: \\d+"
                + ", type: request"
                + ", version: .*"
                + ", header size: \\d+B"
                + ", action: cluster:monitor/stats]"
                + " WRITE: \\d+B";
            final MockLogAppender.LoggingExpectation writeExpectation = new MockLogAppender.PatternSeenEventExpectation(
                "hot threads request",
                TransportLogger.class.getCanonicalName(),
                Level.TRACE,
                writePattern
            );

            final String readPattern = ".*\\[length: \\d+"
                + ", request id: \\d+"
                + ", type: request"
                + ", version: .*"
                + ", header size: \\d+B"
                + ", action: cluster:monitor/stats]"
                + " READ: \\d+B";

            final MockLogAppender.LoggingExpectation readExpectation = new MockLogAppender.PatternSeenEventExpectation(
                "cluster monitor request",
                TransportLogger.class.getCanonicalName(),
                Level.TRACE,
                readPattern
            );

            appender.addExpectation(writeExpectation);
            appender.addExpectation(readExpectation);
            BytesReference bytesReference = buildRequest();
            TransportLogger.logInboundMessage(mock(TcpChannel.class), bytesReference.slice(6, bytesReference.length() - 6));
            TransportLogger.logOutboundMessage(mock(TcpChannel.class), bytesReference);
            appender.assertAllExpectationsMatched();
        }
    }

    private BytesReference buildRequest() throws IOException {
        boolean compress = randomBoolean();
        try (BytesStreamOutput bytesStreamOutput = new BytesStreamOutput()) {
            OutboundMessage.Request request = new OutboundMessage.Request(
                new ThreadContext(Settings.EMPTY),
                new String[0],
                new ClusterStatsRequest(),
                Version.CURRENT,
                ClusterStatsAction.NAME,
                randomInt(30),
                false,
                compress
            );
            return request.serialize(bytesStreamOutput);
        }
    }
}
