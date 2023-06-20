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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.action.admin.cluster.node.stats;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.Version;
import com.colasoft.opensearch.action.support.nodes.BaseNodeResponse;
import com.colasoft.opensearch.cluster.node.DiscoveryNode;
import com.colasoft.opensearch.cluster.node.DiscoveryNodeRole;
import com.colasoft.opensearch.cluster.routing.WeightedRoutingStats;
import com.colasoft.opensearch.cluster.service.ClusterManagerThrottlingStats;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ToXContentFragment;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.discovery.DiscoveryStats;
import com.colasoft.opensearch.http.HttpStats;
import com.colasoft.opensearch.index.stats.IndexingPressureStats;
import com.colasoft.opensearch.index.stats.ShardIndexingPressureStats;
import com.colasoft.opensearch.index.store.remote.filecache.FileCacheStats;
import com.colasoft.opensearch.indices.NodeIndicesStats;
import com.colasoft.opensearch.indices.breaker.AllCircuitBreakerStats;
import com.colasoft.opensearch.ingest.IngestStats;
import com.colasoft.opensearch.monitor.fs.FsInfo;
import com.colasoft.opensearch.monitor.jvm.JvmStats;
import com.colasoft.opensearch.monitor.os.OsStats;
import com.colasoft.opensearch.monitor.process.ProcessStats;
import com.colasoft.opensearch.node.AdaptiveSelectionStats;
import com.colasoft.opensearch.script.ScriptCacheStats;
import com.colasoft.opensearch.script.ScriptStats;
import com.colasoft.opensearch.search.backpressure.stats.SearchBackpressureStats;
import com.colasoft.opensearch.threadpool.ThreadPoolStats;
import com.colasoft.opensearch.transport.TransportStats;

import java.io.IOException;
import java.util.Map;

/**
 * Node statistics (dynamic, changes depending on when created).
 *
 * @opensearch.internal
 */
public class NodeStats extends BaseNodeResponse implements ToXContentFragment {

    private long timestamp;

    @Nullable
    private NodeIndicesStats indices;

    @Nullable
    private OsStats os;

    @Nullable
    private ProcessStats process;

    @Nullable
    private JvmStats jvm;

    @Nullable
    private ThreadPoolStats threadPool;

    @Nullable
    private FsInfo fs;

    @Nullable
    private TransportStats transport;

    @Nullable
    private HttpStats http;

    @Nullable
    private AllCircuitBreakerStats breaker;

    @Nullable
    private ScriptStats scriptStats;

    @Nullable
    private ScriptCacheStats scriptCacheStats;

    @Nullable
    private DiscoveryStats discoveryStats;

    @Nullable
    private IngestStats ingestStats;

    @Nullable
    private AdaptiveSelectionStats adaptiveSelectionStats;

    @Nullable
    private IndexingPressureStats indexingPressureStats;

    @Nullable
    private ShardIndexingPressureStats shardIndexingPressureStats;

    @Nullable
    private SearchBackpressureStats searchBackpressureStats;

    @Nullable
    private ClusterManagerThrottlingStats clusterManagerThrottlingStats;

    @Nullable
    private WeightedRoutingStats weightedRoutingStats;

    @Nullable
    private FileCacheStats fileCacheStats;

    public NodeStats(StreamInput in) throws IOException {
        super(in);
        timestamp = in.readVLong();
        if (in.readBoolean()) {
            indices = new NodeIndicesStats(in);
        }
        os = in.readOptionalWriteable(OsStats::new);
        process = in.readOptionalWriteable(ProcessStats::new);
        jvm = in.readOptionalWriteable(JvmStats::new);
        threadPool = in.readOptionalWriteable(ThreadPoolStats::new);
        fs = in.readOptionalWriteable(FsInfo::new);
        transport = in.readOptionalWriteable(TransportStats::new);
        http = in.readOptionalWriteable(HttpStats::new);
        breaker = in.readOptionalWriteable(AllCircuitBreakerStats::new);
        scriptStats = in.readOptionalWriteable(ScriptStats::new);
        discoveryStats = in.readOptionalWriteable(DiscoveryStats::new);
        ingestStats = in.readOptionalWriteable(IngestStats::new);
        adaptiveSelectionStats = in.readOptionalWriteable(AdaptiveSelectionStats::new);
        scriptCacheStats = null;
        if (in.getVersion().onOrAfter(LegacyESVersion.V_7_8_0)) {
            if (in.getVersion().before(LegacyESVersion.V_7_9_0)) {
                scriptCacheStats = in.readOptionalWriteable(ScriptCacheStats::new);
            } else if (scriptStats != null) {
                scriptCacheStats = scriptStats.toScriptCacheStats();
            }
        }
        if (in.getVersion().onOrAfter(LegacyESVersion.V_7_9_0)) {
            indexingPressureStats = in.readOptionalWriteable(IndexingPressureStats::new);
        } else {
            indexingPressureStats = null;
        }
        if (in.getVersion().onOrAfter(Version.V_1_2_0)) {
            shardIndexingPressureStats = in.readOptionalWriteable(ShardIndexingPressureStats::new);
        } else {
            shardIndexingPressureStats = null;
        }

        if (in.getVersion().onOrAfter(Version.V_2_4_0)) {
            searchBackpressureStats = in.readOptionalWriteable(SearchBackpressureStats::new);
        } else {
            searchBackpressureStats = null;
        }

        if (in.getVersion().onOrAfter(Version.V_2_6_0)) {
            clusterManagerThrottlingStats = in.readOptionalWriteable(ClusterManagerThrottlingStats::new);
        } else {
            clusterManagerThrottlingStats = null;
        }
        if (in.getVersion().onOrAfter(Version.V_2_6_0)) {
            weightedRoutingStats = in.readOptionalWriteable(WeightedRoutingStats::new);
        } else {
            weightedRoutingStats = null;
        }
        if (in.getVersion().onOrAfter(Version.V_2_7_0)) {
            fileCacheStats = in.readOptionalWriteable(FileCacheStats::new);
        } else {
            fileCacheStats = null;
        }
    }

    public NodeStats(
        DiscoveryNode node,
        long timestamp,
        @Nullable NodeIndicesStats indices,
        @Nullable OsStats os,
        @Nullable ProcessStats process,
        @Nullable JvmStats jvm,
        @Nullable ThreadPoolStats threadPool,
        @Nullable FsInfo fs,
        @Nullable TransportStats transport,
        @Nullable HttpStats http,
        @Nullable AllCircuitBreakerStats breaker,
        @Nullable ScriptStats scriptStats,
        @Nullable DiscoveryStats discoveryStats,
        @Nullable IngestStats ingestStats,
        @Nullable AdaptiveSelectionStats adaptiveSelectionStats,
        @Nullable ScriptCacheStats scriptCacheStats,
        @Nullable IndexingPressureStats indexingPressureStats,
        @Nullable ShardIndexingPressureStats shardIndexingPressureStats,
        @Nullable SearchBackpressureStats searchBackpressureStats,
        @Nullable ClusterManagerThrottlingStats clusterManagerThrottlingStats,
        @Nullable WeightedRoutingStats weightedRoutingStats,
        @Nullable FileCacheStats fileCacheStats
    ) {
        super(node);
        this.timestamp = timestamp;
        this.indices = indices;
        this.os = os;
        this.process = process;
        this.jvm = jvm;
        this.threadPool = threadPool;
        this.fs = fs;
        this.transport = transport;
        this.http = http;
        this.breaker = breaker;
        this.scriptStats = scriptStats;
        this.discoveryStats = discoveryStats;
        this.ingestStats = ingestStats;
        this.adaptiveSelectionStats = adaptiveSelectionStats;
        this.scriptCacheStats = scriptCacheStats;
        this.indexingPressureStats = indexingPressureStats;
        this.shardIndexingPressureStats = shardIndexingPressureStats;
        this.searchBackpressureStats = searchBackpressureStats;
        this.clusterManagerThrottlingStats = clusterManagerThrottlingStats;
        this.weightedRoutingStats = weightedRoutingStats;
        this.fileCacheStats = fileCacheStats;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    @Nullable
    public String getHostname() {
        return getNode().getHostName();
    }

    /**
     * Indices level stats.
     */
    @Nullable
    public NodeIndicesStats getIndices() {
        return this.indices;
    }

    /**
     * Operating System level statistics.
     */
    @Nullable
    public OsStats getOs() {
        return this.os;
    }

    /**
     * Process level statistics.
     */
    @Nullable
    public ProcessStats getProcess() {
        return process;
    }

    /**
     * JVM level statistics.
     */
    @Nullable
    public JvmStats getJvm() {
        return jvm;
    }

    /**
     * Thread Pool level statistics.
     */
    @Nullable
    public ThreadPoolStats getThreadPool() {
        return this.threadPool;
    }

    /**
     * File system level stats.
     */
    @Nullable
    public FsInfo getFs() {
        return fs;
    }

    @Nullable
    public TransportStats getTransport() {
        return this.transport;
    }

    @Nullable
    public HttpStats getHttp() {
        return this.http;
    }

    @Nullable
    public AllCircuitBreakerStats getBreaker() {
        return this.breaker;
    }

    @Nullable
    public ScriptStats getScriptStats() {
        return this.scriptStats;
    }

    @Nullable
    public DiscoveryStats getDiscoveryStats() {
        return this.discoveryStats;
    }

    @Nullable
    public IngestStats getIngestStats() {
        return ingestStats;
    }

    @Nullable
    public AdaptiveSelectionStats getAdaptiveSelectionStats() {
        return adaptiveSelectionStats;
    }

    @Nullable
    public ScriptCacheStats getScriptCacheStats() {
        return scriptCacheStats;
    }

    @Nullable
    public IndexingPressureStats getIndexingPressureStats() {
        return indexingPressureStats;
    }

    @Nullable
    public ShardIndexingPressureStats getShardIndexingPressureStats() {
        return shardIndexingPressureStats;
    }

    @Nullable
    public SearchBackpressureStats getSearchBackpressureStats() {
        return searchBackpressureStats;
    }

    @Nullable
    public ClusterManagerThrottlingStats getClusterManagerThrottlingStats() {
        return clusterManagerThrottlingStats;
    }

    public WeightedRoutingStats getWeightedRoutingStats() {
        return weightedRoutingStats;
    }

    public FileCacheStats getFileCacheStats() {
        return fileCacheStats;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVLong(timestamp);
        if (indices == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            indices.writeTo(out);
        }
        out.writeOptionalWriteable(os);
        out.writeOptionalWriteable(process);
        out.writeOptionalWriteable(jvm);
        out.writeOptionalWriteable(threadPool);
        out.writeOptionalWriteable(fs);
        out.writeOptionalWriteable(transport);
        out.writeOptionalWriteable(http);
        out.writeOptionalWriteable(breaker);
        out.writeOptionalWriteable(scriptStats);
        out.writeOptionalWriteable(discoveryStats);
        out.writeOptionalWriteable(ingestStats);
        out.writeOptionalWriteable(adaptiveSelectionStats);
        if (out.getVersion().onOrAfter(LegacyESVersion.V_7_8_0) && out.getVersion().before(LegacyESVersion.V_7_9_0)) {
            out.writeOptionalWriteable(scriptCacheStats);
        }
        if (out.getVersion().onOrAfter(LegacyESVersion.V_7_9_0)) {
            out.writeOptionalWriteable(indexingPressureStats);
        }
        if (out.getVersion().onOrAfter(Version.V_1_2_0)) {
            out.writeOptionalWriteable(shardIndexingPressureStats);
        }
        if (out.getVersion().onOrAfter(Version.V_2_4_0)) {
            out.writeOptionalWriteable(searchBackpressureStats);
        }
        if (out.getVersion().onOrAfter(Version.V_2_6_0)) {
            out.writeOptionalWriteable(clusterManagerThrottlingStats);
        }
        if (out.getVersion().onOrAfter(Version.V_2_6_0)) {
            out.writeOptionalWriteable(weightedRoutingStats);
        }
        if (out.getVersion().onOrAfter(Version.V_2_7_0)) {
            out.writeOptionalWriteable(fileCacheStats);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {

        builder.field("name", getNode().getName());
        builder.field("transport_address", getNode().getAddress().toString());
        builder.field("host", getNode().getHostName());
        builder.field("ip", getNode().getAddress());

        builder.startArray("roles");
        for (DiscoveryNodeRole role : getNode().getRoles()) {
            builder.value(role.roleName());
        }
        builder.endArray();

        if (!getNode().getAttributes().isEmpty()) {
            builder.startObject("attributes");
            for (Map.Entry<String, String> attrEntry : getNode().getAttributes().entrySet()) {
                builder.field(attrEntry.getKey(), attrEntry.getValue());
            }
            builder.endObject();
        }

        if (getIndices() != null) {
            getIndices().toXContent(builder, params);
        }
        if (getOs() != null) {
            getOs().toXContent(builder, params);
        }
        if (getProcess() != null) {
            getProcess().toXContent(builder, params);
        }
        if (getJvm() != null) {
            getJvm().toXContent(builder, params);
        }
        if (getThreadPool() != null) {
            getThreadPool().toXContent(builder, params);
        }
        if (getFs() != null) {
            getFs().toXContent(builder, params);
        }
        if (getTransport() != null) {
            getTransport().toXContent(builder, params);
        }
        if (getHttp() != null) {
            getHttp().toXContent(builder, params);
        }
        if (getBreaker() != null) {
            getBreaker().toXContent(builder, params);
        }
        if (getScriptStats() != null) {
            getScriptStats().toXContent(builder, params);
        }
        if (getDiscoveryStats() != null) {
            getDiscoveryStats().toXContent(builder, params);
        }
        if (getIngestStats() != null) {
            getIngestStats().toXContent(builder, params);
        }
        if (getAdaptiveSelectionStats() != null) {
            getAdaptiveSelectionStats().toXContent(builder, params);
        }
        if (getScriptCacheStats() != null) {
            getScriptCacheStats().toXContent(builder, params);
        }
        if (getIndexingPressureStats() != null) {
            getIndexingPressureStats().toXContent(builder, params);
        }
        if (getShardIndexingPressureStats() != null) {
            getShardIndexingPressureStats().toXContent(builder, params);
        }
        if (getSearchBackpressureStats() != null) {
            getSearchBackpressureStats().toXContent(builder, params);
        }
        if (getClusterManagerThrottlingStats() != null) {
            getClusterManagerThrottlingStats().toXContent(builder, params);
        }
        if (getWeightedRoutingStats() != null) {
            getWeightedRoutingStats().toXContent(builder, params);
        }
        if (getFileCacheStats() != null) {
            getFileCacheStats().toXContent(builder, params);
        }

        return builder;
    }
}
