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

package com.colasoft.opensearch.join.aggregations;

import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry.Entry;
import com.colasoft.opensearch.join.ParentJoinPlugin;
import com.colasoft.opensearch.plugins.SearchPlugin;
import com.colasoft.opensearch.search.aggregations.Aggregation;
import com.colasoft.opensearch.search.aggregations.InternalAggregations;
import com.colasoft.opensearch.search.aggregations.InternalSingleBucketAggregationTestCase;
import com.colasoft.opensearch.search.aggregations.bucket.ParsedSingleBucketAggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InternalParentTests extends InternalSingleBucketAggregationTestCase<InternalParent> {

    @Override
    protected SearchPlugin registerPlugin() {
        return new ParentJoinPlugin();
    }

    @Override
    protected List<Entry> getNamedXContents() {
        List<Entry> extendedNamedXContents = new ArrayList<>(super.getNamedXContents());
        extendedNamedXContents.add(
            new Entry(Aggregation.class, new ParseField(ParentAggregationBuilder.NAME), (p, c) -> ParsedParent.fromXContent(p, (String) c))
        );
        return extendedNamedXContents;
    }

    @Override
    protected InternalParent createTestInstance(
        String name,
        long docCount,
        InternalAggregations aggregations,
        Map<String, Object> metadata
    ) {
        return new InternalParent(name, docCount, aggregations, metadata);
    }

    @Override
    protected void extraAssertReduced(InternalParent reduced, List<InternalParent> inputs) {
        // Nothing extra to assert
    }

    @Override
    protected Class<? extends ParsedSingleBucketAggregation> implementationClass() {
        return ParsedParent.class;
    }
}
