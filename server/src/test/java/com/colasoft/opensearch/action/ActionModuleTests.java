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

package com.colasoft.opensearch.action;

import com.colasoft.opensearch.action.ActionModule.DynamicActionRegistry;
import com.colasoft.opensearch.action.main.MainAction;
import com.colasoft.opensearch.action.main.TransportMainAction;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.TransportAction;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.Writeable.Reader;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.common.settings.SettingsModule;
import com.colasoft.opensearch.common.util.concurrent.ThreadContext;
import com.colasoft.opensearch.extensions.action.ExtensionAction;
import com.colasoft.opensearch.extensions.action.ExtensionTransportAction;
import com.colasoft.opensearch.plugins.ActionPlugin;
import com.colasoft.opensearch.plugins.ActionPlugin.ActionHandler;

import com.colasoft.opensearch.rest.RestChannel;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestRequest.Method;
import com.colasoft.opensearch.rest.action.RestMainAction;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.tasks.TaskManager;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.usage.UsageService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.startsWith;

public class ActionModuleTests extends OpenSearchTestCase {
    public void testSetupActionsContainsKnownBuiltin() {
        assertThat(
            ActionModule.setupActions(emptyList()),
            hasEntry(MainAction.INSTANCE.name(), new ActionHandler<>(MainAction.INSTANCE, TransportMainAction.class))
        );
    }

    public void testPluginCantOverwriteBuiltinAction() {
        ActionPlugin dupsMainAction = new ActionPlugin() {
            @Override
            public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
                return singletonList(new ActionHandler<>(MainAction.INSTANCE, TransportMainAction.class));
            }
        };
        Exception e = expectThrows(IllegalArgumentException.class, () -> ActionModule.setupActions(singletonList(dupsMainAction)));
        assertEquals("action for name [" + MainAction.NAME + "] already registered", e.getMessage());
    }

    public void testPluginCanRegisterAction() {
        class FakeRequest extends ActionRequest {
            @Override
            public ActionRequestValidationException validate() {
                return null;
            }
        }
        class FakeTransportAction extends TransportAction<FakeRequest, ActionResponse> {
            protected FakeTransportAction(String actionName, ActionFilters actionFilters, TaskManager taskManager) {
                super(actionName, actionFilters, taskManager);
            }

            @Override
            protected void doExecute(Task task, FakeRequest request, ActionListener<ActionResponse> listener) {}
        }
        class FakeAction extends ActionType<ActionResponse> {
            protected FakeAction() {
                super("fake", null);
            }
        }
        FakeAction action = new FakeAction();
        ActionPlugin registersFakeAction = new ActionPlugin() {
            @Override
            public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
                return singletonList(new ActionHandler<>(action, FakeTransportAction.class));
            }
        };
        assertThat(
            ActionModule.setupActions(singletonList(registersFakeAction)),
            hasEntry("fake", new ActionHandler<>(action, FakeTransportAction.class))
        );
    }

    public void testSetupRestHandlerContainsKnownBuiltin() {
        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        UsageService usageService = new UsageService();
        ActionModule actionModule = new ActionModule(
            settings.getSettings(),
            new IndexNameExpressionResolver(new ThreadContext(Settings.EMPTY)),
            settings.getIndexScopedSettings(),
            settings.getClusterSettings(),
            settings.getSettingsFilter(),
            null,
            emptyList(),
            null,
            null,
            usageService,
            null
        );
        actionModule.initRestHandlers(null);
        // At this point the easiest way to confirm that a handler is loaded is to try to register another one on top of it and to fail
        Exception e = expectThrows(
            IllegalArgumentException.class,
            () -> actionModule.getRestController().registerHandler(new RestHandler() {
                @Override
                public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {}

                @Override
                public List<Route> routes() {
                    return singletonList(new Route(Method.GET, "/"));
                }
            })
        );
        assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/] for method: GET"));
    }

    public void testPluginCantOverwriteBuiltinRestHandler() throws IOException {
        ActionPlugin dupsMainAction = new ActionPlugin() {
            @Override
            public List<RestHandler> getRestHandlers(
                Settings settings,
                RestController restController,
                ClusterSettings clusterSettings,
                IndexScopedSettings indexScopedSettings,
                SettingsFilter settingsFilter,
                IndexNameExpressionResolver indexNameExpressionResolver,
                Supplier<DiscoveryNodes> nodesInCluster
            ) {
                return singletonList(new RestMainAction() {

                    @Override
                    public String getName() {
                        return "duplicated_" + super.getName();
                    }

                });
            }
        };
        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        ThreadPool threadPool = new TestThreadPool(getTestName());
        try {
            UsageService usageService = new UsageService();
            ActionModule actionModule = new ActionModule(
                settings.getSettings(),
                new IndexNameExpressionResolver(threadPool.getThreadContext()),
                settings.getIndexScopedSettings(),
                settings.getClusterSettings(),
                settings.getSettingsFilter(),
                threadPool,
                singletonList(dupsMainAction),
                null,
                null,
                usageService,
                null
            );
            Exception e = expectThrows(IllegalArgumentException.class, () -> actionModule.initRestHandlers(null));
            assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/] for method: GET"));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testPluginCanRegisterRestHandler() {
        class FakeHandler implements RestHandler {
            @Override
            public List<Route> routes() {
                return singletonList(new Route(Method.GET, "/_dummy"));
            }

            @Override
            public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {}
        }
        ActionPlugin registersFakeHandler = new ActionPlugin() {
            @Override
            public List<RestHandler> getRestHandlers(
                Settings settings,
                RestController restController,
                ClusterSettings clusterSettings,
                IndexScopedSettings indexScopedSettings,
                SettingsFilter settingsFilter,
                IndexNameExpressionResolver indexNameExpressionResolver,
                Supplier<DiscoveryNodes> nodesInCluster
            ) {
                return singletonList(new FakeHandler());
            }
        };

        SettingsModule settings = new SettingsModule(Settings.EMPTY);
        ThreadPool threadPool = new TestThreadPool(getTestName());
        try {
            UsageService usageService = new UsageService();
            ActionModule actionModule = new ActionModule(
                settings.getSettings(),
                new IndexNameExpressionResolver(threadPool.getThreadContext()),
                settings.getIndexScopedSettings(),
                settings.getClusterSettings(),
                settings.getSettingsFilter(),
                threadPool,
                singletonList(registersFakeHandler),
                null,
                null,
                usageService,
                null
            );
            actionModule.initRestHandlers(null);
            // At this point the easiest way to confirm that a handler is loaded is to try to register another one on top of it and to fail
            Exception e = expectThrows(
                IllegalArgumentException.class,
                () -> actionModule.getRestController().registerHandler(new RestHandler() {
                    @Override
                    public void handleRequest(RestRequest request, RestChannel channel, NodeClient client) throws Exception {}

                    @Override
                    public List<Route> routes() {
                        return singletonList(new Route(Method.GET, "/_dummy"));
                    }
                })
            );
            assertThat(e.getMessage(), startsWith("Cannot replace existing handler for [/_dummy] for method: GET"));
        } finally {
            threadPool.shutdown();
        }
    }

    public void testDynamicActionRegistry() {
        ActionFilters emptyFilters = new ActionFilters(Collections.emptySet());
        Map<ActionType, TransportAction> testMap = Map.of(TestAction.INSTANCE, new TestTransportAction("test-action", emptyFilters, null));

        DynamicActionRegistry dynamicActionRegistry = new DynamicActionRegistry();
        dynamicActionRegistry.registerUnmodifiableActionMap(testMap);

        // Should contain the immutable map entry
        assertNotNull(dynamicActionRegistry.get(TestAction.INSTANCE));
        // Should not contain anything not added
        assertNull(dynamicActionRegistry.get(MainAction.INSTANCE));

        // ExtensionsAction not yet registered
        ExtensionAction testExtensionAction = new ExtensionAction("extensionId", "actionName");
        ExtensionTransportAction testExtensionTransportAction = new ExtensionTransportAction("test-action", emptyFilters, null, null);
        assertNull(dynamicActionRegistry.get(testExtensionAction));

        // Register an extension action
        // Should insert without problem
        try {
            dynamicActionRegistry.registerDynamicAction(testExtensionAction, testExtensionTransportAction);
        } catch (Exception e) {
            fail("Should not have thrown exception registering action: " + e);
        }
        assertEquals(testExtensionTransportAction, dynamicActionRegistry.get(testExtensionAction));

        // Should fail inserting twice
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> dynamicActionRegistry.registerDynamicAction(testExtensionAction, testExtensionTransportAction)
        );
        assertEquals("action [actionName] already registered", ex.getMessage());
        // Should remove without problem
        try {
            dynamicActionRegistry.unregisterDynamicAction(testExtensionAction);
        } catch (Exception e) {
            fail("Should not have thrown exception unregistering action: " + e);
        }
        // Should have been removed
        assertNull(dynamicActionRegistry.get(testExtensionAction));

        // Should fail removing twice
        ex = assertThrows(IllegalArgumentException.class, () -> dynamicActionRegistry.unregisterDynamicAction(testExtensionAction));
        assertEquals("action [actionName] was not registered", ex.getMessage());
    }

    private static final class TestAction extends ActionType<ActionResponse> {
        public static final TestAction INSTANCE = new TestAction();

        private TestAction() {
            super("test-action", new Reader<ActionResponse>() {
                @Override
                public ActionResponse read(StreamInput in) throws IOException {
                    return null;
                }
            });
        }
    };

    private static final class TestTransportAction extends TransportAction<ActionRequest, ActionResponse> {
        protected TestTransportAction(String actionName, ActionFilters actionFilters, TaskManager taskManager) {
            super(actionName, actionFilters, taskManager);
        }

        @Override
        protected void doExecute(Task task, ActionRequest request, ActionListener<ActionResponse> listener) {}
    }
}
