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

package com.colasoft.opensearch.search.aggregations;

import com.colasoft.opensearch.common.ParsingException;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.common.xcontent.ToXContent;
import com.colasoft.opensearch.common.xcontent.XContent;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentFactory;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.common.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.rest.action.search.RestSearchAction;
import com.colasoft.opensearch.search.aggregations.Aggregation.CommonFields;
import com.colasoft.opensearch.search.aggregations.bucket.adjacency.InternalAdjacencyMatrixTests;
import com.colasoft.opensearch.search.aggregations.bucket.composite.InternalCompositeTests;
import com.colasoft.opensearch.search.aggregations.bucket.filter.InternalFilterTests;
import com.colasoft.opensearch.search.aggregations.bucket.filter.InternalFiltersTests;
import com.colasoft.opensearch.search.aggregations.bucket.global.InternalGlobalTests;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalAutoDateHistogramTests;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalDateHistogramTests;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalHistogramTests;
import com.colasoft.opensearch.search.aggregations.bucket.histogram.InternalVariableWidthHistogramTests;
import com.colasoft.opensearch.search.aggregations.bucket.missing.InternalMissingTests;
import com.colasoft.opensearch.search.aggregations.bucket.nested.InternalNestedTests;
import com.colasoft.opensearch.search.aggregations.bucket.nested.InternalReverseNestedTests;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalBinaryRangeTests;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalDateRangeTests;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalGeoDistanceTests;
import com.colasoft.opensearch.search.aggregations.bucket.range.InternalRangeTests;
import com.colasoft.opensearch.search.aggregations.bucket.sampler.InternalSamplerTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.DoubleTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.InternalMultiTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongRareTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.LongTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantLongTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.SignificantStringTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringRareTermsTests;
import com.colasoft.opensearch.search.aggregations.bucket.terms.StringTermsTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalExtendedStatsTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMaxTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMedianAbsoluteDeviationTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalMinTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalStatsBucketTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalStatsTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalSumTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalAvgTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalCardinalityTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalGeoCentroidTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentilesRanksTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalHDRPercentilesTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentilesRanksTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTDigestPercentilesTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalScriptedMetricTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalTopHitsTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalValueCountTests;
import com.colasoft.opensearch.search.aggregations.metrics.InternalWeightedAvgTests;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalSimpleValueTests;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalBucketMetricValueTests;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalPercentilesBucketTests;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalExtendedStatsBucketTests;
import com.colasoft.opensearch.search.aggregations.pipeline.InternalDerivativeTests;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.InternalAggregationTestCase;
import com.colasoft.opensearch.test.InternalMultiBucketAggregationTestCase;
import com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.singletonMap;
import static com.colasoft.opensearch.test.XContentTestUtils.insertRandomFields;

/**
 * This class tests that aggregations parsing works properly. It checks that we can parse
 * different aggregations and adds sub-aggregations where applicable.
 *
 */
public class AggregationsTests extends OpenSearchTestCase {
    private static final List<InternalAggregationTestCase<?>> aggsTests = List.of(
        new InternalCardinalityTests(),
        new InternalTDigestPercentilesTests(),
        new InternalTDigestPercentilesRanksTests(),
        new InternalHDRPercentilesTests(),
        new InternalHDRPercentilesRanksTests(),
        new InternalPercentilesBucketTests(),
        new InternalMinTests(),
        new InternalMaxTests(),
        new InternalAvgTests(),
        new InternalWeightedAvgTests(),
        new InternalSumTests(),
        new InternalValueCountTests(),
        new InternalSimpleValueTests(),
        new InternalDerivativeTests(),
        new InternalBucketMetricValueTests(),
        new InternalStatsTests(),
        new InternalStatsBucketTests(),
        new InternalExtendedStatsTests(),
        new InternalExtendedStatsBucketTests(),
        new InternalGeoCentroidTests(),
        new InternalHistogramTests(),
        new InternalDateHistogramTests(),
        new InternalAutoDateHistogramTests(),
        new InternalVariableWidthHistogramTests(),
        new LongTermsTests(),
        new DoubleTermsTests(),
        new StringTermsTests(),
        new LongRareTermsTests(),
        new StringRareTermsTests(),
        new InternalMissingTests(),
        new InternalNestedTests(),
        new InternalReverseNestedTests(),
        new InternalGlobalTests(),
        new InternalFilterTests(),
        new InternalSamplerTests(),
        new InternalRangeTests(),
        new InternalDateRangeTests(),
        new InternalGeoDistanceTests(),
        new InternalFiltersTests(),
        new InternalAdjacencyMatrixTests(),
        new SignificantLongTermsTests(),
        new SignificantStringTermsTests(),
        new InternalScriptedMetricTests(),
        new InternalBinaryRangeTests(),
        new InternalTopHitsTests(),
        new InternalCompositeTests(),
        new InternalMedianAbsoluteDeviationTests(),
        new InternalMultiTermsTests()
    );

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(InternalAggregationTestCase.getDefaultNamedXContents());
    }

    @Before
    public void init() throws Exception {
        for (InternalAggregationTestCase<?> aggsTest : aggsTests) {
            if (aggsTest instanceof InternalMultiBucketAggregationTestCase) {
                // Lower down the number of buckets generated by multi bucket aggregation tests in
                // order to avoid too many aggregations to be created.
                ((InternalMultiBucketAggregationTestCase<?>) aggsTest).setMaxNumberOfBuckets(3);
            }
            aggsTest.setUp();
        }
    }

    @After
    public void cleanUp() throws Exception {
        for (InternalAggregationTestCase<?> aggsTest : aggsTests) {
            aggsTest.tearDown();
        }
    }

    public void testAllAggsAreBeingTested() {
        assertEquals(InternalAggregationTestCase.getDefaultNamedXContents().size(), aggsTests.size());
        Set<String> aggs = aggsTests.stream().map((testCase) -> testCase.createTestInstance().getType()).collect(Collectors.toSet());
        for (NamedXContentRegistry.Entry entry : InternalAggregationTestCase.getDefaultNamedXContents()) {
            assertTrue(aggs.contains(entry.name.getPreferredName()));
        }
    }

    public void testFromXContent() throws IOException {
        parseAndAssert(false);
    }

    public void testFromXContentWithRandomFields() throws IOException {
        parseAndAssert(true);
    }

    /**
     * Test that parsing works for a randomly created Aggregations object with a
     * randomized aggregation tree. The test randomly chooses an
     * {@link XContentType}, randomizes the order of the {@link XContent} fields
     * and randomly sets the `humanReadable` flag when rendering the
     * {@link XContent}.
     *
     * @param addRandomFields
     *            if set, this will also add random {@link XContent} fields to
     *            tests that the parsers are lenient to future additions to rest
     *            responses
     */
    private void parseAndAssert(boolean addRandomFields) throws IOException {
        XContentType xContentType = randomFrom(XContentType.values());
        final ToXContent.Params params = new ToXContent.MapParams(singletonMap(RestSearchAction.TYPED_KEYS_PARAM, "true"));
        Aggregations aggregations = createTestInstance(1, 0, 3);
        BytesReference originalBytes = toShuffledXContent(aggregations, xContentType, params, randomBoolean());
        BytesReference mutated;
        if (addRandomFields) {
            /*
             * - don't insert into the root object because it should only contain the named aggregations to test
             *
             * - don't insert into the "meta" object, because we pass on everything we find there
             *
             * - we don't want to directly insert anything random into "buckets"  objects, they are used with
             * "keyed" aggregations and contain named bucket objects. Any new named object on this level should
             * also be a bucket and be parsed as such.
             *
             * - we cannot insert randomly into VALUE or VALUES objects e.g. in Percentiles, the keys need to be numeric there
             *
             * - we cannot insert into ExtendedMatrixStats "covariance" or "correlation" fields, their syntax is strict
             *
             * - we cannot insert random values in top_hits, as all unknown fields
             * on a root level of SearchHit are interpreted as meta-fields and will be kept
             *
             * - exclude "key", it can be an array of objects and we need strict values
             */
            Predicate<String> excludes = path -> (path.isEmpty()
                || path.endsWith("aggregations")
                || path.endsWith(Aggregation.CommonFields.META.getPreferredName())
                || path.endsWith(Aggregation.CommonFields.BUCKETS.getPreferredName())
                || path.endsWith(CommonFields.VALUES.getPreferredName())
                || path.endsWith("covariance")
                || path.endsWith("correlation")
                || path.contains(CommonFields.VALUE.getPreferredName())
                || path.endsWith(CommonFields.KEY.getPreferredName())) || path.contains("top_hits");
            mutated = insertRandomFields(xContentType, originalBytes, excludes, random());
        } else {
            mutated = originalBytes;
        }
        try (XContentParser parser = createParser(xContentType.xContent(), mutated)) {
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            assertEquals(XContentParser.Token.FIELD_NAME, parser.nextToken());
            assertEquals(Aggregations.AGGREGATIONS_FIELD, parser.currentName());
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            Aggregations parsedAggregations = Aggregations.fromXContent(parser);
            BytesReference parsedBytes = XContentHelper.toXContent(parsedAggregations, xContentType, randomBoolean());
            OpenSearchAssertions.assertToXContentEquivalent(originalBytes, parsedBytes, xContentType);
        }
    }

    public void testParsingExceptionOnUnknownAggregation() throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("unknownAggregation");
            builder.endObject();
        }
        builder.endObject();
        BytesReference originalBytes = BytesReference.bytes(builder);
        try (XContentParser parser = createParser(builder.contentType().xContent(), originalBytes)) {
            assertEquals(XContentParser.Token.START_OBJECT, parser.nextToken());
            ParsingException ex = expectThrows(ParsingException.class, () -> Aggregations.fromXContent(parser));
            assertEquals("Could not parse aggregation keyed as [unknownAggregation]", ex.getMessage());
        }
    }

    public final InternalAggregations createTestInstance() {
        return createTestInstance(1, 0, 5);
    }

    private static InternalAggregations createTestInstance(final int minNumAggs, final int currentDepth, final int maxDepth) {
        int numAggs = randomIntBetween(minNumAggs, 4);
        List<InternalAggregation> aggs = new ArrayList<>(numAggs);
        for (int i = 0; i < numAggs; i++) {
            InternalAggregationTestCase<?> testCase = randomFrom(aggsTests);
            if (testCase instanceof InternalMultiBucketAggregationTestCase) {
                InternalMultiBucketAggregationTestCase<?> multiBucketAggTestCase = (InternalMultiBucketAggregationTestCase<?>) testCase;
                if (currentDepth < maxDepth) {
                    multiBucketAggTestCase.setSubAggregationsSupplier(() -> createTestInstance(0, currentDepth + 1, maxDepth));
                } else {
                    multiBucketAggTestCase.setSubAggregationsSupplier(() -> InternalAggregations.EMPTY);
                }
            } else if (testCase instanceof InternalSingleBucketAggregationTestCase) {
                InternalSingleBucketAggregationTestCase<?> singleBucketAggTestCase = (InternalSingleBucketAggregationTestCase<?>) testCase;
                if (currentDepth < maxDepth) {
                    singleBucketAggTestCase.subAggregationsSupplier = () -> createTestInstance(0, currentDepth + 1, maxDepth);
                } else {
                    singleBucketAggTestCase.subAggregationsSupplier = () -> InternalAggregations.EMPTY;
                }
            }
            aggs.add(testCase.createTestInstanceForXContent());
        }
        return InternalAggregations.from(aggs);
    }
}
