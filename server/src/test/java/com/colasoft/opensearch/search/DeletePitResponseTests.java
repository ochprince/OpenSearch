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

package com.colasoft.opensearch.search;

import com.colasoft.opensearch.action.search.DeletePitInfo;
import com.colasoft.opensearch.action.search.DeletePitResponse;
import com.colasoft.opensearch.common.bytes.BytesReference;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.common.xcontent.XContentHelper;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.common.xcontent.json.JsonXContent;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertToXContentEquivalent;

public class DeletePitResponseTests extends OpenSearchTestCase {

    public void testDeletePitResponseToXContent() throws IOException {
        DeletePitInfo deletePitInfo = new DeletePitInfo(true, "pitId");
        List<DeletePitInfo> deletePitInfoList = new ArrayList<>();
        deletePitInfoList.add(deletePitInfo);
        DeletePitResponse deletePitResponse = new DeletePitResponse(deletePitInfoList);

        try (XContentBuilder builder = JsonXContent.contentBuilder()) {
            deletePitResponse.toXContent(builder, ToXContent.EMPTY_PARAMS);
        }
        assertEquals(true, deletePitResponse.getDeletePitResults().get(0).getPitId().equals("pitId"));
        assertEquals(true, deletePitResponse.getDeletePitResults().get(0).isSuccessful());
    }

    public void testDeletePitResponseToAndFromXContent() throws IOException {
        XContentType xContentType = randomFrom(XContentType.values());
        DeletePitResponse originalResponse = createDeletePitResponseTestItem();

        BytesReference originalBytes = toShuffledXContent(originalResponse, xContentType, ToXContent.EMPTY_PARAMS, randomBoolean());
        DeletePitResponse parsedResponse;
        try (XContentParser parser = createParser(xContentType.xContent(), originalBytes)) {
            parsedResponse = DeletePitResponse.fromXContent(parser);
        }
        assertEquals(
            originalResponse.getDeletePitResults().get(0).isSuccessful(),
            parsedResponse.getDeletePitResults().get(0).isSuccessful()
        );
        assertEquals(originalResponse.getDeletePitResults().get(0).getPitId(), parsedResponse.getDeletePitResults().get(0).getPitId());
        BytesReference parsedBytes = XContentHelper.toXContent(parsedResponse, xContentType, randomBoolean());
        assertToXContentEquivalent(originalBytes, parsedBytes, xContentType);
    }

    private static DeletePitResponse createDeletePitResponseTestItem() {
        DeletePitInfo deletePitInfo = new DeletePitInfo(randomBoolean(), "pitId");
        List<DeletePitInfo> deletePitInfoList = new ArrayList<>();
        deletePitInfoList.add(deletePitInfo);
        return new DeletePitResponse(deletePitInfoList);
    }
}
