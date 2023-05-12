/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.test;

import com.colasoft.opensearch.common.SuppressForbidden;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Helper class that wraps the lifecycle of setting and finally clearing of
 * a {@link com.colasoft.opensearch.common.util.FeatureFlags} string in an {@link AutoCloseable}.
 */
public class FeatureFlagSetter implements AutoCloseable {

    private final String flag;

    private FeatureFlagSetter(String flag) {
        this.flag = flag;
    }

    @SuppressForbidden(reason = "Enables setting of feature flags")
    public static final FeatureFlagSetter set(String flag) {
        AccessController.doPrivileged((PrivilegedAction<String>) () -> System.setProperty(flag, "true"));
        return new FeatureFlagSetter(flag);
    }

    @SuppressForbidden(reason = "Clears the set feature flag on close")
    @Override
    public void close() throws Exception {
        AccessController.doPrivileged((PrivilegedAction<String>) () -> System.clearProperty(this.flag));
    }
}
