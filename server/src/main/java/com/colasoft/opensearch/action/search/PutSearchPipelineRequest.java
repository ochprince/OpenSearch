/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.support.master.AcknowledgedRequest;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.core.xcontent.MediaType;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * Request to put a search pipeline
 *
 * @opensearch.internal
 */
public class PutSearchPipelineRequest extends AcknowledgedRequest<PutSearchPipelineRequest> implements ToXContentObject {
    private String id;
    private BytesReference source;
    private XContentType xContentType;

    public PutSearchPipelineRequest(String id, BytesReference source, MediaType mediaType) {
        this.id = Objects.requireNonNull(id);
        this.source = Objects.requireNonNull(source);
        if (mediaType instanceof XContentType == false) {
            throw new IllegalArgumentException(
                PutSearchPipelineRequest.class.getSimpleName() + " found unsupported media type [" + mediaType.getClass().getName() + "]"
            );
        }
        this.xContentType = XContentType.fromMediaType(Objects.requireNonNull(mediaType));
    }

    public PutSearchPipelineRequest(StreamInput in) throws IOException {
        super(in);
        id = in.readString();
        source = in.readBytesReference();
        xContentType = in.readEnum(XContentType.class);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getId() {
        return id;
    }

    public BytesReference getSource() {
        return source;
    }

    public XContentType getXContentType() {
        return xContentType;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(id);
        out.writeBytesReference(source);
        out.writeEnum(xContentType);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (source != null) {
            builder.rawValue(source.streamInput(), xContentType);
        } else {
            builder.startObject().endObject();
        }
        return builder;
    }

}
