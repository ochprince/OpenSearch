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

package com.colasoft.opensearch.common;

import com.colasoft.opensearch.test.OpenSearchTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Implements exponentially weighted moving averages (commonly abbreviated EWMA) for a single value.
 */
public class ExponentiallyWeightedMovingAverageTests extends OpenSearchTestCase {

    public void testEWMA() {
        final ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(0.5, 10);
        ewma.addValue(12);
        assertThat(ewma.getAverage(), equalTo(11.0));
        ewma.addValue(10);
        ewma.addValue(15);
        ewma.addValue(13);
        assertThat(ewma.getAverage(), equalTo(12.875));
    }

    public void testInvalidAlpha() {
        IllegalArgumentException ex = expectThrows(IllegalArgumentException.class, () -> new ExponentiallyWeightedMovingAverage(-0.5, 10));
        assertThat(ex.getMessage(), equalTo("alpha must be greater or equal to 0 and less than or equal to 1"));

        ex = expectThrows(IllegalArgumentException.class, () -> new ExponentiallyWeightedMovingAverage(1.5, 10));
        assertThat(ex.getMessage(), equalTo("alpha must be greater or equal to 0 and less than or equal to 1"));
    }

    public void testConvergingToValue() {
        final ExponentiallyWeightedMovingAverage ewma = new ExponentiallyWeightedMovingAverage(0.5, 10000);
        for (int i = 0; i < 100000; i++) {
            ewma.addValue(1);
        }
        assertThat(ewma.getAverage(), lessThan(2.0));
    }
}
