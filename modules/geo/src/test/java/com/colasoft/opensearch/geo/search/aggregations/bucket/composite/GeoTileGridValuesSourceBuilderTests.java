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

package com.colasoft.opensearch.geo.search.aggregations.bucket.composite;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.common.geo.GeoBoundingBox;
import com.colasoft.opensearch.common.geo.GeoPoint;
import com.colasoft.opensearch.common.io.stream.BytesStreamOutput;
import com.colasoft.opensearch.common.io.stream.NamedWriteableAwareStreamInput;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.geo.tests.common.RandomGeoGenerator;
import com.colasoft.opensearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.VersionUtils;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;

public class GeoTileGridValuesSourceBuilderTests extends OpenSearchTestCase {

    public void testSetFormat() {
        CompositeValuesSourceBuilder<?> builder = new GeoTileGridValuesSourceBuilder("name");
        expectThrows(IllegalArgumentException.class, () -> builder.format("format"));
    }

    public void testBWCBounds() throws IOException {
        Version noBoundsSupportVersion = VersionUtils.randomVersionBetween(random(), LegacyESVersion.V_7_0_0, LegacyESVersion.V_7_5_0);
        GeoTileGridValuesSourceBuilder builder = new GeoTileGridValuesSourceBuilder("name");
        if (randomBoolean()) {
            builder.geoBoundingBox(RandomGeoGenerator.randomBBox());
        }
        try (BytesStreamOutput output = new BytesStreamOutput()) {
            output.setVersion(LegacyESVersion.V_7_6_0);
            builder.writeTo(output);
            try (
                StreamInput in = new NamedWriteableAwareStreamInput(
                    output.bytes().streamInput(),
                    new NamedWriteableRegistry(Collections.emptyList())
                )
            ) {
                in.setVersion(noBoundsSupportVersion);
                GeoTileGridValuesSourceBuilder readBuilder = new GeoTileGridValuesSourceBuilder(in);
                assertThat(
                    readBuilder.geoBoundingBox(),
                    equalTo(new GeoBoundingBox(new GeoPoint(Double.NaN, Double.NaN), new GeoPoint(Double.NaN, Double.NaN)))
                );
            }
        }
    }
}
