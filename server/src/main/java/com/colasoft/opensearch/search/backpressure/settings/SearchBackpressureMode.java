/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.backpressure.settings;

/**
 * Defines the search backpressure mode.
 */
public enum SearchBackpressureMode {
    /**
     * SearchBackpressureService is completely disabled.
     */
    DISABLED("disabled"),

    /**
     * SearchBackpressureService only monitors the resource usage of running tasks.
     */
    MONITOR_ONLY("monitor_only"),

    /**
     * SearchBackpressureService monitors and rejects tasks that exceed resource usage thresholds.
     */
    ENFORCED("enforced");

    private final String name;

    SearchBackpressureMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static SearchBackpressureMode fromName(String name) {
        switch (name) {
            case "disabled":
                return DISABLED;
            case "monitor_only":
                return MONITOR_ONLY;
            case "enforced":
                return ENFORCED;
        }

        throw new IllegalArgumentException("Invalid SearchBackpressureMode: " + name);
    }
}
