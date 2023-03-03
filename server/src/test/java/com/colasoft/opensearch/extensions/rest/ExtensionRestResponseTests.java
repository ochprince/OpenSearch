/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.extensions.rest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;

import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.rest.RestRequest.Method;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import static com.colasoft.opensearch.rest.BytesRestResponse.TEXT_CONTENT_TYPE;
import static com.colasoft.opensearch.rest.RestStatus.ACCEPTED;
import static com.colasoft.opensearch.rest.RestStatus.OK;

public class ExtensionRestResponseTests extends OpenSearchTestCase {

    private static final String OCTET_CONTENT_TYPE = "application/octet-stream";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private String testText;
    private byte[] testBytes;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testText = "plain text";
        testBytes = new byte[] { 1, 2 };
    }

    private ExtensionRestRequest generateTestRequest() {
        ExtensionRestRequest request = new ExtensionRestRequest(
            Method.GET,
            "/foo",
            Collections.emptyMap(),
            null,
            new BytesArray("Text Content"),
            null
        );
        // consume params "foo" and "bar"
        request.param("foo");
        request.param("bar");
        // consume content
        request.content();
        return request;
    }

    public void testConstructorWithBuilder() throws IOException {
        XContentBuilder builder = XContentBuilder.builder(XContentType.JSON.xContent());
        builder.startObject();
        builder.field("status", ACCEPTED);
        builder.endObject();
        ExtensionRestRequest request = generateTestRequest();
        ExtensionRestResponse response = new ExtensionRestResponse(request, OK, builder);

        assertEquals(OK, response.status());
        assertEquals(JSON_CONTENT_TYPE, response.contentType());
        assertEquals("{\"status\":\"ACCEPTED\"}", response.content().utf8ToString());
        for (String param : response.getConsumedParams()) {
            assertTrue(request.consumedParams().contains(param));
        }
        assertTrue(request.isContentConsumed());
    }

    public void testConstructorWithPlainText() {
        ExtensionRestRequest request = generateTestRequest();
        ExtensionRestResponse response = new ExtensionRestResponse(request, OK, testText);

        assertEquals(OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        assertEquals(testText, response.content().utf8ToString());
        for (String param : response.getConsumedParams()) {
            assertTrue(request.consumedParams().contains(param));
        }
        assertTrue(request.isContentConsumed());
    }

    public void testConstructorWithText() {
        ExtensionRestRequest request = generateTestRequest();
        ExtensionRestResponse response = new ExtensionRestResponse(request, OK, TEXT_CONTENT_TYPE, testText);

        assertEquals(OK, response.status());
        assertEquals(TEXT_CONTENT_TYPE, response.contentType());
        assertEquals(testText, response.content().utf8ToString());

        for (String param : response.getConsumedParams()) {
            assertTrue(request.consumedParams().contains(param));
        }
        assertTrue(request.isContentConsumed());
    }

    public void testConstructorWithByteArray() {
        ExtensionRestRequest request = generateTestRequest();
        ExtensionRestResponse response = new ExtensionRestResponse(request, OK, OCTET_CONTENT_TYPE, testBytes);

        assertEquals(OK, response.status());
        assertEquals(OCTET_CONTENT_TYPE, response.contentType());
        assertArrayEquals(testBytes, BytesReference.toBytes(response.content()));
        for (String param : response.getConsumedParams()) {
            assertTrue(request.consumedParams().contains(param));
        }
        assertTrue(request.isContentConsumed());
    }

    public void testConstructorWithBytesReference() {
        ExtensionRestRequest request = generateTestRequest();
        ExtensionRestResponse response = new ExtensionRestResponse(
            request,
            OK,
            OCTET_CONTENT_TYPE,
            BytesReference.fromByteBuffer(ByteBuffer.wrap(testBytes, 0, 2))
        );

        assertEquals(OK, response.status());
        assertEquals(OCTET_CONTENT_TYPE, response.contentType());
        assertArrayEquals(testBytes, BytesReference.toBytes(response.content()));
        for (String param : response.getConsumedParams()) {
            assertTrue(request.consumedParams().contains(param));
        }
        assertTrue(request.isContentConsumed());
    }
}
