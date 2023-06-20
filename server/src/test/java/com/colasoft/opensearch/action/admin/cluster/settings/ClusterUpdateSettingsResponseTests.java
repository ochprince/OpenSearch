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

package com.colasoft.opensearch.action.admin.cluster.settings;

import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.Settings.Builder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.test.AbstractSerializingTestCase;
import com.colasoft.opensearch.test.VersionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ClusterUpdateSettingsResponseTests extends AbstractSerializingTestCase<ClusterUpdateSettingsResponse> {

    @Override
    protected ClusterUpdateSettingsResponse doParseInstance(XContentParser parser) {
        return ClusterUpdateSettingsResponse.fromXContent(parser);
    }

    @Override
    protected ClusterUpdateSettingsResponse mutateInstance(ClusterUpdateSettingsResponse response) {
        int i = randomIntBetween(0, 2);
        switch (i) {
            case 0:
                return new ClusterUpdateSettingsResponse(
                    response.isAcknowledged() == false,
                    response.transientSettings,
                    response.persistentSettings
                );
            case 1:
                return new ClusterUpdateSettingsResponse(
                    response.isAcknowledged(),
                    mutateSettings(response.transientSettings),
                    response.persistentSettings
                );
            case 2:
                return new ClusterUpdateSettingsResponse(
                    response.isAcknowledged(),
                    response.transientSettings,
                    mutateSettings(response.persistentSettings)
                );
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static Settings mutateSettings(Settings settings) {
        if (settings.isEmpty()) {
            return randomClusterSettings(1, 3);
        }
        Set<String> allKeys = settings.keySet();
        List<String> keysToBeModified = randomSubsetOf(randomIntBetween(1, allKeys.size()), allKeys);
        Builder builder = Settings.builder();
        for (String key : allKeys) {
            String value = settings.get(key);
            if (keysToBeModified.contains(key)) {
                value += randomAlphaOfLengthBetween(2, 5);
            }
            builder.put(key, value);
        }
        return builder.build();
    }

    @Override
    protected Predicate<String> getRandomFieldsExcludeFilter() {
        return p -> p.startsWith("transient") || p.startsWith("persistent");
    }

    public static Settings randomClusterSettings(int min, int max) {
        int num = randomIntBetween(min, max);
        Builder builder = Settings.builder();
        for (int i = 0; i < num; i++) {
            Setting<?> setting = randomFrom(ClusterSettings.BUILT_IN_CLUSTER_SETTINGS);
            builder.put(setting.getKey(), randomAlphaOfLengthBetween(2, 10));
        }
        return builder.build();
    }

    @Override
    protected ClusterUpdateSettingsResponse createTestInstance() {
        return new ClusterUpdateSettingsResponse(randomBoolean(), randomClusterSettings(0, 2), randomClusterSettings(0, 2));
    }

    @Override
    protected Writeable.Reader<ClusterUpdateSettingsResponse> instanceReader() {
        return ClusterUpdateSettingsResponse::new;
    }

    public void testOldSerialisation() throws IOException {
        ClusterUpdateSettingsResponse original = createTestInstance();
        assertSerialization(original, VersionUtils.randomIndexCompatibleVersion(random()));
    }
}
