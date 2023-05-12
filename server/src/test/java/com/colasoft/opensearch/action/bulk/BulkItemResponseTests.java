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

package com.colasoft.opensearch.action.bulk;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.ExceptionsHelper;
import com.colasoft.opensearch.action.DocWriteRequest;
import com.colasoft.opensearch.action.DocWriteResponse;
import com.colasoft.opensearch.action.bulk.BulkItemResponse.Failure;
import com.colasoft.opensearch.action.delete.DeleteResponseTests;
import com.colasoft.opensearch.action.index.IndexResponseTests;
import com.colasoft.opensearch.action.update.UpdateResponse;
import com.colasoft.opensearch.action.update.UpdateResponseTests;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.collect.Tuple;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;

import static com.colasoft.opensearch.OpenSearchExceptionTests.assertDeepEquals;
import static com.colasoft.opensearch.OpenSearchExceptionTests.randomExceptions;
import static org.hamcrest.Matchers.containsString;

public class BulkItemResponseTests extends OpenSearchTestCase {

    public void testFailureToString() {
        Failure failure = new Failure("index", "id", new RuntimeException("test"));
        String toString = failure.toString();
        assertThat(toString, containsString("\"type\":\"runtime_exception\""));
        assertThat(toString, containsString("\"reason\":\"test\""));
        assertThat(toString, containsString("\"status\":500"));
    }

    public void testToAndFromXContent() throws IOException {
        final XContentType xContentType = randomFrom(XContentType.values());

        for (DocWriteRequest.OpType opType : DocWriteRequest.OpType.values()) {
            int bulkItemId = randomIntBetween(0, 100);
            boolean humanReadable = randomBoolean();

            Tuple<? extends DocWriteResponse, ? extends DocWriteResponse> randomDocWriteResponses = null;
            if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                randomDocWriteResponses = IndexResponseTests.randomIndexResponse();
            } else if (opType == DocWriteRequest.OpType.DELETE) {
                randomDocWriteResponses = DeleteResponseTests.randomDeleteResponse();
            } else if (opType == DocWriteRequest.OpType.UPDATE) {
                randomDocWriteResponses = UpdateResponseTests.randomUpdateResponse(xContentType);
            } else {
                fail("Test does not support opType [" + opType + "]");
            }

            BulkItemResponse bulkItemResponse = new BulkItemResponse(bulkItemId, opType, randomDocWriteResponses.v1());
            BulkItemResponse expectedBulkItemResponse = new BulkItemResponse(bulkItemId, opType, randomDocWriteResponses.v2());
            BytesReference originalBytes = toShuffledXContent(bulkItemResponse, xContentType, ToXContent.EMPTY_PARAMS, humanReadable);

            BulkItemResponse parsedBulkItemResponse;
            try (XContentParser parser = createParser(xContentType.xContent(), originalBytes)) {
                assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
                parsedBulkItemResponse = BulkItemResponse.fromXContent(parser, bulkItemId);
                assertNull(parser.nextToken());
            }
            assertBulkItemResponse(expectedBulkItemResponse, parsedBulkItemResponse);
        }
    }

    public void testFailureToAndFromXContent() throws IOException {
        final XContentType xContentType = randomFrom(XContentType.values());

        int itemId = randomIntBetween(0, 100);
        String index = randomAlphaOfLength(5);
        String id = randomAlphaOfLength(5);
        DocWriteRequest.OpType opType = randomFrom(DocWriteRequest.OpType.values());

        final Tuple<Throwable, OpenSearchException> exceptions = randomExceptions();

        Exception bulkItemCause = (Exception) exceptions.v1();
        Failure bulkItemFailure = new Failure(index, id, bulkItemCause);
        BulkItemResponse bulkItemResponse = new BulkItemResponse(itemId, opType, bulkItemFailure);
        Failure expectedBulkItemFailure = new Failure(index, id, exceptions.v2(), ExceptionsHelper.status(bulkItemCause));
        BulkItemResponse expectedBulkItemResponse = new BulkItemResponse(itemId, opType, expectedBulkItemFailure);
        BytesReference originalBytes = toShuffledXContent(bulkItemResponse, xContentType, ToXContent.EMPTY_PARAMS, randomBoolean());

        // Shuffle the XContent fields
        if (randomBoolean()) {
            try (XContentParser parser = createParser(xContentType.xContent(), originalBytes)) {
                originalBytes = BytesReference.bytes(shuffleXContent(parser, randomBoolean()));
            }
        }

        BulkItemResponse parsedBulkItemResponse;
        try (XContentParser parser = createParser(xContentType.xContent(), originalBytes)) {
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            parsedBulkItemResponse = BulkItemResponse.fromXContent(parser, itemId);
            assertNull(parser.nextToken());
        }
        assertBulkItemResponse(expectedBulkItemResponse, parsedBulkItemResponse);
    }

    public static void assertBulkItemResponse(BulkItemResponse expected, BulkItemResponse actual) {
        assertEquals(expected.getItemId(), actual.getItemId());
        assertEquals(expected.getIndex(), actual.getIndex());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getOpType(), actual.getOpType());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.isFailed(), actual.isFailed());

        if (expected.isFailed()) {
            BulkItemResponse.Failure expectedFailure = expected.getFailure();
            BulkItemResponse.Failure actualFailure = actual.getFailure();

            assertEquals(expectedFailure.getIndex(), actualFailure.getIndex());
            assertEquals(expectedFailure.getId(), actualFailure.getId());
            assertEquals(expectedFailure.getMessage(), actualFailure.getMessage());
            assertEquals(expectedFailure.getStatus(), actualFailure.getStatus());

            assertDeepEquals((OpenSearchException) expectedFailure.getCause(), (OpenSearchException) actualFailure.getCause());
        } else {
            DocWriteResponse expectedDocResponse = expected.getResponse();
            DocWriteResponse actualDocResponse = expected.getResponse();

            IndexResponseTests.assertDocWriteResponse(expectedDocResponse, actualDocResponse);
            if (expected.getOpType() == DocWriteRequest.OpType.UPDATE) {
                assertEquals(((UpdateResponse) expectedDocResponse).getGetResult(), ((UpdateResponse) actualDocResponse).getGetResult());
            }
        }
    }
}
