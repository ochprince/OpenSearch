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

package org.opensearch.action.admin.indices.shards;

import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.opensearch.LegacyESVersion;
import org.opensearch.OpenSearchException;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.DefaultShardOperationFailedException;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.collect.ImmutableOpenIntMap;
import org.opensearch.common.collect.ImmutableOpenMap;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.ToXContentFragment;
import org.opensearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Response for {@link IndicesShardStoresAction}
 *
 * Consists of {@link StoreStatus}s for requested indices grouped by
 * indices and shard ids and a list of encountered node {@link Failure}s
 */
public class IndicesShardStoresResponse extends ActionResponse implements ToXContentFragment {

    /**
     * Shard store information from a node
     */
    public static class StoreStatus implements Writeable, ToXContentFragment, Comparable<StoreStatus> {
        private final DiscoveryNode node;
        private final String allocationId;
        private Exception storeException;
        private final AllocationStatus allocationStatus;

        /**
         * The status of the shard store with respect to the cluster
         */
        public enum AllocationStatus {

            /**
             * Allocated as primary
             */
            PRIMARY((byte) 0),

            /**
             * Allocated as a replica
             */
            REPLICA((byte) 1),

            /**
             * Not allocated
             */
            UNUSED((byte) 2);

            private final byte id;

            AllocationStatus(byte id) {
                this.id = id;
            }

            private static AllocationStatus fromId(byte id) {
                switch (id) {
                    case 0: return PRIMARY;
                    case 1: return REPLICA;
                    case 2: return UNUSED;
                    default: throw new IllegalArgumentException("unknown id for allocation status [" + id + "]");
                }
            }

            public String value() {
                switch (id) {
                    case 0: return "primary";
                    case 1: return "replica";
                    case 2: return "unused";
                    default: throw new IllegalArgumentException("unknown id for allocation status [" + id + "]");
                }
            }

            private static AllocationStatus readFrom(StreamInput in) throws IOException {
                return fromId(in.readByte());
            }

            private void writeTo(StreamOutput out) throws IOException {
                out.writeByte(id);
            }
        }

        public StoreStatus(StreamInput in) throws IOException {
            node = new DiscoveryNode(in);
            if (in.getVersion().before(LegacyESVersion.V_6_0_0_alpha1)) {
                // legacy version
                in.readLong();
            }
            allocationId = in.readOptionalString();
            allocationStatus = AllocationStatus.readFrom(in);
            if (in.readBoolean()) {
                storeException = in.readException();
            }
        }

        public StoreStatus(DiscoveryNode node, String allocationId, AllocationStatus allocationStatus, Exception storeException) {
            this.node = node;
            this.allocationId = allocationId;
            this.allocationStatus = allocationStatus;
            this.storeException = storeException;
        }

        /**
         * Node the store belongs to
         */
        public DiscoveryNode getNode() {
            return node;
        }

        /**
         * AllocationStatus id of the store, used to select the store that will be
         * used as a primary.
         */
        public String getAllocationId() {
            return allocationId;
        }

        /**
         * Exception while trying to open the
         * shard index or from when the shard failed
         */
        public Exception getStoreException() {
            return storeException;
        }

        /**
         * The allocationStatus status of the store.
         * {@link AllocationStatus#PRIMARY} indicates a primary shard copy
         * {@link AllocationStatus#REPLICA} indicates a replica shard copy
         * {@link AllocationStatus#UNUSED} indicates an unused shard copy
         */
        public AllocationStatus getAllocationStatus() {
            return allocationStatus;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            node.writeTo(out);
            if (out.getVersion().before(LegacyESVersion.V_6_0_0_alpha1)) {
                // legacy version
                out.writeLong(-1L);
            }
            out.writeOptionalString(allocationId);
            allocationStatus.writeTo(out);
            if (storeException != null) {
                out.writeBoolean(true);
                out.writeException(storeException);
            } else {
                out.writeBoolean(false);
            }
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            node.toXContent(builder, params);
            if (allocationId != null) {
                builder.field(Fields.ALLOCATION_ID, allocationId);
            }
            builder.field(Fields.ALLOCATED, allocationStatus.value());
            if (storeException != null) {
                builder.startObject(Fields.STORE_EXCEPTION);
                OpenSearchException.generateThrowableXContent(builder, params, storeException);
                builder.endObject();
            }
            return builder;
        }

        @Override
        public int compareTo(StoreStatus other) {
            if (storeException != null && other.storeException == null) {
                return 1;
            } else if (other.storeException != null && storeException == null) {
                return -1;
            }
            if (allocationId != null && other.allocationId == null) {
                return -1;
            } else if (allocationId == null && other.allocationId != null) {
                return 1;
            } else if (allocationId == null && other.allocationId == null) {
                return Integer.compare(allocationStatus.id, other.allocationStatus.id);
            } else {
                int compare = Integer.compare(allocationStatus.id, other.allocationStatus.id);
                if (compare == 0) {
                    return allocationId.compareTo(other.allocationId);
                }
                return compare;
            }
        }
    }

    /**
     * Single node failure while retrieving shard store information
     */
    public static class Failure extends DefaultShardOperationFailedException {
        private String nodeId;

        public Failure(String nodeId, String index, int shardId, Throwable reason) {
            super(index, shardId, reason);
            this.nodeId = nodeId;
        }

        private Failure(StreamInput in) throws IOException {
            if (in.getVersion().before(LegacyESVersion.V_7_4_0)) {
                nodeId = in.readString();
            }
            readFrom(in, this);
            if (in.getVersion().onOrAfter(LegacyESVersion.V_7_4_0)) {
                nodeId = in.readString();
            }
        }

        public String nodeId() {
            return nodeId;
        }

        static Failure readFailure(StreamInput in) throws IOException {
            return new Failure(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            if (out.getVersion().before(LegacyESVersion.V_7_4_0)) {
                out.writeString(nodeId);
            }
            super.writeTo(out);
            if (out.getVersion().onOrAfter(LegacyESVersion.V_7_4_0)) {
                out.writeString(nodeId);
            }
        }

        @Override
        public XContentBuilder innerToXContent(XContentBuilder builder, Params params) throws IOException {
            builder.field("node", nodeId());
            return super.innerToXContent(builder, params);
        }
    }

    private final ImmutableOpenMap<String, ImmutableOpenIntMap<List<StoreStatus>>> storeStatuses;
    private final List<Failure> failures;

    public IndicesShardStoresResponse(ImmutableOpenMap<String, ImmutableOpenIntMap<List<StoreStatus>>> storeStatuses,
                                      List<Failure> failures) {
        this.storeStatuses = storeStatuses;
        this.failures = failures;
    }

    IndicesShardStoresResponse() {
        this(ImmutableOpenMap.of(), Collections.emptyList());
    }

    public IndicesShardStoresResponse(StreamInput in) throws IOException {
        super(in);
        storeStatuses = in.readImmutableMap(StreamInput::readString, i -> {
            int indexEntries = i.readVInt();
            ImmutableOpenIntMap.Builder<List<StoreStatus>> shardEntries = ImmutableOpenIntMap.builder();
            for (int shardCount = 0; shardCount < indexEntries; shardCount++) {
                shardEntries.put(i.readInt(), i.readList(StoreStatus::new));
            }
            return shardEntries.build();
        });
        failures = Collections.unmodifiableList(in.readList(Failure::readFailure));
    }

    /**
     * Returns {@link StoreStatus}s
     * grouped by their index names and shard ids.
     */
    public ImmutableOpenMap<String, ImmutableOpenIntMap<List<StoreStatus>>> getStoreStatuses() {
        return storeStatuses;
    }

    /**
     * Returns node {@link Failure}s encountered
     * while executing the request
     */
    public List<Failure> getFailures() {
        return failures;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeMap(storeStatuses, StreamOutput::writeString, (o, v) -> {
            o.writeVInt(v.size());
            for (IntObjectCursor<List<StoreStatus>> shardStatusesEntry : v) {
                o.writeInt(shardStatusesEntry.key);
                o.writeCollection(shardStatusesEntry.value);
            }
        });
        out.writeList(failures);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (failures.size() > 0) {
            builder.startArray(Fields.FAILURES);
            for (Failure failure : failures) {
                failure.toXContent(builder, params);
            }
            builder.endArray();
        }

        builder.startObject(Fields.INDICES);
        for (ObjectObjectCursor<String, ImmutableOpenIntMap<List<StoreStatus>>> indexShards : storeStatuses) {
            builder.startObject(indexShards.key);

            builder.startObject(Fields.SHARDS);
            for (IntObjectCursor<List<StoreStatus>> shardStatusesEntry : indexShards.value) {
                builder.startObject(String.valueOf(shardStatusesEntry.key));
                builder.startArray(Fields.STORES);
                for (StoreStatus storeStatus : shardStatusesEntry.value) {
                    builder.startObject();
                    storeStatus.toXContent(builder, params);
                    builder.endObject();
                }
                builder.endArray();

                builder.endObject();
            }
            builder.endObject();

            builder.endObject();
        }
        builder.endObject();
        return builder;
    }

    static final class Fields {
        static final String INDICES = "indices";
        static final String SHARDS = "shards";
        static final String FAILURES = "failures";
        static final String STORES = "stores";
        // StoreStatus fields
        static final String ALLOCATION_ID = "allocation_id";
        static final String STORE_EXCEPTION = "store_exception";
        static final String ALLOCATED = "allocation";
    }
}
