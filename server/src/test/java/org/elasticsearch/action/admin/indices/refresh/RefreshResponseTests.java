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

package org.elasticsearch.action.admin.indices.refresh;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.test.XContentTestUtils.insertRandomFields;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

public class RefreshResponseTests extends ESTestCase {

    public void testToXContent() {
        RefreshResponse response = new RefreshResponse(10, 10, 0, null);
        String output = Strings.toString(response);
        assertEquals("{\"_shards\":{\"total\":10,\"successful\":10,\"failed\":0}}", output);
    }

    public void testToAndFromXContent() throws IOException {
        doFromXContentTestWithRandomFields(false);
    }

    public void testFromXContentWithRandomFields() throws IOException {
        doFromXContentTestWithRandomFields(true);
    }

    public void testFailuresDeduplication() throws IOException {
        List<DefaultShardOperationFailedException> failures = new ArrayList<>();
        Index index = new Index("test", "_na_");
        ElasticsearchException exception1 = new ElasticsearchException("foo", new IllegalArgumentException("bar"));
        exception1.setIndex(index);
        exception1.setShard(new ShardId(index, 0));
        ElasticsearchException exception2 = new ElasticsearchException("foo", new IllegalArgumentException("bar"));
        exception2.setIndex(index);
        exception2.setShard(new ShardId(index, 1));
        ElasticsearchException exception3 = new ElasticsearchException("fizz", new IllegalStateException("buzz"));
        exception3.setIndex(index);
        exception3.setShard(new ShardId(index, 2));
        failures.add(new DefaultShardOperationFailedException(exception1));
        failures.add(new DefaultShardOperationFailedException(exception2));
        failures.add(new DefaultShardOperationFailedException(exception3));

        RefreshResponse response = new RefreshResponse(10, 7, 3, failures);
        boolean humanReadable = randomBoolean();
        XContentType xContentType = randomFrom(XContentType.values());
        BytesReference bytesReference = toShuffledXContent(response, xContentType, ToXContent.EMPTY_PARAMS, humanReadable);
        RefreshResponse parsedResponse;
        try(XContentParser parser = createParser(xContentType.xContent(), bytesReference)) {
            parsedResponse = RefreshResponse.fromXContent(parser);
            assertNull(parser.nextToken());
        }

        assertThat(parsedResponse.getShardFailures().length, equalTo(2));
        DefaultShardOperationFailedException[] parsedFailures = parsedResponse.getShardFailures();
        assertThat(parsedFailures[0].index(), equalTo("test"));
        assertThat(parsedFailures[0].shardId(), anyOf(equalTo(0), equalTo(1)));
        assertThat(parsedFailures[0].status(), equalTo(RestStatus.INTERNAL_SERVER_ERROR));
        assertThat(parsedFailures[0].getCause().getMessage(), containsString("foo"));
        assertThat(parsedFailures[1].index(), equalTo("test"));
        assertThat(parsedFailures[1].shardId(), equalTo(2));
        assertThat(parsedFailures[1].status(), equalTo(RestStatus.INTERNAL_SERVER_ERROR));
        assertThat(parsedFailures[1].getCause().getMessage(), containsString("fizz"));

        ToXContent.Params params = new ToXContent.MapParams(Collections.singletonMap("group_shard_failures", "false"));
        BytesReference bytesReferenceWithoutDedup = toShuffledXContent(response, xContentType, params, humanReadable);
        try(XContentParser parser = createParser(xContentType.xContent(), bytesReferenceWithoutDedup)) {
            parsedResponse = RefreshResponse.fromXContent(parser);
            assertNull(parser.nextToken());
        }

        assertThat(parsedResponse.getShardFailures().length, equalTo(3));
        parsedFailures = parsedResponse.getShardFailures();
        for (int i = 0; i < 3; i++) {
            if (i < 2) {
                assertThat(parsedFailures[i].index(), equalTo("test"));
                assertThat(parsedFailures[i].shardId(), equalTo(i));
                assertThat(parsedFailures[i].status(), equalTo(RestStatus.INTERNAL_SERVER_ERROR));
                assertThat(parsedFailures[i].getCause().getMessage(), containsString("foo"));
            } else {
                assertThat(parsedFailures[i].index(), equalTo("test"));
                assertThat(parsedFailures[i].shardId(), equalTo(i));
                assertThat(parsedFailures[i].status(), equalTo(RestStatus.INTERNAL_SERVER_ERROR));
                assertThat(parsedFailures[i].getCause().getMessage(), containsString("fizz"));
            }
        }
    }

    private void doFromXContentTestWithRandomFields(boolean addRandomFields) throws IOException {
        RefreshResponse response = createTestItem(10);
        boolean humanReadable = randomBoolean();
        XContentType xContentType = randomFrom(XContentType.values());
        BytesReference bytesReference = toShuffledXContent(response, xContentType, ToXContent.EMPTY_PARAMS, humanReadable);
        if (addRandomFields) {
            bytesReference = insertRandomFields(xContentType, bytesReference, null, random());
        }
        RefreshResponse parsedResponse;
        try(XContentParser parser = createParser(xContentType.xContent(), bytesReference)) {
            parsedResponse = RefreshResponse.fromXContent(parser);
            assertNull(parser.nextToken());
        }

        assertThat(response.getTotalShards(), equalTo(parsedResponse.getTotalShards()));
        assertThat(response.getSuccessfulShards(), equalTo(parsedResponse.getSuccessfulShards()));
        assertThat(response.getFailedShards(), equalTo(parsedResponse.getFailedShards()));
        assertFailureEquals(response.getShardFailures(), parsedResponse.getShardFailures());
    }

    private static void assertFailureEquals(DefaultShardOperationFailedException[] original,
                                            DefaultShardOperationFailedException[] parsedback) {
        assertThat(original.length, equalTo(parsedback.length));
        for (int i = 0; i < original.length; i++) {
            assertThat(original[i].index(), equalTo(parsedback[i].index()));
            assertThat(original[i].shardId(), equalTo(parsedback[i].shardId()));
            assertThat(original[i].status(), equalTo(parsedback[i].status()));
            assertThat(parsedback[i].getCause().getMessage(), containsString(original[i].getCause().getMessage()));
        }
    }

    private static RefreshResponse createTestItem(int totalShards) {
        List<DefaultShardOperationFailedException> failures = null;
        int successfulShards = randomInt(totalShards);
        int failedShards = totalShards - successfulShards;
        if (failedShards > 0) {
            failures = new ArrayList<>();
            for (int i = 0; i < failedShards; i++) {
                ElasticsearchException exception = new ElasticsearchException("exception message " + i);
                exception.setIndex(new Index("index" + i, "_na_"));
                exception.setShard(new ShardId("index" + i, "_na_", i));
                if (randomBoolean()) {
                    failures.add(new DefaultShardOperationFailedException(exception));
                } else {
                    failures.add(new DefaultShardOperationFailedException("index" + i, i, new Exception("exception message " + i)));
                }
            }
        }
        return new RefreshResponse(totalShards, successfulShards, failedShards, failures);
    }
}
