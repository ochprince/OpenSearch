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

package com.colasoft.opensearch.common.xcontent.yaml;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.colasoft.opensearch.core.xcontent.DeprecationHandler;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.XContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentGenerator;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.core.xcontent.MediaType;
import com.colasoft.opensearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Set;

/**
 * A YAML based content implementation using Jackson.
 */
public class YamlXContent implements XContent {

    public static XContentBuilder contentBuilder() throws IOException {
        return XContentBuilder.builder(yamlXContent);
    }

    static final YAMLFactory yamlFactory;
    public static final YamlXContent yamlXContent;

    static {
        yamlFactory = new YAMLFactory();
        yamlFactory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
        yamlXContent = new YamlXContent();
    }

    private YamlXContent() {}

    @Override
    public MediaType mediaType() {
        return XContentType.YAML;
    }

    @Override
    public byte streamSeparator() {
        throw new UnsupportedOperationException("yaml does not support stream parsing...");
    }

    @Override
    public XContentGenerator createGenerator(OutputStream os, Set<String> includes, Set<String> excludes) throws IOException {
        return new YamlXContentGenerator(yamlFactory.createGenerator(os, JsonEncoding.UTF8), os, includes, excludes);
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler, String content)
        throws IOException {
        return new YamlXContentParser(xContentRegistry, deprecationHandler, yamlFactory.createParser(content));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler, InputStream is)
        throws IOException {
        return new YamlXContentParser(xContentRegistry, deprecationHandler, yamlFactory.createParser(is));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler, byte[] data)
        throws IOException {
        return new YamlXContentParser(xContentRegistry, deprecationHandler, yamlFactory.createParser(data));
    }

    @Override
    public XContentParser createParser(
        NamedXContentRegistry xContentRegistry,
        DeprecationHandler deprecationHandler,
        byte[] data,
        int offset,
        int length
    ) throws IOException {
        return new YamlXContentParser(xContentRegistry, deprecationHandler, yamlFactory.createParser(data, offset, length));
    }

    @Override
    public XContentParser createParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler, Reader reader)
        throws IOException {
        return new YamlXContentParser(xContentRegistry, deprecationHandler, yamlFactory.createParser(reader));
    }
}
