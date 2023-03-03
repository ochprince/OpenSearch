/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package com.colasoft.opensearch.action.admin.indices.segments;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.search.ListPitInfo;
import com.colasoft.opensearch.action.search.PitService;
import com.colasoft.opensearch.action.search.SearchContextId;
import com.colasoft.opensearch.action.search.SearchContextIdForNode;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.DefaultShardOperationFailedException;
import com.colasoft.opensearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.block.ClusterBlockException;
import com.colasoft.opensearch.cluster.block.ClusterBlockLevel;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.routing.AllocationId;
import com.colasoft.opensearch.cluster.routing.PlainShardsIterator;
import com.colasoft.opensearch.cluster.routing.RecoverySource;
import com.colasoft.opensearch.cluster.routing.ShardRouting;
import com.colasoft.opensearch.cluster.routing.ShardRoutingState;
import com.colasoft.opensearch.cluster.routing.ShardsIterator;
import com.colasoft.opensearch.cluster.routing.UnassignedInfo;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.xcontent.XContentBuilder;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.indices.IndicesService;
import com.colasoft.opensearch.search.SearchService;
import com.colasoft.opensearch.search.internal.PitReaderContext;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.colasoft.opensearch.action.search.SearchContextId.decode;

/**
 * Transport action for retrieving segment information of PITs
 */
public class TransportPitSegmentsAction extends TransportBroadcastByNodeAction<PitSegmentsRequest, IndicesSegmentResponse, ShardSegments> {
    private final ClusterService clusterService;
    private final IndicesService indicesService;
    private final SearchService searchService;
    private final NamedWriteableRegistry namedWriteableRegistry;
    private final TransportService transportService;
    private final PitService pitService;

    @Inject
    public TransportPitSegmentsAction(
        ClusterService clusterService,
        TransportService transportService,
        IndicesService indicesService,
        ActionFilters actionFilters,
        IndexNameExpressionResolver indexNameExpressionResolver,
        SearchService searchService,
        NamedWriteableRegistry namedWriteableRegistry,
        PitService pitService
    ) {
        super(
            PitSegmentsAction.NAME,
            clusterService,
            transportService,
            actionFilters,
            indexNameExpressionResolver,
            PitSegmentsRequest::new,
            ThreadPool.Names.MANAGEMENT
        );
        this.clusterService = clusterService;
        this.indicesService = indicesService;
        this.searchService = searchService;
        this.namedWriteableRegistry = namedWriteableRegistry;
        this.transportService = transportService;
        this.pitService = pitService;
    }

    /**
     * Execute PIT segments flow for all PITs or request PIT IDs
     */
    @Override
    protected void doExecute(Task task, PitSegmentsRequest request, ActionListener<IndicesSegmentResponse> listener) {
        List<String> pitIds = request.getPitIds();
        if (pitIds.size() == 1 && "_all".equals(pitIds.get(0))) {
            pitService.getAllPits(ActionListener.wrap(response -> {
                request.clearAndSetPitIds(response.getPitInfos().stream().map(ListPitInfo::getPitId).collect(Collectors.toList()));
                super.doExecute(task, request, listener);
            }, listener::onFailure));
        } else {
            super.doExecute(task, request, listener);
        }
    }

    /**
     * This adds list of shards on which we need to retrieve pit segments details
     * @param clusterState    the cluster state
     * @param request         the underlying request
     * @param concreteIndices the concrete indices on which to execute the operation
     */
    @Override
    protected ShardsIterator shards(ClusterState clusterState, PitSegmentsRequest request, String[] concreteIndices) {
        final ArrayList<ShardRouting> iterators = new ArrayList<>();
        for (String pitId : request.getPitIds()) {
            SearchContextId searchContext = decode(namedWriteableRegistry, pitId);
            for (Map.Entry<ShardId, SearchContextIdForNode> entry : searchContext.shards().entrySet()) {
                final SearchContextIdForNode perNode = entry.getValue();
                // check if node is part of local cluster
                if (Strings.isEmpty(perNode.getClusterAlias())) {
                    final ShardId shardId = entry.getKey();
                    iterators.add(
                        new PitAwareShardRouting(
                            pitId,
                            shardId,
                            perNode.getNode(),
                            null,
                            true,
                            ShardRoutingState.STARTED,
                            null,
                            null,
                            null,
                            -1L
                        )
                    );
                }
            }
        }
        return new PlainShardsIterator(iterators);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, PitSegmentsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, PitSegmentsRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }

    @Override
    protected ShardSegments readShardResult(StreamInput in) throws IOException {
        return new ShardSegments(in);
    }

    @Override
    protected IndicesSegmentResponse newResponse(
        PitSegmentsRequest request,
        int totalShards,
        int successfulShards,
        int failedShards,
        List<ShardSegments> results,
        List<DefaultShardOperationFailedException> shardFailures,
        ClusterState clusterState
    ) {
        return new IndicesSegmentResponse(
            results.toArray(new ShardSegments[results.size()]),
            totalShards,
            successfulShards,
            failedShards,
            shardFailures
        );
    }

    @Override
    protected PitSegmentsRequest readRequestFrom(StreamInput in) throws IOException {
        return new PitSegmentsRequest(in);
    }

    @Override
    public List<ShardRouting> getShardRoutingsFromInputStream(StreamInput in) throws IOException {
        return in.readList(PitAwareShardRouting::new);
    }

    /**
     * This retrieves segment details of PIT context
     * @param request      the node-level request
     * @param shardRouting the shard on which to execute the operation
     */
    @Override
    protected ShardSegments shardOperation(PitSegmentsRequest request, ShardRouting shardRouting) {
        assert shardRouting instanceof PitAwareShardRouting;
        PitAwareShardRouting pitAwareShardRouting = (PitAwareShardRouting) shardRouting;
        SearchContextIdForNode searchContextIdForNode = decode(namedWriteableRegistry, pitAwareShardRouting.getPitId()).shards()
            .get(shardRouting.shardId());
        PitReaderContext pitReaderContext = searchService.getPitReaderContext(searchContextIdForNode.getSearchContextId());
        if (pitReaderContext == null) {
            return new ShardSegments(shardRouting, Collections.emptyList());
        }
        return new ShardSegments(pitReaderContext.getShardRouting(), pitReaderContext.getSegments());
    }

    /**
     * This holds PIT id which is used to perform broadcast operation in PIT shards to retrieve segments information
     */
    public class PitAwareShardRouting extends ShardRouting {

        private final String pitId;

        public PitAwareShardRouting(StreamInput in) throws IOException {
            super(in);
            this.pitId = in.readString();
        }

        public PitAwareShardRouting(
            String pitId,
            ShardId shardId,
            String currentNodeId,
            String relocatingNodeId,
            boolean primary,
            ShardRoutingState state,
            RecoverySource recoverySource,
            UnassignedInfo unassignedInfo,
            AllocationId allocationId,
            long expectedShardSize
        ) {
            super(
                shardId,
                currentNodeId,
                relocatingNodeId,
                primary,
                state,
                recoverySource,
                unassignedInfo,
                allocationId,
                expectedShardSize
            );
            this.pitId = pitId;
        }

        public String getPitId() {
            return pitId;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(pitId);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            super.toXContent(builder, params);
            builder.field("pit_id", pitId);
            return builder.endObject();
        }
    }
}
