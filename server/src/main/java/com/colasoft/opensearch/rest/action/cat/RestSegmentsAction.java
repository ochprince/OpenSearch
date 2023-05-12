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

package com.colasoft.opensearch.rest.action.cat;

import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateRequest;
import com.colasoft.opensearch.action.admin.cluster.state.ClusterStateResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndexSegments;
import com.colasoft.opensearch.action.admin.indices.segments.IndexShardSegments;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentResponse;
import com.colasoft.opensearch.action.admin.indices.segments.IndicesSegmentsRequest;
import com.colasoft.opensearch.action.admin.indices.segments.ShardSegments;
import com.colasoft.opensearch.client.node.NodeClient;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.Table;
import com.colasoft.opensearch.common.logging.DeprecationLogger;
import com.colasoft.opensearch.index.engine.Segment;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.rest.RestResponse;
import com.colasoft.opensearch.rest.action.RestActionListener;
import com.colasoft.opensearch.rest.action.RestResponseListener;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static com.colasoft.opensearch.rest.RestRequest.Method.GET;

/**
 * _cat API action to get segments information
 *
 * @opensearch.api
 */
public class RestSegmentsAction extends AbstractCatAction {

    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestSegmentsAction.class);

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_cat/segments"), new Route(GET, "/_cat/segments/{index}")));
    }

    @Override
    public String getName() {
        return "cat_segments_action";
    }

    @Override
    public boolean allowSystemIndexAccessByDefault() {
        return true;
    }

    @Override
    public RestChannelConsumer doCatRequest(final RestRequest request, final NodeClient client) {
        final String[] indices = Strings.splitStringByCommaToArray(request.param("index"));

        final ClusterStateRequest clusterStateRequest = new ClusterStateRequest();
        clusterStateRequest.local(request.paramAsBoolean("local", clusterStateRequest.local()));
        clusterStateRequest.clusterManagerNodeTimeout(
            request.paramAsTime("cluster_manager_timeout", clusterStateRequest.clusterManagerNodeTimeout())
        );
        parseDeprecatedMasterTimeoutParameter(clusterStateRequest, request);
        clusterStateRequest.clear().nodes(true).routingTable(true).indices(indices);

        return channel -> client.admin().cluster().state(clusterStateRequest, new RestActionListener<ClusterStateResponse>(channel) {
            @Override
            public void processResponse(final ClusterStateResponse clusterStateResponse) {
                final IndicesSegmentsRequest indicesSegmentsRequest = new IndicesSegmentsRequest();
                indicesSegmentsRequest.indices(indices);
                client.admin().indices().segments(indicesSegmentsRequest, new RestResponseListener<IndicesSegmentResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(final IndicesSegmentResponse indicesSegmentResponse) throws Exception {
                        final Map<String, IndexSegments> indicesSegments = indicesSegmentResponse.getIndices();
                        Table tab = buildTable(request, clusterStateResponse, indicesSegments);
                        return RestTable.buildResponse(tab, channel);
                    }
                });
            }
        });
    }

    @Override
    protected void documentation(StringBuilder sb) {
        sb.append("/_cat/segments\n");
        sb.append("/_cat/segments/{index}\n");
    }

    @Override
    protected Table getTableWithHeader(RestRequest request) {
        Table table = new Table();
        table.startHeaders();
        table.addCell("index", "default:true;alias:i,idx;desc:index name");
        table.addCell("shard", "default:true;alias:s,sh;desc:shard name");
        table.addCell("prirep", "alias:p,pr,primaryOrReplica;default:true;desc:primary or replica");
        table.addCell("ip", "default:true;desc:ip of node where it lives");
        table.addCell("id", "default:false;desc:unique id of node where it lives");
        table.addCell("segment", "default:true;alias:seg;desc:segment name");
        table.addCell("generation", "default:true;alias:g,gen;text-align:right;desc:segment generation");
        table.addCell("docs.count", "default:true;alias:dc,docsCount;text-align:right;desc:number of docs in segment");
        table.addCell("docs.deleted", "default:true;alias:dd,docsDeleted;text-align:right;desc:number of deleted docs in segment");
        table.addCell("size", "default:true;alias:si;text-align:right;desc:segment size in bytes");
        table.addCell("size.memory", "default:true;alias:sm,sizeMemory;text-align:right;desc:segment memory in bytes");
        table.addCell("committed", "default:true;alias:ic,isCommitted;desc:is segment committed");
        table.addCell("searchable", "default:true;alias:is,isSearchable;desc:is segment searched");
        table.addCell("version", "default:true;alias:v,ver;desc:version");
        table.addCell("compound", "default:true;alias:ico,isCompound;desc:is segment compound");
        table.endHeaders();
        return table;
    }

    private Table buildTable(final RestRequest request, ClusterStateResponse state, Map<String, IndexSegments> indicesSegments) {
        Table table = getTableWithHeader(request);

        DiscoveryNodes nodes = state.getState().nodes();

        for (IndexSegments indexSegments : indicesSegments.values()) {
            Map<Integer, IndexShardSegments> shards = indexSegments.getShards();

            for (IndexShardSegments indexShardSegments : shards.values()) {
                ShardSegments[] shardSegments = indexShardSegments.getShards();

                for (ShardSegments shardSegment : shardSegments) {
                    List<Segment> segments = shardSegment.getSegments();

                    for (Segment segment : segments) {
                        table.startRow();

                        table.addCell(shardSegment.getShardRouting().getIndexName());
                        table.addCell(shardSegment.getShardRouting().getId());
                        table.addCell(shardSegment.getShardRouting().primary() ? "p" : "r");
                        table.addCell(nodes.get(shardSegment.getShardRouting().currentNodeId()).getHostAddress());
                        table.addCell(shardSegment.getShardRouting().currentNodeId());
                        table.addCell(segment.getName());
                        table.addCell(segment.getGeneration());
                        table.addCell(segment.getNumDocs());
                        table.addCell(segment.getDeletedDocs());
                        table.addCell(segment.getSize());
                        table.addCell(0L);
                        table.addCell(segment.isCommitted());
                        table.addCell(segment.isSearch());
                        table.addCell(segment.getVersion());
                        table.addCell(segment.isCompound());

                        table.endRow();
                    }

                }
            }

        }

        return table;
    }
}
