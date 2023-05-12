/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.extensions.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.extensions.DiscoveryExtensionNode;
import com.colasoft.opensearch.extensions.ExtensionsManager;
import com.colasoft.opensearch.rest.BaseRestHandler;
import com.colasoft.opensearch.rest.BytesRestResponse;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestRequest.Method;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportException;
import com.colasoft.opensearch.transport.TransportResponseHandler;
import com.colasoft.opensearch.transport.TransportService;
import com.colasoft.opensearch.http.HttpRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;

/**
 * An action that forwards REST requests to an extension
 */
public class RestSendToExtensionAction extends BaseRestHandler {

    private static final String SEND_TO_EXTENSION_ACTION = "send_to_extension_action";
    private static final Logger logger = LogManager.getLogger(RestSendToExtensionAction.class);
    // To replace with user identity see https://github.com/opensearch-project/OpenSearch/pull/4247
    private static final Principal DEFAULT_PRINCIPAL = new Principal() {
        @Override
        public String getName() {
            return "OpenSearchUser";
        }
    };

    private final List<Route> routes;
    private final List<DeprecatedRoute> deprecatedRoutes;
    private final String pathPrefix;
    private final DiscoveryExtensionNode discoveryExtensionNode;
    private final TransportService transportService;

    private static final Set<String> allowList = Set.of("Content-Type");
    private static final Set<String> denyList = Set.of("Authorization", "Proxy-Authorization");

    /**
     * Instantiates this object using a {@link RegisterRestActionsRequest} to populate the routes.
     *
     * @param restActionsRequest A request encapsulating a list of Strings with the API methods and paths.
     * @param transportService The OpenSearch transport service
     * @param discoveryExtensionNode The extension node to which to send actions
     */
    public RestSendToExtensionAction(
        RegisterRestActionsRequest restActionsRequest,
        DiscoveryExtensionNode discoveryExtensionNode,
        TransportService transportService
    ) {
        this.pathPrefix = "/_extensions/_" + restActionsRequest.getUniqueId();
        RestRequest.Method method;
        String path;

        List<Route> restActionsAsRoutes = new ArrayList<>();
        for (String restAction : restActionsRequest.getRestActions()) {
            int delim = restAction.indexOf(' ');
            try {
                method = RestRequest.Method.valueOf(restAction.substring(0, delim));
                path = pathPrefix + restAction.substring(delim).trim();
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new IllegalArgumentException(restAction + " does not begin with a valid REST method");
            }
            logger.info("Registering: " + method + " " + path);
            restActionsAsRoutes.add(new Route(method, path));
        }
        this.routes = unmodifiableList(restActionsAsRoutes);

        List<DeprecatedRoute> restActionsAsDeprecatedRoutes = new ArrayList<>();
        // Iterate in pairs of route / deprecation message
        List<String> deprecatedActions = restActionsRequest.getDeprecatedRestActions();
        for (int i = 0; i < deprecatedActions.size() - 1; i += 2) {
            String restAction = deprecatedActions.get(i);
            String message = deprecatedActions.get(i + 1);
            int delim = restAction.indexOf(' ');
            try {
                method = RestRequest.Method.valueOf(restAction.substring(0, delim));
                path = pathPrefix + restAction.substring(delim).trim();
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new IllegalArgumentException(restAction + " does not begin with a valid REST method");
            }
            logger.info("Registering: " + method + " " + path + " with deprecation message " + message);
            restActionsAsDeprecatedRoutes.add(new DeprecatedRoute(method, path, message));
        }
        this.deprecatedRoutes = unmodifiableList(restActionsAsDeprecatedRoutes);

        this.discoveryExtensionNode = discoveryExtensionNode;
        this.transportService = transportService;
    }

    @Override
    public String getName() {
        return SEND_TO_EXTENSION_ACTION;
    }

    @Override
    public List<Route> routes() {
        return this.routes;
    }

    @Override
    public List<DeprecatedRoute> deprecatedRoutes() {
        return this.deprecatedRoutes;
    }

    public Map<String, List<String>> filterHeaders(Map<String, List<String>> headers, Set<String> allowList, Set<String> denyList) {
        Map<String, List<String>> filteredHeaders = headers.entrySet()
            .stream()
            .filter(e -> !denyList.contains(e.getKey()))
            .filter(e -> allowList.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return filteredHeaders;
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        HttpRequest httpRequest = request.getHttpRequest();
        String path = request.path();
        Method method = request.method();
        String uri = httpRequest.uri();
        Map<String, String> params = request.params();
        Map<String, List<String>> headers = request.getHeaders();
        XContentType contentType = request.getXContentType();
        BytesReference content = request.content();
        HttpRequest.HttpVersion httpVersion = httpRequest.protocolVersion();

        if (path.startsWith(pathPrefix)) {
            path = path.substring(pathPrefix.length());
        }
        String message = "Forwarding the request " + method + " " + path + " to " + discoveryExtensionNode;
        logger.info(message);
        // Initialize response. Values will be changed in the handler.
        final RestExecuteOnExtensionResponse restExecuteOnExtensionResponse = new RestExecuteOnExtensionResponse(
            RestStatus.INTERNAL_SERVER_ERROR,
            BytesRestResponse.TEXT_CONTENT_TYPE,
            message.getBytes(StandardCharsets.UTF_8),
            emptyMap(),
            emptyList(),
            false
        );
        final CompletableFuture<RestExecuteOnExtensionResponse> inProgressFuture = new CompletableFuture<>();
        final TransportResponseHandler<RestExecuteOnExtensionResponse> restExecuteOnExtensionResponseHandler = new TransportResponseHandler<
            RestExecuteOnExtensionResponse>() {

            @Override
            public RestExecuteOnExtensionResponse read(StreamInput in) throws IOException {
                return new RestExecuteOnExtensionResponse(in);
            }

            @Override
            public void handleResponse(RestExecuteOnExtensionResponse response) {
                logger.info("Received response from extension: {}", response.getStatus());
                restExecuteOnExtensionResponse.setStatus(response.getStatus());
                restExecuteOnExtensionResponse.setContentType(response.getContentType());
                restExecuteOnExtensionResponse.setContent(response.getContent());
                restExecuteOnExtensionResponse.setHeaders(response.getHeaders());
                // Consume parameters and content
                response.getConsumedParams().stream().forEach(p -> request.param(p));
                if (response.isContentConsumed()) {
                    request.content();
                }
                inProgressFuture.complete(response);
            }

            @Override
            public void handleException(TransportException exp) {
                logger.debug("REST request failed", exp);
                inProgressFuture.completeExceptionally(exp);
            }

            @Override
            public String executor() {
                return ThreadPool.Names.GENERIC;
            }
        };

        try {
            // Will be replaced with ExtensionTokenProcessor and PrincipalIdentifierToken classes from feature/identity
            final String extensionTokenProcessor = "placeholder_token_processor";
            final String requestIssuerIdentity = "placeholder_request_issuer_identity";

            Map<String, List<String>> filteredHeaders = filterHeaders(headers, allowList, denyList);

            transportService.sendRequest(
                discoveryExtensionNode,
                ExtensionsManager.REQUEST_REST_EXECUTE_ON_EXTENSION_ACTION,
                // DO NOT INCLUDE HEADERS WITH SECURITY OR PRIVACY INFORMATION
                // SEE https://github.com/opensearch-project/OpenSearch/issues/4429
                new ExtensionRestRequest(
                    method,
                    uri,
                    path,
                    params,
                    filteredHeaders,
                    contentType,
                    content,
                    requestIssuerIdentity,
                    httpVersion
                ),
                restExecuteOnExtensionResponseHandler
            );
            inProgressFuture.orTimeout(ExtensionsManager.EXTENSION_REQUEST_WAIT_TIMEOUT, TimeUnit.SECONDS).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                return channel -> channel.sendResponse(
                    new BytesRestResponse(RestStatus.REQUEST_TIMEOUT, "No response from extension to request.")
                );
            }
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } catch (Exception ex) {
            logger.info("Failed to send REST Actions to extension " + discoveryExtensionNode.getName(), ex);
            return channel -> channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
        }
        BytesRestResponse restResponse = new BytesRestResponse(
            restExecuteOnExtensionResponse.getStatus(),
            restExecuteOnExtensionResponse.getContentType(),
            restExecuteOnExtensionResponse.getContent()
        );
        // No constructor that includes headers so we roll our own
        restExecuteOnExtensionResponse.getHeaders().entrySet().stream().forEach(e -> {
            e.getValue().stream().forEach(v -> restResponse.addHeader(e.getKey(), v));
        });

        return channel -> channel.sendResponse(restResponse);
    }
}
