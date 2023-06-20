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

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodeInfo;
import com.colasoft.opensearch.action.admin.cluster.node.info.NodesInfoRequest;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.clustermanager.TransportClusterManagerNodeAction;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.client.OriginSettingClient;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.search.pipeline.SearchPipelineService;
import com.colasoft.opensearch.search.pipeline.SearchPipelineInfo;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.colasoft.opensearch.search.pipeline.SearchPipelineService.SEARCH_PIPELINE_ORIGIN;

/**
 * Perform the action of putting a search pipeline
 *
 * @opensearch.internal
 */
public class PutSearchPipelineTransportAction extends TransportClusterManagerNodeAction<PutSearchPipelineRequest, AcknowledgedResponse> {

    private final SearchPipelineService searchPipelineService;
    private final OriginSettingClient client;

    @Inject
    public PutSearchPipelineTransportAction(
        ThreadPool threadPool,
        TransportService transportService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        SearchPipelineService searchPipelineService,
        NodeClient client
    ) {
        super(
            PutSearchPipelineAction.NAME,
            transportService,
            searchPipelineService.getClusterService(),
            threadPool,
            actionFilters,
            PutSearchPipelineRequest::new,
            indexNameExpressionResolver
        );
        this.client = new OriginSettingClient(client, SEARCH_PIPELINE_ORIGIN);
        this.searchPipelineService = searchPipelineService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.SAME;
    }

    @Override
    protected AcknowledgedResponse read(StreamInput in) throws IOException {
        return new AcknowledgedResponse(in);
    }

    @Override
    protected void clusterManagerOperation(
        PutSearchPipelineRequest request,
        ClusterState state,
        ActionListener<AcknowledgedResponse> listener
    ) throws Exception {
        NodesInfoRequest nodesInfoRequest = new NodesInfoRequest();
        client.admin().cluster().nodesInfo(nodesInfoRequest, ActionListener.wrap(nodeInfos -> {
            Map<DiscoveryNode, SearchPipelineInfo> searchPipelineInfos = new HashMap<>();
            for (NodeInfo nodeInfo : nodeInfos.getNodes()) {
                searchPipelineInfos.put(nodeInfo.getNode(), nodeInfo.getInfo(SearchPipelineInfo.class));
            }
            searchPipelineService.putPipeline(searchPipelineInfos, request, listener);
        }, listener::onFailure));
    }

    @Override
    protected ClusterBlockException checkBlock(PutSearchPipelineRequest request, ClusterState state) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }
}
