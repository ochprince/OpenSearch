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

package com.colasoft.opensearch.plugins.spi;

import com.colasoft.opensearch.common.ParseField;
import com.colasoft.opensearch.common.io.Streams;
import com.colasoft.opensearch.common.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.search.aggregations.Aggregation;
import com.colasoft.opensearch.search.aggregations.pipeline.ParsedSimpleValue;
import com.colasoft.opensearch.search.suggest.Suggest;
import com.colasoft.opensearch.search.suggest.term.TermSuggestion;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;

public class NamedXContentProviderTests extends OpenSearchTestCase {

    public void testSpiFileExists() throws IOException {
        String serviceFile = "/META-INF/services/" + NamedXContentProvider.class.getName();
        List<String> implementations = new ArrayList<>();
        try (InputStream input = NamedXContentProviderTests.class.getResourceAsStream(serviceFile)) {
            Streams.readAllLines(input, implementations::add);
        }

        assertEquals(1, implementations.size());
        assertEquals(TestNamedXContentProvider.class.getName(), implementations.get(0));
    }

    public void testNamedXContents() {
        final List<NamedXContentRegistry.Entry> namedXContents = new ArrayList<>();
        for (NamedXContentProvider service : ServiceLoader.load(NamedXContentProvider.class)) {
            namedXContents.addAll(service.getNamedXContentParsers());
        }

        assertEquals(2, namedXContents.size());

        List<Predicate<NamedXContentRegistry.Entry>> predicates = new ArrayList<>(2);
        predicates.add(e -> Aggregation.class.equals(e.categoryClass) && "test_aggregation".equals(e.name.getPreferredName()));
        predicates.add(e -> Suggest.Suggestion.class.equals(e.categoryClass) && "test_suggestion".equals(e.name.getPreferredName()));
        predicates.forEach(predicate -> assertEquals(1, namedXContents.stream().filter(predicate).count()));
    }

    public static class TestNamedXContentProvider implements NamedXContentProvider {

        public TestNamedXContentProvider() {}

        @Override
        public List<NamedXContentRegistry.Entry> getNamedXContentParsers() {
            return Arrays.asList(
                new NamedXContentRegistry.Entry(
                    Aggregation.class,
                    new ParseField("test_aggregation"),
                    (parser, context) -> ParsedSimpleValue.fromXContent(parser, (String) context)
                ),
                new NamedXContentRegistry.Entry(
                    Suggest.Suggestion.class,
                    new ParseField("test_suggestion"),
                    (parser, context) -> TermSuggestion.fromXContent(parser, (String) context)
                )
            );
        }
    }
}
