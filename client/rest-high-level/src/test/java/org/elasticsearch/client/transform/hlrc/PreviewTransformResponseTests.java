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

package org.elasticsearch.client.transform.hlrc;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.AbstractResponseTestCase;
import org.elasticsearch.client.transform.PreviewTransformResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.core.transform.action.PreviewTransformAction;
import org.elasticsearch.xpack.core.transform.action.PreviewTransformAction.Response;
import org.elasticsearch.xpack.core.transform.transforms.TransformDestIndexSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;

public class PreviewTransformResponseTests extends AbstractResponseTestCase<
    PreviewTransformAction.Response,
    org.elasticsearch.client.transform.PreviewTransformResponse> {

    public static Response randomPreviewResponse() {
        int size = randomIntBetween(0, 10);
        List<Map<String, Object>> data = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            data.add(Collections.singletonMap(randomAlphaOfLength(10), Collections.singletonMap("value1", randomIntBetween(1, 100))));
        }

        return new Response(data, randomGeneratedDestIndexSettings());
    }

    private static TransformDestIndexSettings randomGeneratedDestIndexSettings() {
        int size = randomIntBetween(0, 10);

        Map<String, Object> mappings = null;

        if (randomBoolean()) {
            mappings = new HashMap<>(size);

            for (int i = 0; i < size; i++) {
                mappings.put(randomAlphaOfLength(10), Collections.singletonMap("type", randomAlphaOfLength(10)));
            }
        }

        Settings settings = null;
        if (randomBoolean()) {
            Settings.Builder settingsBuilder = Settings.builder();
            size = randomIntBetween(0, 10);
            for (int i = 0; i < size; i++) {
                settingsBuilder.put(randomAlphaOfLength(10), randomBoolean());
            }
            settings = settingsBuilder.build();
        }

        Set<Alias> aliases = null;

        if (randomBoolean()) {
            aliases = new HashSet<>();
            size = randomIntBetween(0, 10);
            for (int i = 0; i < size; i++) {
                aliases.add(new Alias(randomAlphaOfLength(10)));
            }
        }

        return new TransformDestIndexSettings(mappings, settings, aliases);
    }

    @Override
    protected Response createServerTestInstance(XContentType xContentType) {
        return randomPreviewResponse();
    }

    @Override
    protected PreviewTransformResponse doParseToClientInstance(XContentParser parser) throws IOException {
        return org.elasticsearch.client.transform.PreviewTransformResponse.fromXContent(parser);
    }

    @Override
    protected void assertInstances(Response serverTestInstance, PreviewTransformResponse clientInstance) {
        assertThat(serverTestInstance.getDocs(), equalTo(clientInstance.getDocs()));
        assertThat(
            serverTestInstance.getGeneratedDestIndexSettings().getAliases(),
            equalTo(clientInstance.getGeneratedDestIndexSettings().getAliases())
        );
        assertThat(
            serverTestInstance.getGeneratedDestIndexSettings().getMappings(),
            equalTo(clientInstance.getGeneratedDestIndexSettings().getMappings())
        );
        assertThat(
            serverTestInstance.getGeneratedDestIndexSettings().getSettings(),
            equalTo(clientInstance.getGeneratedDestIndexSettings().getSettings())
        );
    }
}
