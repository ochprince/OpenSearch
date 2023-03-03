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
import com.colasoft.opensearch.action.support.HandledTransportAction;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.extensions.ExtensionsManager;
import com.colasoft.opensearch.node.Node;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.transport.TransportService;

/**
 * The main proxy transport action used to proxy a transport request from extension to another extension
 *
 * @opensearch.internal
 */
public class ExtensionTransportAction extends HandledTransportAction<ExtensionActionRequest, ExtensionActionResponse> {

    private final String nodeName;
    private final ClusterService clusterService;
    private final ExtensionsManager extensionsManager;

    @Inject
    public ExtensionTransportAction(
        Settings settings,
        TransportService transportService,
        ActionFilters actionFilters,
        ClusterService clusterService,
        ExtensionsManager extensionsManager
    ) {
        super(ExtensionProxyAction.NAME, transportService, actionFilters, ExtensionActionRequest::new);
        this.nodeName = Node.NODE_NAME_SETTING.get(settings);
        this.clusterService = clusterService;
        this.extensionsManager = extensionsManager;
    }

    @Override
    protected void doExecute(Task task, ExtensionActionRequest request, ActionListener<ExtensionActionResponse> listener) {
        try {
            listener.onResponse(extensionsManager.handleTransportRequest(request));
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }
}
