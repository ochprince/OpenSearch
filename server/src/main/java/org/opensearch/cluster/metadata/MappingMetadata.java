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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.cluster.metadata;

import org.opensearch.LegacyESVersion;
import org.opensearch.OpenSearchParseException;
import org.opensearch.cluster.AbstractDiffable;
import org.opensearch.cluster.Diff;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.index.mapper.DocumentMapper;
import org.opensearch.index.mapper.MapperService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;

import static org.opensearch.common.xcontent.support.XContentMapValues.nodeBooleanValue;

/**
 * Mapping configuration for a type.
 */
public class MappingMetadata extends AbstractDiffable<MappingMetadata> {
    public static final MappingMetadata EMPTY_MAPPINGS = new MappingMetadata(MapperService.SINGLE_MAPPING_NAME, Collections.emptyMap());

    public static class Routing {

        public static final Routing EMPTY = new Routing(false);

        private final boolean required;

        public Routing(boolean required) {
            this.required = required;
        }

        public boolean required() {
            return required;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Routing routing = (Routing) o;

            return required == routing.required;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode() + (required ? 1 : 0);
        }
    }

    private final String type;

    private final CompressedXContent source;

    private final Routing routing;

    public MappingMetadata(DocumentMapper docMapper) {
        this.type = docMapper.type();
        this.source = docMapper.mappingSource();
        this.routing = new Routing(docMapper.routingFieldMapper().required());
    }

    @SuppressWarnings("unchecked")
    public MappingMetadata(CompressedXContent mapping) {
        this.source = mapping;
        Map<String, Object> mappingMap = XContentHelper.convertToMap(mapping.compressedReference(), true).v2();
        if (mappingMap.size() != 1) {
            throw new IllegalStateException("Can't derive type from mapping, no root type: " + mapping.string());
        }
        this.type = mappingMap.keySet().iterator().next();
        this.routing = initRouting((Map<String, Object>) mappingMap.get(this.type));
    }

    @SuppressWarnings("unchecked")
    public MappingMetadata(String type, Map<String, Object> mapping) {
        this.type = type;
        try {
            XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().map(mapping);
            this.source = new CompressedXContent(BytesReference.bytes(mappingBuilder));
        } catch (IOException e) {
            throw new UncheckedIOException(e);  // XContent exception, should never happen
        }
        Map<String, Object> withoutType = mapping;
        if (mapping.size() == 1 && mapping.containsKey(type)) {
            withoutType = (Map<String, Object>) mapping.get(type);
        }
        this.routing = initRouting(withoutType);
    }

    @SuppressWarnings("unchecked")
    private Routing initRouting(Map<String, Object> withoutType) {
        if (withoutType.containsKey("_routing")) {
            boolean required = false;
            Map<String, Object> routingNode = (Map<String, Object>) withoutType.get("_routing");
            for (Map.Entry<String, Object> entry : routingNode.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldNode = entry.getValue();
                if (fieldName.equals("required")) {
                    try {
                        required = nodeBooleanValue(fieldNode);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException(
                            "Failed to create mapping for type [" + this.type() + "]. " + "Illegal value in field [_routing.required].",
                            ex
                        );
                    }
                }
            }
            return new Routing(required);
        } else {
            return Routing.EMPTY;
        }
    }

    public String type() {
        return this.type;
    }

    public CompressedXContent source() {
        return this.source;
    }

    /**
     * Converts the serialized compressed form of the mappings into a parsed map.
     */
    public Map<String, Object> sourceAsMap() throws OpenSearchParseException {
        Map<String, Object> mapping = XContentHelper.convertToMap(source.compressedReference(), true).v2();
        if (mapping.size() == 1 && mapping.containsKey(type())) {
            // the type name is the root value, reduce it
            mapping = (Map<String, Object>) mapping.get(type());
        }
        return mapping;
    }

    /**
     * Converts the serialized compressed form of the mappings into a parsed map.
     */
    public Map<String, Object> getSourceAsMap() throws OpenSearchParseException {
        return sourceAsMap();
    }

    public Routing routing() {
        return this.routing;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(type());
        source().writeTo(out);
        // routing
        out.writeBoolean(routing().required());
        if (out.getVersion().before(LegacyESVersion.V_7_0_0)) {
            out.writeBoolean(false); // hasParentField
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MappingMetadata that = (MappingMetadata) o;

        if (!routing.equals(that.routing)) return false;
        if (!source.equals(that.source)) return false;
        if (!type.equals(that.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + source.hashCode();
        result = 31 * result + routing.hashCode();
        return result;
    }

    public MappingMetadata(StreamInput in) throws IOException {
        type = in.readString();
        source = CompressedXContent.readCompressedString(in);
        // routing
        routing = new Routing(in.readBoolean());
        if (in.getVersion().before(LegacyESVersion.V_7_0_0)) {
            in.readBoolean(); // hasParentField
        }
    }

    public static Diff<MappingMetadata> readDiffFrom(StreamInput in) throws IOException {
        return readDiffFrom(MappingMetadata::new, in);
    }
}
