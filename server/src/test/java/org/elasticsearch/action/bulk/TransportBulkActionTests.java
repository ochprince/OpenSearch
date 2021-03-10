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

package org.elasticsearch.action.bulk;

import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.Version;
import org.opensearch.action.ActionListener;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.TransportBulkActionTookTests.Resolver;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.ActionTestUtils;
import org.elasticsearch.action.support.AutoCreateIndex;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.DataStream;
import org.elasticsearch.cluster.metadata.IndexAbstraction;
import org.elasticsearch.cluster.metadata.IndexAbstraction.Index;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.cluster.metadata.Metadata;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeRole;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.IndexingPressure;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.indices.SystemIndexDescriptor;
import org.elasticsearch.indices.SystemIndices;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.test.transport.CapturingTransport;
import org.elasticsearch.threadpool.TestThreadPool;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.junit.After;
import org.junit.Before;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.bulk.TransportBulkAction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.opensearch.action.bulk.TransportBulkAction.prohibitCustomRoutingOnDataStream;
import static org.elasticsearch.cluster.metadata.MetadataCreateDataStreamServiceTests.createDataStream;
import static org.elasticsearch.test.ClusterServiceUtils.createClusterService;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TransportBulkActionTests extends ESTestCase {

    /** Services needed by bulk action */
    private TransportService transportService;
    private ClusterService clusterService;
    private TestThreadPool threadPool;

    private TestTransportBulkAction bulkAction;

    class TestTransportBulkAction extends TransportBulkAction {

        volatile boolean failIndexCreation = false;
        boolean indexCreated = false; // set when the "real" index is created

        TestTransportBulkAction() {
            super(TransportBulkActionTests.this.threadPool, transportService, clusterService, null, null,
                    null, new ActionFilters(Collections.emptySet()), new Resolver(),
                    new AutoCreateIndex(Settings.EMPTY, clusterService.getClusterSettings(), new Resolver(), new SystemIndices(emptyMap())),
                    new IndexingPressure(Settings.EMPTY), new SystemIndices(emptyMap()));
        }

        @Override
        protected boolean needToCheck() {
            return true;
        }

        @Override
        void createIndex(String index, TimeValue timeout, Version minNodeVersion, ActionListener<CreateIndexResponse> listener) {
            indexCreated = true;
            if (failIndexCreation) {
                listener.onFailure(new ResourceAlreadyExistsException("index already exists"));
            } else {
                listener.onResponse(null);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        threadPool = new TestThreadPool(getClass().getName());
        DiscoveryNode discoveryNode = new DiscoveryNode("node", ESTestCase.buildNewFakeTransportAddress(), emptyMap(),
            DiscoveryNodeRole.BUILT_IN_ROLES, VersionUtils.randomCompatibleVersion(random(), Version.CURRENT));
        clusterService = createClusterService(threadPool, discoveryNode);
        CapturingTransport capturingTransport = new CapturingTransport();
        transportService = capturingTransport.createTransportService(clusterService.getSettings(), threadPool,
            TransportService.NOOP_TRANSPORT_INTERCEPTOR,
            boundAddress -> clusterService.localNode(), null, Collections.emptySet());
        transportService.start();
        transportService.acceptIncomingRequests();
        bulkAction = new TestTransportBulkAction();
    }

    @After
    public void tearDown() throws Exception {
        ThreadPool.terminate(threadPool, 30, TimeUnit.SECONDS);
        threadPool = null;
        clusterService.close();
        super.tearDown();
    }

    public void testDeleteNonExistingDocDoesNotCreateIndex() throws Exception {
        BulkRequest bulkRequest = new BulkRequest().add(new DeleteRequest("index", "type", "id"));

        PlainActionFuture<BulkResponse> future = PlainActionFuture.newFuture();
        ActionTestUtils.execute(bulkAction, null, bulkRequest, future);

        BulkResponse response = future.actionGet();
        assertFalse(bulkAction.indexCreated);
        BulkItemResponse[] bulkResponses = ((BulkResponse) response).getItems();
        assertEquals(bulkResponses.length, 1);
        assertTrue(bulkResponses[0].isFailed());
        assertTrue(bulkResponses[0].getFailure().getCause() instanceof IndexNotFoundException);
        assertEquals("index", bulkResponses[0].getFailure().getIndex());
    }

    public void testDeleteNonExistingDocExternalVersionCreatesIndex() throws Exception {
        BulkRequest bulkRequest = new BulkRequest()
                .add(new DeleteRequest("index", "type", "id").versionType(VersionType.EXTERNAL).version(0));

        PlainActionFuture<BulkResponse> future = PlainActionFuture.newFuture();
        ActionTestUtils.execute(bulkAction, null, bulkRequest, future);
        future.actionGet();
        assertTrue(bulkAction.indexCreated);
    }

    public void testDeleteNonExistingDocExternalGteVersionCreatesIndex() throws Exception {
        BulkRequest bulkRequest = new BulkRequest()
                .add(new DeleteRequest("index2", "type", "id").versionType(VersionType.EXTERNAL_GTE).version(0));

        PlainActionFuture<BulkResponse> future = PlainActionFuture.newFuture();
        ActionTestUtils.execute(bulkAction, null, bulkRequest, future);
        future.actionGet();
        assertTrue(bulkAction.indexCreated);
    }

    public void testGetIndexWriteRequest() throws Exception {
        IndexRequest indexRequest = new IndexRequest("index", "type", "id1").source(emptyMap());
        UpdateRequest upsertRequest = new UpdateRequest("index", "type", "id1").upsert(indexRequest).script(mockScript("1"));
        UpdateRequest docAsUpsertRequest = new UpdateRequest("index", "type", "id2").doc(indexRequest).docAsUpsert(true);
        UpdateRequest scriptedUpsert = new UpdateRequest("index", "type", "id2").upsert(indexRequest).script(mockScript("1"))
            .scriptedUpsert(true);

        assertEquals(TransportBulkAction.getIndexWriteRequest(indexRequest), indexRequest);
        assertEquals(TransportBulkAction.getIndexWriteRequest(upsertRequest), indexRequest);
        assertEquals(TransportBulkAction.getIndexWriteRequest(docAsUpsertRequest), indexRequest);
        assertEquals(TransportBulkAction.getIndexWriteRequest(scriptedUpsert), indexRequest);

        DeleteRequest deleteRequest = new DeleteRequest("index", "id");
        assertNull(TransportBulkAction.getIndexWriteRequest(deleteRequest));

        UpdateRequest badUpsertRequest = new UpdateRequest("index", "type", "id1");
        assertNull(TransportBulkAction.getIndexWriteRequest(badUpsertRequest));
    }

    public void testProhibitAppendWritesInBackingIndices() throws Exception {
        String dataStreamName = "logs-foobar";
        ClusterState clusterState = createDataStream(dataStreamName);
        Metadata metadata = clusterState.metadata();

        // Testing create op against backing index fails:
        String backingIndexName = DataStream.getDefaultBackingIndexName(dataStreamName, 1);
        IndexRequest invalidRequest1 = new IndexRequest(backingIndexName).opType(DocWriteRequest.OpType.CREATE);
        Exception e = expectThrows(IllegalArgumentException.class,
            () -> TransportBulkAction.prohibitAppendWritesInBackingIndices(invalidRequest1, metadata));
        assertThat(e.getMessage(), equalTo("index request with op_type=create targeting backing indices is disallowed, " +
            "target corresponding data stream [logs-foobar] instead"));

        // Testing index op against backing index fails:
        IndexRequest invalidRequest2 = new IndexRequest(backingIndexName).opType(DocWriteRequest.OpType.INDEX);
        e = expectThrows(IllegalArgumentException.class,
            () -> TransportBulkAction.prohibitAppendWritesInBackingIndices(invalidRequest2, metadata));
        assertThat(e.getMessage(), equalTo("index request with op_type=index and no if_primary_term and if_seq_no set " +
            "targeting backing indices is disallowed, target corresponding data stream [logs-foobar] instead"));

        // Testing valid writes ops against a backing index:
        DocWriteRequest<?> validRequest = new IndexRequest(backingIndexName).opType(DocWriteRequest.OpType.INDEX)
            .setIfSeqNo(1).setIfPrimaryTerm(1);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);
        validRequest = new DeleteRequest(backingIndexName);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);
        validRequest = new UpdateRequest(backingIndexName, "_id");
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);

        // Testing append only write via ds name
        validRequest = new IndexRequest(dataStreamName).opType(DocWriteRequest.OpType.CREATE);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);

        validRequest = new IndexRequest(dataStreamName).opType(DocWriteRequest.OpType.INDEX);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);

        // Append only for a backing index that doesn't exist is allowed:
        validRequest = new IndexRequest(DataStream.getDefaultBackingIndexName("logs-barbaz", 1))
            .opType(DocWriteRequest.OpType.CREATE);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);

        // Some other index names:
        validRequest = new IndexRequest("my-index").opType(DocWriteRequest.OpType.CREATE);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);
        validRequest = new IndexRequest("foobar").opType(DocWriteRequest.OpType.CREATE);
        TransportBulkAction.prohibitAppendWritesInBackingIndices(validRequest, metadata);
    }

    public void testProhibitCustomRoutingOnDataStream() throws Exception {
        String dataStreamName = "logs-foobar";
        ClusterState clusterState = createDataStream(dataStreamName);
        Metadata metadata = clusterState.metadata();

        // custom routing requests against the data stream are prohibited
        DocWriteRequest<?> writeRequestAgainstDataStream = new IndexRequest(dataStreamName).opType(DocWriteRequest.OpType.INDEX)
            .routing("custom");
        IllegalArgumentException exception =
            expectThrows(IllegalArgumentException.class, () -> prohibitCustomRoutingOnDataStream(writeRequestAgainstDataStream, metadata));
        assertThat(exception.getMessage(), is("index request targeting data stream [logs-foobar] specifies a custom routing. target the " +
            "backing indices directly or remove the custom routing."));

        // test custom routing is allowed when the index request targets the backing index
        DocWriteRequest<?> writeRequestAgainstIndex =
            new IndexRequest(DataStream.getDefaultBackingIndexName(dataStreamName, 1L)).opType(DocWriteRequest.OpType.INDEX)
            .routing("custom");
        prohibitCustomRoutingOnDataStream(writeRequestAgainstIndex, metadata);
    }

    public void testOnlySystem() {
        SortedMap<String, IndexAbstraction> indicesLookup = new TreeMap<>();
        Settings settings = Settings.builder().put("index.version.created", Version.CURRENT).build();
        indicesLookup.put(".foo",
            new Index(IndexMetadata.builder(".foo").settings(settings).system(true).numberOfShards(1).numberOfReplicas(0).build()));
        indicesLookup.put(".bar",
            new Index(IndexMetadata.builder(".bar").settings(settings).system(true).numberOfShards(1).numberOfReplicas(0).build()));
        SystemIndices systemIndices = new SystemIndices(singletonMap("plugin", singletonList(new SystemIndexDescriptor(".test", ""))));
        List<String> onlySystem = Arrays.asList(".foo", ".bar");
        assertTrue(bulkAction.isOnlySystem(buildBulkRequest(onlySystem), indicesLookup, systemIndices));

        onlySystem = Arrays.asList(".foo", ".bar", ".test");
        assertTrue(bulkAction.isOnlySystem(buildBulkRequest(onlySystem), indicesLookup, systemIndices));

        List<String> nonSystem = Arrays.asList("foo", "bar");
        assertFalse(bulkAction.isOnlySystem(buildBulkRequest(nonSystem), indicesLookup, systemIndices));

        List<String> mixed = Arrays.asList(".foo", ".test", "other");
        assertFalse(bulkAction.isOnlySystem(buildBulkRequest(mixed), indicesLookup, systemIndices));
    }

    public void testIncludesSystem() {
        SortedMap<String, IndexAbstraction> indicesLookup = new TreeMap<>();
        Settings settings = Settings.builder().put("index.version.created", Version.CURRENT).build();
        indicesLookup.put(".foo",
            new Index(IndexMetadata.builder(".foo").settings(settings).system(true).numberOfShards(1).numberOfReplicas(0).build()));
        indicesLookup.put(".bar",
            new Index(IndexMetadata.builder(".bar").settings(settings).system(true).numberOfShards(1).numberOfReplicas(0).build()));
        SystemIndices systemIndices = new SystemIndices(org.elasticsearch.common.collect.Map.of("plugin",
            org.elasticsearch.common.collect.List.of(new SystemIndexDescriptor(".test", ""))));
        List<String> onlySystem = org.elasticsearch.common.collect.List.of(".foo", ".bar");
        assertTrue(bulkAction.includesSystem(buildBulkRequest(onlySystem), indicesLookup, systemIndices));

        onlySystem = org.elasticsearch.common.collect.List.of(".foo", ".bar", ".test");
        assertTrue(bulkAction.includesSystem(buildBulkRequest(onlySystem), indicesLookup, systemIndices));

        List<String> nonSystem = org.elasticsearch.common.collect.List.of("foo", "bar");
        assertFalse(bulkAction.includesSystem(buildBulkRequest(nonSystem), indicesLookup, systemIndices));

        List<String> mixed = org.elasticsearch.common.collect.List.of(".foo", ".test", "other");
        assertTrue(bulkAction.includesSystem(buildBulkRequest(mixed), indicesLookup, systemIndices));
    }

    public void testRejectionAfterCreateIndexIsPropagated() throws Exception {
        BulkRequest bulkRequest = new BulkRequest().add(new IndexRequest("index").id("id").source(Collections.emptyMap()));
        bulkAction.failIndexCreation = randomBoolean();

        try {
            threadPool.startForcingRejections();
            PlainActionFuture<BulkResponse> future = PlainActionFuture.newFuture();
            ActionTestUtils.execute(bulkAction, null, bulkRequest, future);
            expectThrows(EsRejectedExecutionException.class, future::actionGet);
        } finally {
            threadPool.stopForcingRejections();
        }
    }

    private BulkRequest buildBulkRequest(List<String> indices) {
        BulkRequest request = new BulkRequest();
        for (String index : indices) {
            final DocWriteRequest<?> subRequest;
            switch (randomIntBetween(1, 3)) {
                case 1:
                    subRequest = new IndexRequest(index);
                    break;
                case 2:
                    subRequest = new DeleteRequest(index).id("0");
                    break;
                case 3:
                    subRequest = new UpdateRequest(index, "0");
                    break;
                default:
                    throw new IllegalStateException("only have 3 cases");
            }
            request.add(subRequest);
        }
        return request;
    }
}
