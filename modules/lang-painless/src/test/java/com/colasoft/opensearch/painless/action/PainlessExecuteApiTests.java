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

package com.colasoft.opensearch.painless.action;

import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.index.IndexService;
import com.colasoft.opensearch.index.query.MatchQueryBuilder;
import com.colasoft.opensearch.painless.PainlessPlugin;
import com.colasoft.opensearch.painless.action.PainlessExecuteAction.Request;
import com.colasoft.opensearch.painless.action.PainlessExecuteAction.Response;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.script.Script;
import com.colasoft.opensearch.script.ScriptException;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.script.ScriptType;
import com.colasoft.opensearch.test.OpenSearchSingleNodeTestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static com.colasoft.opensearch.painless.action.PainlessExecuteAction.TransportAction.innerShardOperation;
import static org.hamcrest.Matchers.equalTo;

public class PainlessExecuteApiTests extends OpenSearchSingleNodeTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Collections.singleton(PainlessPlugin.class);
    }

    public void testDefaults() throws IOException {
        ScriptService scriptService = getInstanceFromNode(ScriptService.class);
        Request request = new Request(new Script("100.0 / 1000.0"), null, null);
        Response response = innerShardOperation(request, scriptService, null);
        assertThat(response.getResult(), equalTo("0.1"));

        Map<String, Object> params = new HashMap<>();
        params.put("count", 100.0D);
        params.put("total", 1000.0D);
        request = new Request(new Script(ScriptType.INLINE, "painless", "params.count / params.total", params), null, null);
        response = innerShardOperation(request, scriptService, null);
        assertThat(response.getResult(), equalTo("0.1"));

        Exception e = expectThrows(ScriptException.class, () -> {
            Request r = new Request(
                new Script(ScriptType.INLINE, "painless", "params.count / params.total + doc['constant']", params),
                null,
                null
            );
            innerShardOperation(r, scriptService, null);
        });
        assertThat(e.getCause().getMessage(), equalTo("cannot resolve symbol [doc]"));
    }

    public void testFilterExecutionContext() throws IOException {
        ScriptService scriptService = getInstanceFromNode(ScriptService.class);
        IndexService indexService = createIndex("index", Settings.EMPTY, "doc", "field", "type=long");

        Request.ContextSetup contextSetup = new Request.ContextSetup("index", new BytesArray("{\"field\": 3}"), null);
        contextSetup.setXContentType(XContentType.JSON);
        Request request = new Request(new Script("doc['field'].value >= 3"), "filter", contextSetup);
        Response response = innerShardOperation(request, scriptService, indexService);
        assertThat(response.getResult(), equalTo(true));

        contextSetup = new Request.ContextSetup("index", new BytesArray("{\"field\": 3}"), null);
        contextSetup.setXContentType(XContentType.JSON);
        request = new Request(
            new Script(ScriptType.INLINE, "painless", "doc['field'].value >= params.max", singletonMap("max", 3)),
            "filter",
            contextSetup
        );
        response = innerShardOperation(request, scriptService, indexService);
        assertThat(response.getResult(), equalTo(true));

        contextSetup = new Request.ContextSetup("index", new BytesArray("{\"field\": 2}"), null);
        contextSetup.setXContentType(XContentType.JSON);
        request = new Request(
            new Script(ScriptType.INLINE, "painless", "doc['field'].value >= params.max", singletonMap("max", 3)),
            "filter",
            contextSetup
        );
        response = innerShardOperation(request, scriptService, indexService);
        assertThat(response.getResult(), equalTo(false));
    }

    public void testScoreExecutionContext() throws IOException {
        ScriptService scriptService = getInstanceFromNode(ScriptService.class);
        IndexService indexService = createIndex("index", Settings.EMPTY, "doc", "rank", "type=long", "text", "type=text");

        Request.ContextSetup contextSetup = new Request.ContextSetup(
            "index",
            new BytesArray("{\"rank\": 4.0, \"text\": \"quick brown fox\"}"),
            new MatchQueryBuilder("text", "fox")
        );
        contextSetup.setXContentType(XContentType.JSON);
        Request request = new Request(
            new Script(
                ScriptType.INLINE,
                "painless",
                "Math.round((_score + (doc['rank'].value / params.max_rank)) * 100.0) / 100.0",
                singletonMap("max_rank", 5.0)
            ),
            "score",
            contextSetup
        );
        Response response = innerShardOperation(request, scriptService, indexService);
        assertThat(response.getResult(), equalTo(0.93D));
    }

}
