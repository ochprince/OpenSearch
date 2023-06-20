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

package com.colasoft.opensearch.indices.recovery;

import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.ExceptionsHelper;
import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.ActionListenerResponseHandler;
import com.colasoft.opensearch.action.support.RetryableAction;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.breaker.CircuitBreakingException;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.common.util.CancellableThreads;
import com.colasoft.opensearch.common.util.concurrent.ConcurrentCollections;
import com.colasoft.opensearch.common.util.concurrent.OpenSearchRejectedExecutionException;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.ConnectTransportException;
import com.colasoft.opensearch.transport.RemoteTransportException;
import com.colasoft.opensearch.transport.SendRequestTransportException;
import com.colasoft.opensearch.transport.TransportRequest;
import com.colasoft.opensearch.transport.TransportRequestOptions;
import com.colasoft.opensearch.transport.TransportResponse;
import com.colasoft.opensearch.transport.TransportService;

import java.util.Map;

/**
 * Client that implements retry functionality for transport layer requests.
 *
 * @opensearch.internal
 */
public final class RetryableTransportClient {

    private final ThreadPool threadPool;
    private final Map<Object, RetryableAction<?>> onGoingRetryableActions = ConcurrentCollections.newConcurrentMap();
    private volatile boolean isCancelled = false;
    private final TransportService transportService;
    private final TimeValue retryTimeout;
    private final DiscoveryNode targetNode;

    private final Logger logger;

    public RetryableTransportClient(TransportService transportService, DiscoveryNode targetNode, TimeValue retryTimeout, Logger logger) {
        this.threadPool = transportService.getThreadPool();
        this.transportService = transportService;
        this.retryTimeout = retryTimeout;
        this.targetNode = targetNode;
        this.logger = logger;
    }

    /**
     * Execute a retryable action.
     * @param action {@link String} Action Name.
     * @param request {@link TransportRequest} Transport request to execute.
     * @param actionListener {@link ActionListener} Listener to complete
     * @param reader {@link Writeable.Reader} Reader to read the response stream.
     * @param <T> {@link TransportResponse} type.
     */
    public <T extends TransportResponse> void executeRetryableAction(
        String action,
        TransportRequest request,
        ActionListener<T> actionListener,
        Writeable.Reader<T> reader
    ) {
        final TransportRequestOptions options = TransportRequestOptions.builder().withTimeout(retryTimeout).build();
        executeRetryableAction(action, request, options, actionListener, reader);
    }

    public <T extends TransportResponse> void executeRetryableAction(
        String action,
        TransportRequest request,
        TransportRequestOptions options,
        ActionListener<T> actionListener,
        Writeable.Reader<T> reader
    ) {
        final Object key = new Object();
        final ActionListener<T> removeListener = ActionListener.runBefore(actionListener, () -> onGoingRetryableActions.remove(key));
        final TimeValue initialDelay = TimeValue.timeValueMillis(200);
        final RetryableAction<T> retryableAction = new RetryableAction<T>(logger, threadPool, initialDelay, retryTimeout, removeListener) {

            @Override
            public void tryAction(ActionListener<T> listener) {
                transportService.sendRequest(
                    targetNode,
                    action,
                    request,
                    options,
                    new ActionListenerResponseHandler<>(listener, reader, ThreadPool.Names.GENERIC)
                );
            }

            @Override
            public boolean shouldRetry(Exception e) {
                return targetNode.getVersion().onOrAfter(LegacyESVersion.V_7_9_0) && retryableException(e);
            }
        };
        onGoingRetryableActions.put(key, retryableAction);
        retryableAction.run();
        if (isCancelled) {
            retryableAction.cancel(new CancellableThreads.ExecutionCancelledException("retryable action was cancelled"));
        }
    }

    public void cancel() {
        isCancelled = true;
        if (onGoingRetryableActions.isEmpty()) {
            return;
        }
        final RuntimeException exception = new CancellableThreads.ExecutionCancelledException("retryable action was cancelled");
        // Dispatch to generic as cancellation calls can come on the cluster state applier thread
        threadPool.generic().execute(() -> {
            for (RetryableAction<?> action : onGoingRetryableActions.values()) {
                action.cancel(exception);
            }
            onGoingRetryableActions.clear();
        });
    }

    private static boolean retryableException(Exception e) {
        if (e instanceof ConnectTransportException) {
            return true;
        } else if (e instanceof SendRequestTransportException) {
            final Throwable cause = ExceptionsHelper.unwrapCause(e);
            return cause instanceof ConnectTransportException;
        } else if (e instanceof RemoteTransportException) {
            final Throwable cause = ExceptionsHelper.unwrapCause(e);
            return cause instanceof CircuitBreakingException || cause instanceof OpenSearchRejectedExecutionException;
        }
        return false;
    }
}
