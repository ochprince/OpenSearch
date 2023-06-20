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

package com.colasoft.opensearch.extensions.action;

import org.junit.After;
import org.junit.Before;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.ActionModule;
import com.colasoft.opensearch.action.ActionModule.DynamicActionRegistry;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.transport.TransportAddress;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.extensions.DiscoveryExtensionNode;
import com.colasoft.opensearch.extensions.AcknowledgedResponse;
import com.colasoft.opensearch.extensions.rest.RestSendToExtensionActionTests;
import com.colasoft.opensearch.indices.breaker.NoneCircuitBreakerService;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.client.NoOpNodeClient;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.ActionNotFoundTransportException;
import com.colasoft.opensearch.transport.NodeNotConnectedException;
import com.colasoft.opensearch.transport.TransportService;
import com.colasoft.opensearch.transport.nio.MockNioTransport;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

public class ExtensionTransportActionsHandlerTests extends OpenSearchTestCase {
    private static final ActionFilters EMPTY_FILTERS = new ActionFilters(Collections.emptySet());
    private TransportService transportService;
    private MockNioTransport transport;
    private DiscoveryExtensionNode discoveryExtensionNode;
    private ExtensionTransportActionsHandler extensionTransportActionsHandler;
    private NodeClient client;
    private final ThreadPool threadPool = new TestThreadPool(RestSendToExtensionActionTests.class.getSimpleName());

    @Before
    public void setup() throws Exception {
        Settings settings = Settings.builder().put("cluster.name", "test").build();
        transport = new MockNioTransport(
            settings,
            Version.CURRENT,
            threadPool,
            new NetworkService(Collections.emptyList()),
            PageCacheRecycler.NON_RECYCLING_INSTANCE,
            new NamedWriteableRegistry(Collections.emptyList()),
            new NoneCircuitBreakerService()
        );
        transportService = new MockTransportService(
            settings,
            transport,
            threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            (boundAddress) -> new DiscoveryNode(
                "test_node",
                "test_node",
                boundAddress.publishAddress(),
                emptyMap(),
                emptySet(),
                Version.CURRENT
            ),
            null,
            Collections.emptySet()
        );
        discoveryExtensionNode = new DiscoveryExtensionNode(
            "firstExtension",
            "uniqueid1",
            new TransportAddress(InetAddress.getByName("127.0.0.0"), 9300),
            new HashMap<String, String>(),
            Version.fromString("3.0.0"),
            Version.CURRENT,
            Collections.emptyList()
        );
        client = new NoOpNodeClient(this.getTestName());
        ActionModule mockActionModule = mock(ActionModule.class);
        DynamicActionRegistry dynamicActionRegistry = new DynamicActionRegistry();
        dynamicActionRegistry.registerUnmodifiableActionMap(Collections.emptyMap());
        when(mockActionModule.getDynamicActionRegistry()).thenReturn(dynamicActionRegistry);
        when(mockActionModule.getActionFilters()).thenReturn(EMPTY_FILTERS);
        extensionTransportActionsHandler = new ExtensionTransportActionsHandler(
            Map.of("uniqueid1", discoveryExtensionNode),
            transportService,
            client,
            mockActionModule,
            null
        );
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        transportService.close();
        client.close();
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    public void testRegisterAction() {
        String action = "test-action";
        extensionTransportActionsHandler.registerAction(action, discoveryExtensionNode.getId());
        assertEquals(discoveryExtensionNode, extensionTransportActionsHandler.getExtension(action));

        // Test duplicate action registration
        expectThrows(
            IllegalArgumentException.class,
            () -> extensionTransportActionsHandler.registerAction(action, discoveryExtensionNode.getId())
        );
        assertEquals(discoveryExtensionNode, extensionTransportActionsHandler.getExtension(action));
    }

    public void testRegisterTransportActionsRequest() {
        String action = "test-action";
        RegisterTransportActionsRequest request = new RegisterTransportActionsRequest("uniqueid1", Set.of(action));
        AcknowledgedResponse response = (AcknowledgedResponse) extensionTransportActionsHandler.handleRegisterTransportActionsRequest(
            request
        );
        assertTrue(response.getStatus());
        assertEquals(discoveryExtensionNode, extensionTransportActionsHandler.getExtension(action));

        // Test duplicate action registration
        response = (AcknowledgedResponse) extensionTransportActionsHandler.handleRegisterTransportActionsRequest(request);
        assertFalse(response.getStatus());
    }

    public void testTransportActionRequestFromExtension() throws Exception {
        String action = "test-action";
        byte[] requestBytes = "requestBytes".getBytes(StandardCharsets.UTF_8);
        TransportActionRequestFromExtension request = new TransportActionRequestFromExtension(action, requestBytes, "uniqueid1");
        RemoteExtensionActionResponse response = extensionTransportActionsHandler.handleTransportActionRequestFromExtension(request);
        assertFalse(response.isSuccess());
        String responseString = response.getResponseBytesAsString();
        assertEquals("Request failed: action [test-action] is not registered for any extension.", responseString);
    }

    public void testSendTransportRequestToExtension() throws InterruptedException {
        String action = "test-action";
        byte[] requestBytes = "request-bytes".getBytes(StandardCharsets.UTF_8);
        ExtensionActionRequest request = new ExtensionActionRequest(action, requestBytes);

        // Action not registered, expect exception
        expectThrows(
            ActionNotFoundTransportException.class,
            () -> extensionTransportActionsHandler.sendTransportRequestToExtension(request)
        );

        // Register Action
        RegisterTransportActionsRequest registerRequest = new RegisterTransportActionsRequest("uniqueid1", Set.of(action));
        AcknowledgedResponse response = (AcknowledgedResponse) extensionTransportActionsHandler.handleRegisterTransportActionsRequest(
            registerRequest
        );
        assertTrue(response.getStatus());

        expectThrows(NodeNotConnectedException.class, () -> extensionTransportActionsHandler.sendTransportRequestToExtension(request));
    }
}
