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

package org.opensearch.client;

import org.opensearch.action.ActionFuture;
import org.opensearch.action.ActionListener;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkRequestBuilder;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteRequestBuilder;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.explain.ExplainRequest;
import org.opensearch.action.explain.ExplainRequestBuilder;
import org.opensearch.action.explain.ExplainResponse;
import org.opensearch.action.fieldcaps.FieldCapabilitiesRequest;
import org.opensearch.action.fieldcaps.FieldCapabilitiesRequestBuilder;
import org.opensearch.action.fieldcaps.FieldCapabilitiesResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetRequestBuilder;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.get.MultiGetRequestBuilder;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexRequestBuilder;
import org.opensearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollRequestBuilder;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.opensearch.action.termvectors.MultiTermVectorsRequest;
import org.opensearch.action.termvectors.MultiTermVectorsRequestBuilder;
import org.opensearch.action.termvectors.MultiTermVectorsResponse;
import org.opensearch.action.termvectors.TermVectorsRequest;
import org.opensearch.action.termvectors.TermVectorsRequestBuilder;
import org.opensearch.action.termvectors.TermVectorsResponse;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.action.update.UpdateRequestBuilder;
import org.opensearch.action.update.UpdateResponse;
import org.elasticsearch.common.Nullable;
import org.opensearch.common.lease.Releasable;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;

import java.util.Map;

/**
 * A client provides a one stop interface for performing actions/operations against the cluster.
 * <p>
 * All operations performed are asynchronous by nature. Each action/operation has two flavors, the first
 * simply returns an {@link org.opensearch.action.ActionFuture}, while the second accepts an
 * {@link ActionListener}.
 * <p>
 * A client can either be retrieved from a {@link org.opensearch.node.Node} started, or connected remotely
 * to one or more nodes using {@link org.opensearch.client.transport.TransportClient}.
 *
 * @see org.opensearch.node.Node#client()
 * @see org.opensearch.client.transport.TransportClient
 */
public interface Client extends OpenSearchClient, Releasable {

    Setting<String> CLIENT_TYPE_SETTING_S = new Setting<>("client.type", "node", (s) -> {
        switch (s) {
            case "node":
            case "transport":
                return s;
            default:
                throw new IllegalArgumentException("Can't parse [client.type] must be one of [node, transport]");
        }
    }, Property.NodeScope);

    /**
     * The admin client that can be used to perform administrative operations.
     */
    AdminClient admin();


    /**
     * Index a JSON source associated with a given index and type.
     * <p>
     * The id is optional, if it is not provided, one will be generated automatically.
     *
     * @param request The index request
     * @return The result future
     * @see Requests#indexRequest(String)
     */
    ActionFuture<IndexResponse> index(IndexRequest request);

    /**
     * Index a document associated with a given index and type.
     * <p>
     * The id is optional, if it is not provided, one will be generated automatically.
     *
     * @param request  The index request
     * @param listener A listener to be notified with a result
     * @see Requests#indexRequest(String)
     */
    void index(IndexRequest request, ActionListener<IndexResponse> listener);

    /**
     * Index a document associated with a given index and type.
     * <p>
     * The id is optional, if it is not provided, one will be generated automatically.
     */
    IndexRequestBuilder prepareIndex();

    /**
     * Updates a document based on a script.
     *
     * @param request The update request
     * @return The result future
     */
    ActionFuture<UpdateResponse> update(UpdateRequest request);

    /**
     * Updates a document based on a script.
     *
     * @param request  The update request
     * @param listener A listener to be notified with a result
     */
    void update(UpdateRequest request, ActionListener<UpdateResponse> listener);

    /**
     * Updates a document based on a script.
     */
    UpdateRequestBuilder prepareUpdate();

    /**
     * Updates a document based on a script.
     */
    UpdateRequestBuilder prepareUpdate(String index, String type, String id);

    /**
     * Index a document associated with a given index and type.
     * <p>
     * The id is optional, if it is not provided, one will be generated automatically.
     *
     * @param index The index to index the document to
     * @param type  The type to index the document to
     */
    IndexRequestBuilder prepareIndex(String index, String type);

    /**
     * Index a document associated with a given index and type.
     * <p>
     * The id is optional, if it is not provided, one will be generated automatically.
     *
     * @param index The index to index the document to
     * @param type  The type to index the document to
     * @param id    The id of the document
     */
    IndexRequestBuilder prepareIndex(String index, String type, @Nullable String id);

    /**
     * Deletes a document from the index based on the index, type and id.
     *
     * @param request The delete request
     * @return The result future
     * @see Requests#deleteRequest(String)
     */
    ActionFuture<DeleteResponse> delete(DeleteRequest request);

    /**
     * Deletes a document from the index based on the index, type and id.
     *
     * @param request  The delete request
     * @param listener A listener to be notified with a result
     * @see Requests#deleteRequest(String)
     */
    void delete(DeleteRequest request, ActionListener<DeleteResponse> listener);

    /**
     * Deletes a document from the index based on the index, type and id.
     */
    DeleteRequestBuilder prepareDelete();

    /**
     * Deletes a document from the index based on the index, type and id.
     *
     * @param index The index to delete the document from
     * @param type  The type of the document to delete
     * @param id    The id of the document to delete
     */
    DeleteRequestBuilder prepareDelete(String index, String type, String id);

    /**
     * Executes a bulk of index / delete operations.
     *
     * @param request The bulk request
     * @return The result future
     * @see Requests#bulkRequest()
     */
    ActionFuture<BulkResponse> bulk(BulkRequest request);

    /**
     * Executes a bulk of index / delete operations.
     *
     * @param request  The bulk request
     * @param listener A listener to be notified with a result
     * @see Requests#bulkRequest()
     */
    void bulk(BulkRequest request, ActionListener<BulkResponse> listener);

    /**
     * Executes a bulk of index / delete operations.
     */
    BulkRequestBuilder prepareBulk();

    /**
     * Executes a bulk of index / delete operations with default index and/or type
     */
    BulkRequestBuilder prepareBulk(@Nullable String globalIndex, @Nullable String globalType);

    /**
     * Gets the document that was indexed from an index with a type and id.
     *
     * @param request The get request
     * @return The result future
     * @see Requests#getRequest(String)
     */
    ActionFuture<GetResponse> get(GetRequest request);

    /**
     * Gets the document that was indexed from an index with a type and id.
     *
     * @param request  The get request
     * @param listener A listener to be notified with a result
     * @see Requests#getRequest(String)
     */
    void get(GetRequest request, ActionListener<GetResponse> listener);

    /**
     * Gets the document that was indexed from an index with a type and id.
     */
    GetRequestBuilder prepareGet();

    /**
     * Gets the document that was indexed from an index with a type (optional) and id.
     */
    GetRequestBuilder prepareGet(String index, @Nullable String type, String id);

    /**
     * Multi get documents.
     */
    ActionFuture<MultiGetResponse> multiGet(MultiGetRequest request);

    /**
     * Multi get documents.
     */
    void multiGet(MultiGetRequest request, ActionListener<MultiGetResponse> listener);

    /**
     * Multi get documents.
     */
    MultiGetRequestBuilder prepareMultiGet();

    /**
     * Search across one or more indices and one or more types with a query.
     *
     * @param request The search request
     * @return The result future
     * @see Requests#searchRequest(String...)
     */
    ActionFuture<SearchResponse> search(SearchRequest request);

    /**
     * Search across one or more indices and one or more types with a query.
     *
     * @param request  The search request
     * @param listener A listener to be notified of the result
     * @see Requests#searchRequest(String...)
     */
    void search(SearchRequest request, ActionListener<SearchResponse> listener);

    /**
     * Search across one or more indices and one or more types with a query.
     */
    SearchRequestBuilder prepareSearch(String... indices);

    /**
     * A search scroll request to continue searching a previous scrollable search request.
     *
     * @param request The search scroll request
     * @return The result future
     * @see Requests#searchScrollRequest(String)
     */
    ActionFuture<SearchResponse> searchScroll(SearchScrollRequest request);

    /**
     * A search scroll request to continue searching a previous scrollable search request.
     *
     * @param request  The search scroll request
     * @param listener A listener to be notified of the result
     * @see Requests#searchScrollRequest(String)
     */
    void searchScroll(SearchScrollRequest request, ActionListener<SearchResponse> listener);

    /**
     * A search scroll request to continue searching a previous scrollable search request.
     */
    SearchScrollRequestBuilder prepareSearchScroll(String scrollId);

    /**
     * Performs multiple search requests.
     */
    ActionFuture<MultiSearchResponse> multiSearch(MultiSearchRequest request);

    /**
     * Performs multiple search requests.
     */
    void multiSearch(MultiSearchRequest request, ActionListener<MultiSearchResponse> listener);

    /**
     * Performs multiple search requests.
     */
    MultiSearchRequestBuilder prepareMultiSearch();

    /**
     * An action that returns the term vectors for a specific document.
     *
     * @param request The term vector request
     * @return The response future
     */
    ActionFuture<TermVectorsResponse> termVectors(TermVectorsRequest request);

    /**
     * An action that returns the term vectors for a specific document.
     *
     * @param request The term vector request
     */
    void termVectors(TermVectorsRequest request, ActionListener<TermVectorsResponse> listener);

    /**
     * Builder for the term vector request.
     */
    TermVectorsRequestBuilder prepareTermVectors();

    /**
     * Builder for the term vector request.
     *
     * @param index The index to load the document from
     * @param type  The type of the document
     * @param id    The id of the document
     */
    TermVectorsRequestBuilder prepareTermVectors(String index, String type, String id);

    /**
     * Multi get term vectors.
     */
    ActionFuture<MultiTermVectorsResponse> multiTermVectors(MultiTermVectorsRequest request);

    /**
     * Multi get term vectors.
     */
    void multiTermVectors(MultiTermVectorsRequest request, ActionListener<MultiTermVectorsResponse> listener);

    /**
     * Multi get term vectors.
     */
    MultiTermVectorsRequestBuilder prepareMultiTermVectors();

    /**
     * Computes a score explanation for the specified request.
     *
     * @param index The index this explain is targeted for
     * @param type  The type this explain is targeted for
     * @param id    The document identifier this explain is targeted for
     */
    ExplainRequestBuilder prepareExplain(String index, String type, String id);

    /**
     * Computes a score explanation for the specified request.
     *
     * @param request The request encapsulating the query and document identifier to compute a score explanation for
     */
    ActionFuture<ExplainResponse> explain(ExplainRequest request);

    /**
     * Computes a score explanation for the specified request.
     *
     * @param request  The request encapsulating the query and document identifier to compute a score explanation for
     * @param listener A listener to be notified of the result
     */
    void explain(ExplainRequest request, ActionListener<ExplainResponse> listener);

    /**
     * Clears the search contexts associated with specified scroll ids.
     */
    ClearScrollRequestBuilder prepareClearScroll();

    /**
     * Clears the search contexts associated with specified scroll ids.
     */
    ActionFuture<ClearScrollResponse> clearScroll(ClearScrollRequest request);

    /**
     * Clears the search contexts associated with specified scroll ids.
     */
    void clearScroll(ClearScrollRequest request, ActionListener<ClearScrollResponse> listener);

    /**
     * Builder for the field capabilities request.
     */
    FieldCapabilitiesRequestBuilder prepareFieldCaps(String... indices);

    /**
     * An action that returns the field capabilities from the provided request
     */
    ActionFuture<FieldCapabilitiesResponse> fieldCaps(FieldCapabilitiesRequest request);

    /**
     * An action that returns the field capabilities from the provided request
     */
    void fieldCaps(FieldCapabilitiesRequest request, ActionListener<FieldCapabilitiesResponse> listener);

    /**
     * Returns this clients settings
     */
    Settings settings();

    /**
     * Returns a new lightweight Client that applies all given headers to each of the requests
     * issued from it.
     */
    Client filterWithHeader(Map<String, String> headers);

    /**
     * Returns a client to a remote cluster with the given cluster alias.
     *
     * @throws IllegalArgumentException if the given clusterAlias doesn't exist
     * @throws UnsupportedOperationException if this functionality is not available on this client.
     */
    default Client getRemoteClusterClient(String clusterAlias) {
        throw new UnsupportedOperationException("this client doesn't support remote cluster connections");
    }
}
