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

package com.colasoft.opensearch.monitor.jvm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Setting.Property;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.node.ReportingService;

/**
 * Service for monitoring the JVM
 *
 * @opensearch.internal
 */
public class JvmService implements ReportingService<JvmInfo> {

    private static final Logger logger = LogManager.getLogger(JvmService.class);

    private final JvmInfo jvmInfo;

    private final TimeValue refreshInterval;

    private JvmStats jvmStats;

    public static final Setting<TimeValue> REFRESH_INTERVAL_SETTING = Setting.timeSetting(
        "monitor.jvm.refresh_interval",
        TimeValue.timeValueSeconds(1),
        TimeValue.timeValueSeconds(1),
        Property.NodeScope
    );

    public JvmService(Settings settings) {
        this.jvmInfo = JvmInfo.jvmInfo();
        this.jvmStats = JvmStats.jvmStats();

        this.refreshInterval = REFRESH_INTERVAL_SETTING.get(settings);

        logger.debug("using refresh_interval [{}]", refreshInterval);
    }

    @Override
    public JvmInfo info() {
        return this.jvmInfo;
    }

    public synchronized JvmStats stats() {
        if ((System.currentTimeMillis() - jvmStats.getTimestamp()) > refreshInterval.millis()) {
            jvmStats = JvmStats.jvmStats();
        }
        return jvmStats;
    }
}
