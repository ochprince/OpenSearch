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

package org.opensearch.search.aggregations;

import org.opensearch.common.unit.TimeValue;
import org.opensearch.plugins.Plugin;
import org.opensearch.test.TestGeoShapeFieldMapperPlugin;

import java.util.Arrays;
import java.util.Collection;

public class DelayedShardAggregationBuilderTests extends BaseAggregationTestCase<DelayedShardAggregationBuilder> {
    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Arrays.asList(DelayedShardAggregationPlugin.class, TestGeoShapeFieldMapperPlugin.class);
    }

    @Override
    protected DelayedShardAggregationBuilder createTestAggregatorBuilder() {
        return new DelayedShardAggregationBuilder(randomAlphaOfLength(10), TimeValue.timeValueMillis(100));
    }
}
