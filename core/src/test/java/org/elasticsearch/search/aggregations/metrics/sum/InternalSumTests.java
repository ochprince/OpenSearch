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
package org.elasticsearch.search.aggregations.metrics.sum;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.InternalAggregationTestCase;
import org.elasticsearch.search.aggregations.ParsedAggregation;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.util.List;
import java.util.Map;

public class InternalSumTests extends InternalAggregationTestCase<InternalSum> {

    @Override
    protected InternalSum createTestInstance(String name, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        double value = frequently() ? randomDouble() : randomFrom(new Double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY });
        DocValueFormat formatter = randomFrom(new DocValueFormat.Decimal("###.##"), DocValueFormat.BOOLEAN, DocValueFormat.RAW);
        return new InternalSum(name, value, formatter, pipelineAggregators, metaData);
    }

    @Override
    protected Writeable.Reader<InternalSum> instanceReader() {
        return InternalSum::new;
    }

    @Override
    protected void assertReduced(InternalSum reduced, List<InternalSum> inputs) {
        double expectedSum = inputs.stream().mapToDouble(InternalSum::getValue).sum();
        assertEquals(expectedSum, reduced.getValue(), 0.0001d);
    }

    @Override
    protected void assertFromXContent(InternalSum sum, ParsedAggregation parsedAggregation) {
        ParsedSum parsed = ((ParsedSum) parsedAggregation);
        assertEquals(sum.getValue(), parsed.getValue(), Double.MIN_VALUE);
        assertEquals(sum.getValueAsString(), parsed.getValueAsString());
    }
}
