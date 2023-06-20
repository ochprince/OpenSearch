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
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.common.concurrent;

import org.junit.Before;
import com.colasoft.opensearch.common.util.concurrent.RefCounted;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AutoCloseableRefCountedTests extends OpenSearchTestCase {

    private RefCounted mockRefCounted;
    private AutoCloseableRefCounted<RefCounted> testObject;

    @Before
    public void setup() {
        mockRefCounted = mock(RefCounted.class);
        testObject = new AutoCloseableRefCounted<>(mockRefCounted);
    }

    public void testGet() {
        assertEquals(mockRefCounted, testObject.get());
    }

    public void testClose() {
        testObject.close();
        verify(mockRefCounted, atMostOnce()).decRef();
    }

    public void testIdempotent() {
        testObject.close();
        testObject.close();
        verify(mockRefCounted, atMostOnce()).decRef();
    }
}
