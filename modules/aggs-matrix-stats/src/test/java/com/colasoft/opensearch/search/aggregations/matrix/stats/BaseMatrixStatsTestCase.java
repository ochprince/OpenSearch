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

package com.colasoft.opensearch.search.aggregations.matrix.stats;

import com.colasoft.opensearch.test.OpenSearchTestCase;
import org.junit.Before;

import java.util.ArrayList;

public abstract class BaseMatrixStatsTestCase extends OpenSearchTestCase {
    protected final int numObs = atLeast(10000);
    protected final ArrayList<Double> fieldA = new ArrayList<>(numObs);
    protected final ArrayList<Double> fieldB = new ArrayList<>(numObs);
    protected final MultiPassStats actualStats = new MultiPassStats(fieldAKey, fieldBKey);
    protected static final String fieldAKey = "fieldA";
    protected static final String fieldBKey = "fieldB";

    @Before
    public void setup() {
        createStats();
    }

    public void createStats() {
        for (int n = 0; n < numObs; ++n) {
            fieldA.add(randomDouble());
            fieldB.add(randomDouble());
        }
        actualStats.computeStats(fieldA, fieldB);
    }

}
