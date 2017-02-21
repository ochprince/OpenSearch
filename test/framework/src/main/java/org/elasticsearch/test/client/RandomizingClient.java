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

package org.elasticsearch.test.client;

import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import org.apache.lucene.util.TestUtil;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.FilterClient;
import org.elasticsearch.cluster.routing.Preference;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

/** A {@link Client} that randomizes request parameters. */
public class RandomizingClient extends FilterClient {

    private final SearchType defaultSearchType;
    private final String defaultPreference;
    private final int reduceUpTo;


    public RandomizingClient(Client client, Random random) {
        super(client);
        // we don't use the QUERY_AND_FETCH types that break quite a lot of tests
        // given that they return `size*num_shards` hits instead of `size`
        defaultSearchType = RandomPicks.randomFrom(random, Arrays.asList(
                SearchType.DFS_QUERY_THEN_FETCH,
                SearchType.QUERY_THEN_FETCH));
        if (random.nextInt(10) == 0) {
            defaultPreference = RandomPicks.randomFrom(random, EnumSet.of(Preference.PRIMARY_FIRST, Preference.LOCAL)).type();
        } else if (random.nextInt(10) == 0) {
            String s = TestUtil.randomRealisticUnicodeString(random, 1, 10);
            defaultPreference = s.startsWith("_") ? null : s; // '_' is a reserved character
        } else {
            defaultPreference = null;
        }
        this.reduceUpTo = 2 + random.nextInt(10);

    }

    @Override
    public SearchRequestBuilder prepareSearch(String... indices) {
        return in.prepareSearch(indices).setSearchType(defaultSearchType).setPreference(defaultPreference).setReduceUpTo(reduceUpTo);
    }

    @Override
    public String toString() {
        return "randomized(" + super.toString() + ")";
    }

}
