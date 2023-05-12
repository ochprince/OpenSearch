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

package com.colasoft.opensearch.search.aggregations.pipeline;

import com.colasoft.opensearch.search.DocValueFormat;
import com.colasoft.opensearch.search.aggregations.ParsedAggregation;
import com.colasoft.opensearch.test.InternalAggregationTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalDerivativeTests extends InternalAggregationTestCase<InternalDerivative> {

    @Override
    protected InternalDerivative createTestInstance(String name, Map<String, Object> metadata) {
        DocValueFormat formatter = randomNumericDocValueFormat();
        double value = frequently()
            ? randomDoubleBetween(-100000, 100000, true)
            : randomFrom(new Double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN });
        double normalizationFactor = frequently() ? randomDoubleBetween(0, 100000, true) : 0;
        return new InternalDerivative(name, value, normalizationFactor, formatter, metadata);
    }

    @Override
    public void testReduceRandom() {
        expectThrows(UnsupportedOperationException.class, () -> createTestInstance("name", null).reduce(null, null));
    }

    @Override
    protected void assertReduced(InternalDerivative reduced, List<InternalDerivative> inputs) {
        // no test since reduce operation is unsupported
    }

    @Override
    protected void assertFromXContent(InternalDerivative derivative, ParsedAggregation parsedAggregation) {
        ParsedDerivative parsed = ((ParsedDerivative) parsedAggregation);
        if (Double.isInfinite(derivative.getValue()) == false && Double.isNaN(derivative.getValue()) == false) {
            assertEquals(derivative.getValue(), parsed.value(), Double.MIN_VALUE);
            assertEquals(derivative.getValueAsString(), parsed.getValueAsString());
        } else {
            // we write Double.NEGATIVE_INFINITY, Double.POSITIVE amd Double.NAN to xContent as 'null', so we
            // cannot differentiate between them. Also we cannot recreate the exact String representation
            assertEquals(parsed.value(), Double.NaN, Double.MIN_VALUE);
        }
    }

    @Override
    protected InternalDerivative mutateInstance(InternalDerivative instance) {
        String name = instance.getName();
        double value = instance.getValue();
        double normalizationFactor = instance.getNormalizationFactor();
        DocValueFormat formatter = instance.formatter();
        Map<String, Object> metadata = instance.getMetadata();
        switch (between(0, 2)) {
            case 0:
                name += randomAlphaOfLength(5);
                break;
            case 1:
                if (Double.isFinite(value)) {
                    value += between(1, 100);
                } else {
                    value = randomDoubleBetween(0, 100000, true);
                }
                break;
            case 2:
                normalizationFactor += between(1, 100);
                break;
            case 3:
                if (metadata == null) {
                    metadata = new HashMap<>(1);
                } else {
                    metadata = new HashMap<>(instance.getMetadata());
                }
                metadata.put(randomAlphaOfLength(15), randomInt());
                break;
            default:
                throw new AssertionError("Illegal randomisation branch");
        }
        return new InternalDerivative(name, value, normalizationFactor, formatter, metadata);
    }
}
