/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.client;

import com.colasoft.opensearch.action.search.DeleteSearchPipelineRequest;
import com.colasoft.opensearch.action.search.GetSearchPipelineRequest;
import com.colasoft.opensearch.action.search.GetSearchPipelineResponse;
import com.colasoft.opensearch.action.search.PutSearchPipelineRequest;
import com.colasoft.opensearch.action.support.master.AcknowledgedResponse;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

public class SearchPipelineClientIT extends OpenSearchRestHighLevelClientTestCase {

    public void testPutPipeline() throws IOException {
        String id = "some_pipeline_id";
        XContentBuilder pipelineBuilder = buildSearchPipeline();
        PutSearchPipelineRequest request = new PutSearchPipelineRequest(
            id,
            BytesReference.bytes(pipelineBuilder),
            pipelineBuilder.contentType()
        );
        createPipeline(request);
    }

    private static void createPipeline(PutSearchPipelineRequest request) throws IOException {
        AcknowledgedResponse response = execute(
            request,
            highLevelClient().searchPipeline()::put,
            highLevelClient().searchPipeline()::putAsync
        );
        assertTrue(response.isAcknowledged());
    }

    public void testGetPipeline() throws IOException {
        String id = "some_pipeline_id";
        XContentBuilder pipelineBuilder = buildSearchPipeline();
        PutSearchPipelineRequest request = new PutSearchPipelineRequest(
            id,
            BytesReference.bytes(pipelineBuilder),
            pipelineBuilder.contentType()
        );
        createPipeline(request);

        GetSearchPipelineRequest getRequest = new GetSearchPipelineRequest(id);
        GetSearchPipelineResponse response = execute(
            getRequest,
            highLevelClient().searchPipeline()::get,
            highLevelClient().searchPipeline()::getAsync
        );
        assertTrue(response.isFound());
        assertEquals(1, response.pipelines().size());
        assertEquals(id, response.pipelines().get(0).getId());
    }

    public void testDeletePipeline() throws IOException {
        String id = "some_pipeline_id";
        XContentBuilder pipelineBuilder = buildSearchPipeline();
        PutSearchPipelineRequest request = new PutSearchPipelineRequest(
            id,
            BytesReference.bytes(pipelineBuilder),
            pipelineBuilder.contentType()
        );
        createPipeline(request);

        DeleteSearchPipelineRequest deleteRequest = new DeleteSearchPipelineRequest(id);
        AcknowledgedResponse response = execute(
            deleteRequest,
            highLevelClient().searchPipeline()::delete,
            highLevelClient().searchPipeline()::deleteAsync
        );
        assertTrue(response.isAcknowledged());
    }

    private static XContentBuilder buildSearchPipeline() throws IOException {
        XContentType xContentType = randomFrom(XContentType.values());
        XContentBuilder pipelineBuilder = XContentBuilder.builder(xContentType.xContent());
        return buildSearchPipeline(pipelineBuilder);
    }

    private static XContentBuilder buildSearchPipeline(XContentBuilder builder) throws IOException {
        builder.startObject();
        {
            builder.field("description", "a pipeline description");
            builder.startArray("request_processors");
            {
                builder.startObject().startObject("filter_query");
                {
                    builder.startObject("query");
                    {
                        builder.startObject("term");
                        {
                            builder.field("field", "value");
                        }
                        builder.endObject();
                    }
                    builder.endObject();
                }
                builder.endObject().endObject();
            }
            builder.endArray();
        }
        builder.endObject();
        return builder;
    }
}
