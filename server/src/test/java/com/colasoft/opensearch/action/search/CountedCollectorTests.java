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

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.action.OriginalIndices;
import com.colasoft.opensearch.common.UUIDs;
import com.colasoft.opensearch.common.util.concurrent.AtomicArray;
import com.colasoft.opensearch.index.shard.ShardId;
import com.colasoft.opensearch.search.SearchPhaseResult;
import com.colasoft.opensearch.search.SearchShardTarget;
import com.colasoft.opensearch.search.dfs.DfsSearchResult;
import com.colasoft.opensearch.search.internal.ShardSearchContextId;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class CountedCollectorTests extends OpenSearchTestCase {
    public void testCollect() throws InterruptedException {
        ArraySearchPhaseResults<SearchPhaseResult> consumer = new ArraySearchPhaseResults<>(randomIntBetween(1, 100));
        List<Integer> state = new ArrayList<>();
        int numResultsExpected = randomIntBetween(1, consumer.getAtomicArray().length());
        MockSearchPhaseContext context = new MockSearchPhaseContext(consumer.getAtomicArray().length());
        CountDownLatch latch = new CountDownLatch(1);
        boolean maybeFork = randomBoolean();
        Executor executor = (runnable) -> {
            if (randomBoolean() && maybeFork) {
                new Thread(runnable).start();

            } else {
                runnable.run();
            }
        };
        CountedCollector<SearchPhaseResult> collector = new CountedCollector<>(consumer, numResultsExpected, latch::countDown, context);
        for (int i = 0; i < numResultsExpected; i++) {
            int shardID = i;
            switch (randomIntBetween(0, 2)) {
                case 0:
                    state.add(0);
                    executor.execute(() -> collector.countDown());
                    break;
                case 1:
                    state.add(1);
                    executor.execute(() -> {
                        DfsSearchResult dfsSearchResult = new DfsSearchResult(
                            new ShardSearchContextId(UUIDs.randomBase64UUID(), shardID),
                            null,
                            null
                        );
                        dfsSearchResult.setShardIndex(shardID);
                        dfsSearchResult.setSearchShardTarget(
                            new SearchShardTarget("foo", new ShardId("bar", "baz", shardID), null, OriginalIndices.NONE)
                        );
                        collector.onResult(dfsSearchResult);
                    });
                    break;
                case 2:
                    state.add(2);
                    executor.execute(
                        () -> collector.onFailure(
                            shardID,
                            new SearchShardTarget("foo", new ShardId("bar", "baz", shardID), null, OriginalIndices.NONE),
                            new RuntimeException("boom")
                        )
                    );
                    break;
                default:
                    fail("unknown state");
            }
        }
        latch.await();
        assertEquals(numResultsExpected, state.size());
        AtomicArray<SearchPhaseResult> results = consumer.getAtomicArray();
        for (int i = 0; i < numResultsExpected; i++) {
            switch (state.get(i)) {
                case 0:
                    assertNull(results.get(i));
                    break;
                case 1:
                    assertNotNull(results.get(i));
                    assertEquals(i, results.get(i).getContextId().getId());
                    break;
                case 2:
                    final int shardId = i;
                    assertEquals(1, context.failures.stream().filter(f -> f.shardId() == shardId).count());
                    break;
                default:
                    fail("unknown state");
            }
        }

        for (int i = numResultsExpected; i < results.length(); i++) {
            assertNull("index: " + i, results.get(i));
        }
    }
}
