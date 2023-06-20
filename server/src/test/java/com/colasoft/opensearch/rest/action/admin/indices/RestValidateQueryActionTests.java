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

package com.colasoft.opensearch.rest.action.admin.indices;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionType;
import com.colasoft.opensearch.action.ActionModule.DynamicActionRegistry;
import com.colasoft.opensearch.action.admin.indices.validate.query.ValidateQueryAction;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.TransportAction;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.bytes.BytesArray;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.indices.breaker.NoneCircuitBreakerService;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.search.AbstractSearchTestCase;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.tasks.TaskManager;
import com.colasoft.opensearch.test.rest.FakeRestChannel;
import com.colasoft.opensearch.test.rest.FakeRestRequest;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.usage.UsageService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class RestValidateQueryActionTests extends AbstractSearchTestCase {

    private static ThreadPool threadPool = new TestThreadPool(RestValidateQueryActionTests.class.getName());
    private static NodeClient client = new NodeClient(Settings.EMPTY, threadPool);

    private static UsageService usageService = new UsageService();
    private static RestController controller = new RestController(emptySet(), null, client, new NoneCircuitBreakerService(), usageService);
    private static RestValidateQueryAction action = new RestValidateQueryAction();

    /**
     * Configures {@link NodeClient} to stub {@link ValidateQueryAction} transport action.
     * <p>
     * This lower level of validation is out of the scope of this test.
     */
    @BeforeClass
    public static void stubValidateQueryAction() {
        final TaskManager taskManager = new TaskManager(Settings.EMPTY, threadPool, Collections.emptySet());

        final TransportAction<?, ?> transportAction = new TransportAction<>(
            ValidateQueryAction.NAME,
            new ActionFilters(Collections.emptySet()),
            taskManager
        ) {
            @Override
            protected void doExecute(Task task, ActionRequest request, ActionListener listener) {}
        };

        final Map<ActionType, TransportAction> actions = new HashMap<>();
        actions.put(ValidateQueryAction.INSTANCE, transportAction);

        DynamicActionRegistry dynamicActionRegistry = new DynamicActionRegistry();
        dynamicActionRegistry.registerUnmodifiableActionMap(actions);
        client.initialize(dynamicActionRegistry, () -> "local", null, new NamedWriteableRegistry(Collections.emptyList()));
        controller.registerHandler(action);
    }

    @Before
    public void ensureCleanContext() {
        // Make sure we have a clean context for each test
        threadPool.getThreadContext().stashContext();
    }

    @AfterClass
    public static void terminateThreadPool() throws InterruptedException {
        terminate(threadPool);

        threadPool = null;
        client = null;

        usageService = null;
        controller = null;
        action = null;
    }

    public void testRestValidateQueryAction() throws Exception {
        // GIVEN a valid query
        final String content = "{\"query\":{\"bool\":{\"must\":{\"term\":{\"user\":\"foobar\"}}}}}";

        final RestRequest request = createRestRequest(content);
        final FakeRestChannel channel = new FakeRestChannel(request, true, 0);

        // WHEN
        action.handleRequest(request, channel, client);

        // THEN query is valid (i.e. not marked as invalid)
        assertThat(channel.responses().get(), equalTo(0));
        assertThat(channel.errors().get(), equalTo(0));
        assertNull(channel.capturedResponse());
    }

    public void testRestValidateQueryAction_emptyQuery() throws Exception {
        // GIVEN an empty (i.e. invalid) query wrapped into a valid JSON
        final String content = "{\"query\":{}}";

        final RestRequest request = createRestRequest(content);
        final FakeRestChannel channel = new FakeRestChannel(request, true, 0);

        // WHEN
        action.handleRequest(request, channel, client);

        // THEN query is marked as invalid
        assertThat(channel.responses().get(), equalTo(1));
        assertThat(channel.errors().get(), equalTo(0));
        assertThat(channel.capturedResponse().content().utf8ToString(), containsString("{\"valid\":false}"));
    }

    public void testRestValidateQueryAction_malformedQuery() throws Exception {
        // GIVEN an invalid query due to a malformed JSON
        final String content = "{malformed_json}";

        final RestRequest request = createRestRequest(content);
        final FakeRestChannel channel = new FakeRestChannel(request, true, 0);

        // WHEN
        action.handleRequest(request, channel, client);

        // THEN query is marked as invalid
        assertThat(channel.responses().get(), equalTo(1));
        assertThat(channel.errors().get(), equalTo(0));
        assertThat(channel.capturedResponse().content().utf8ToString(), containsString("{\"valid\":false}"));
    }

    private RestRequest createRestRequest(String content) {
        return new FakeRestRequest.Builder(xContentRegistry()).withPath("index1/type1/_validate/query")
            .withParams(emptyMap())
            .withContent(new BytesArray(content), XContentType.JSON)
            .build();
    }
}
