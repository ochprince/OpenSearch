/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.action.admin.cluster.shards.routing.weighted.delete;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.OpenSearchGenerationException;
import com.colasoft.opensearch.OpenSearchParseException;
import com.colasoft.opensearch.action.ActionRequestValidationException;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import com.colasoft.opensearch.cluster.metadata.WeightedRoutingMetadata;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.core.xcontent.DeprecationHandler;
import com.colasoft.opensearch.core.xcontent.MediaType;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;

/**
 * Request to delete weights for weighted round-robin shard routing policy.
 *
 * @opensearch.internal
 */
public class ClusterDeleteWeightedRoutingRequest extends ClusterManagerNodeRequest<ClusterDeleteWeightedRoutingRequest> {
    private static final Logger logger = LogManager.getLogger(ClusterDeleteWeightedRoutingRequest.class);

    private long version;
    private String awarenessAttribute;

    public void setVersion(long version) {
        this.version = version;
    }

    ClusterDeleteWeightedRoutingRequest() {
        this.version = WeightedRoutingMetadata.VERSION_UNSET_VALUE;
    }

    public ClusterDeleteWeightedRoutingRequest(StreamInput in) throws IOException {
        super(in);
        version = in.readLong();
        if (in.available() != 0) {
            awarenessAttribute = in.readString();
        }
    }

    public long getVersion() {
        return version;
    }

    public String getAwarenessAttribute() {
        return awarenessAttribute;
    }

    public void setAwarenessAttribute(String awarenessAttribute) {
        this.awarenessAttribute = awarenessAttribute;
    }

    public ClusterDeleteWeightedRoutingRequest(String awarenessAttribute) {
        this.awarenessAttribute = awarenessAttribute;
        this.version = WeightedRoutingMetadata.VERSION_UNSET_VALUE;
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * @param source weights definition from request body
     * @return this request
     */
    public ClusterDeleteWeightedRoutingRequest source(Map<String, String> source) {
        try {
            if (source.isEmpty()) {
                throw new OpenSearchParseException(("Empty request body"));
            }
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.map(source);
            setRequestBody(BytesReference.bytes(builder), builder.contentType());
        } catch (IOException e) {
            throw new OpenSearchGenerationException("Failed to generate [" + source + "]", e);
        }
        return this;
    }

    private void setRequestBody(BytesReference source, MediaType contentType) {
        try (
            XContentParser parser = XContentHelper.createParser(
                NamedXContentRegistry.EMPTY,
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                source,
                contentType
            )
        ) {
            String versionAttr = null;
            XContentParser.Token token;
            // move to the first alias
            parser.nextToken();
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    if (fieldName != null && fieldName.equals(WeightedRoutingMetadata.VERSION)) {
                        versionAttr = parser.currentName();
                    } else {
                        throw new OpenSearchParseException(
                            "failed to parse delete weighted routing request body [{}], unknown type",
                            fieldName
                        );
                    }
                } else if (token == XContentParser.Token.VALUE_STRING) {
                    if (versionAttr != null && versionAttr.equals(WeightedRoutingMetadata.VERSION)) {
                        this.version = Long.parseLong(parser.text());
                    }
                } else {
                    throw new OpenSearchParseException("failed to parse delete weighted routing request body");
                }
            }
        } catch (IOException e) {
            logger.error("error while parsing delete request for weighted routing request object", e);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeLong(version);
        if (awarenessAttribute != null) {
            out.writeString(awarenessAttribute);
        }
    }

    @Override
    public String toString() {
        return "ClusterDeleteWeightedRoutingRequest{" + "version= " + version + "awarenessAttribute=" + awarenessAttribute + "}";
    }
}
