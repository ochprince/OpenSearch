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

package com.colasoft.opensearch.extensions.rest;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import org.junit.After;
import org.junit.Before;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.network.NetworkService;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.transport.TransportAddress;
import com.colasoft.opensearch.common.util.PageCacheRecycler;
import com.colasoft.opensearch.extensions.DiscoveryExtensionNode;
import com.colasoft.opensearch.indices.breaker.NoneCircuitBreakerService;
import com.colasoft.opensearch.rest.RestHandler.Route;
import com.colasoft.opensearch.rest.RestRequest.Method;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.test.transport.MockTransportService;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;
import com.colasoft.opensearch.transport.nio.MockNioTransport;

public class RestSendToExtensionActionTests extends OpenSearchTestCase {

    private TransportService transportService;
    private MockNioTransport transport;
    private DiscoveryExtensionNode discoveryExtensionNode;
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
            Version.CURRENT,
            Version.CURRENT,
            Collections.emptyList()
        );
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        transportService.close();
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
    }

    public void testRestSendToExtensionAction() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("GET /foo", "PUT /bar", "POST /baz"),
            List.of("GET /deprecated/foo", "It's deprecated!")
        );
        RestSendToExtensionAction restSendToExtensionAction = new RestSendToExtensionAction(
            registerRestActionRequest,
            discoveryExtensionNode,
            transportService
        );

        assertEquals("send_to_extension_action", restSendToExtensionAction.getName());
        List<Route> expected = new ArrayList<>();
        String uriPrefix = "/_extensions/_uniqueid1";
        expected.add(new Route(Method.GET, uriPrefix + "/foo"));
        expected.add(new Route(Method.PUT, uriPrefix + "/bar"));
        expected.add(new Route(Method.POST, uriPrefix + "/baz"));

        List<Route> routes = restSendToExtensionAction.routes();
        assertEquals(expected.size(), routes.size());
        List<String> expectedPaths = expected.stream().map(Route::getPath).collect(Collectors.toList());
        List<String> paths = routes.stream().map(Route::getPath).collect(Collectors.toList());
        List<Method> expectedMethods = expected.stream().map(Route::getMethod).collect(Collectors.toList());
        List<Method> methods = routes.stream().map(Route::getMethod).collect(Collectors.toList());
        assertTrue(paths.containsAll(expectedPaths));
        assertTrue(expectedPaths.containsAll(paths));
        assertTrue(methods.containsAll(expectedMethods));
        assertTrue(expectedMethods.containsAll(methods));
    }

    public void testRestSendToExtensionActionFilterHeaders() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("GET /foo", "PUT /bar", "POST /baz"),
            List.of("GET /deprecated/foo", "It's deprecated!")
        );
        RestSendToExtensionAction restSendToExtensionAction = new RestSendToExtensionAction(
            registerRestActionRequest,
            discoveryExtensionNode,
            transportService
        );

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        headers.put("Authorization", Arrays.asList("Bearer token"));
        headers.put("Proxy-Authorization", Arrays.asList("Basic credentials"));

        Set<String> allowList = Set.of("Content-Type"); // allowed headers
        Set<String> denyList = Set.of("Authorization", "Proxy-Authorization"); // denied headers

        Map<String, List<String>> filteredHeaders = restSendToExtensionAction.filterHeaders(headers, allowList, denyList);

        assertTrue(filteredHeaders.containsKey("Content-Type"));
        assertFalse(filteredHeaders.containsKey("Authorization"));
        assertFalse(filteredHeaders.containsKey("Proxy-Authorization"));
    }

    public void testRestSendToExtensionActionBadMethod() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("/foo", "PUT /bar", "POST /baz"),
            List.of("GET /deprecated/foo", "It's deprecated!")
        );
        expectThrows(
            IllegalArgumentException.class,
            () -> new RestSendToExtensionAction(registerRestActionRequest, discoveryExtensionNode, transportService)
        );
    }

    public void testRestSendToExtensionActionBadDeprecatedMethod() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("GET /foo", "PUT /bar", "POST /baz"),
            List.of("/deprecated/foo", "It's deprecated!")
        );
        expectThrows(
            IllegalArgumentException.class,
            () -> new RestSendToExtensionAction(registerRestActionRequest, discoveryExtensionNode, transportService)
        );
    }

    public void testRestSendToExtensionActionMissingUri() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("GET", "PUT /bar", "POST /baz"),
            List.of("GET /deprecated/foo", "It's deprecated!")
        );
        expectThrows(
            IllegalArgumentException.class,
            () -> new RestSendToExtensionAction(registerRestActionRequest, discoveryExtensionNode, transportService)
        );
    }

    public void testRestSendToExtensionActionMissingDeprecatedUri() throws Exception {
        RegisterRestActionsRequest registerRestActionRequest = new RegisterRestActionsRequest(
            "uniqueid1",
            List.of("GET /foo", "PUT /bar", "POST /baz"),
            List.of("GET", "It's deprecated!")
        );
        expectThrows(
            IllegalArgumentException.class,
            () -> new RestSendToExtensionAction(registerRestActionRequest, discoveryExtensionNode, transportService)
        );
    }
}
