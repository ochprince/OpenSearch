/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.extensions.action;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.TransportAction;
import com.colasoft.opensearch.extensions.ExtensionsManager;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.tasks.TaskManager;

/**
 * A proxy transport action used to proxy a transport request from an extension to execute on another extension
 *
 * @opensearch.internal
 */
public class ExtensionTransportAction extends TransportAction<ExtensionActionRequest, RemoteExtensionActionResponse> {

    private final ExtensionsManager extensionsManager;

    public ExtensionTransportAction(
        String actionName,
        ActionFilters actionFilters,
        TaskManager taskManager,
        ExtensionsManager extensionsManager
    ) {
        super(actionName, actionFilters, taskManager);
        this.extensionsManager = extensionsManager;
    }

    @Override
    protected void doExecute(Task task, ExtensionActionRequest request, ActionListener<RemoteExtensionActionResponse> listener) {
        try {
            listener.onResponse(extensionsManager.handleRemoteTransportRequest(request));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
