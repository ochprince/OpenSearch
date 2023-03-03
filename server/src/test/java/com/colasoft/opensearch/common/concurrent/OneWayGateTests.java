/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.common.concurrent;

import org.junit.Before;
import com.colasoft.opensearch.test.OpenSearchTestCase;

public class OneWayGateTests extends OpenSearchTestCase {

    private OneWayGate testGate;

    @Before
    public void setup() {
        testGate = new OneWayGate();
    }

    public void testGateOpen() {
        assertFalse(testGate.isClosed());
    }

    public void testGateClosed() {
        testGate.close();
        assertTrue(testGate.isClosed());
    }

    public void testGateIdempotent() {
        assertTrue(testGate.close());
        assertFalse(testGate.close());
    }
}
