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

package com.colasoft.opensearch.search.pipeline;

import com.colasoft.opensearch.cluster.AbstractDiffable;
import com.colasoft.opensearch.cluster.Diff;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.core.xcontent.ContextParser;
import com.colasoft.opensearch.core.xcontent.MediaType;
import com.colasoft.opensearch.core.xcontent.ObjectParser;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * TODO: Copied verbatim from {@link com.colasoft.opensearch.ingest.PipelineConfiguration}.
 *
 * See if we can refactor into a common class. I suspect not, just because this one will hold
 */
public class PipelineConfiguration extends AbstractDiffable<PipelineConfiguration> implements ToXContentObject {
    private static final ObjectParser<Builder, Void> PARSER = new ObjectParser<>(
        "pipeline_config",
        true,
        PipelineConfiguration.Builder::new
    );
    static {
        PARSER.declareString(PipelineConfiguration.Builder::setId, new ParseField("id"));
        PARSER.declareField((parser, builder, aVoid) -> {
            XContentBuilder contentBuilder = XContentBuilder.builder(parser.contentType().xContent());
            contentBuilder.generator().copyCurrentStructure(parser);
            builder.setConfig(BytesReference.bytes(contentBuilder), contentBuilder.contentType());
        }, new ParseField("config"), ObjectParser.ValueType.OBJECT);

    }

    public static ContextParser<Void, PipelineConfiguration> getParser() {
        return (parser, context) -> PARSER.apply(parser, null).build();
    }

    private static class Builder {

        private String id;
        private BytesReference config;
        private XContentType xContentType;

        void setId(String id) {
            this.id = id;
        }

        void setConfig(BytesReference config, MediaType mediaType) {
            if (mediaType instanceof XContentType == false) {
                throw new IllegalArgumentException("PipelineConfiguration does not support media type [" + mediaType.getClass() + "]");
            }
            this.config = config;
            this.xContentType = XContentType.fromMediaType(mediaType);
        }

        PipelineConfiguration build() {
            return new PipelineConfiguration(id, config, xContentType);
        }
    }

    private final String id;
    // Store config as bytes reference, because the config is only used when the pipeline store reads the cluster state
    // and the way the map of maps config is read requires a deep copy (it removes instead of gets entries to check for unused options)
    // also the get pipeline api just directly returns this to the caller
    private final BytesReference config;
    private final XContentType xContentType;

    public PipelineConfiguration(String id, BytesReference config, XContentType xContentType) {
        this.id = Objects.requireNonNull(id);
        this.config = Objects.requireNonNull(config);
        this.xContentType = Objects.requireNonNull(xContentType);
    }

    public PipelineConfiguration(String id, BytesReference config, MediaType mediaType) {
        this(id, config, XContentType.fromMediaType(mediaType));
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getConfigAsMap() {
        return XContentHelper.convertToMap(config, true, xContentType).v2();
    }

    // pkg-private for tests
    XContentType getXContentType() {
        return xContentType;
    }

    // pkg-private for tests
    BytesReference getConfig() {
        return config;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("id", id);
        builder.field("config", getConfigAsMap());
        builder.endObject();
        return builder;
    }

    public static PipelineConfiguration readFrom(StreamInput in) throws IOException {
        return new PipelineConfiguration(in.readString(), in.readBytesReference(), in.readEnum(XContentType.class));
    }

    public static Diff<PipelineConfiguration> readDiffFrom(StreamInput in) throws IOException {
        return readDiffFrom(PipelineConfiguration::readFrom, in);
    }

    @Override
    public String toString() {
        return Strings.toString(XContentType.JSON, this);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(id);
        out.writeBytesReference(config);
        out.writeEnum(xContentType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PipelineConfiguration that = (PipelineConfiguration) o;

        if (!id.equals(that.id)) return false;
        return getConfigAsMap().equals(that.getConfigAsMap());

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + getConfigAsMap().hashCode();
        return result;
    }
}
