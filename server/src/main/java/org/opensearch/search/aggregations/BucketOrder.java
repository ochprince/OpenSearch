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

package org.opensearch.search.aggregations;

import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.search.aggregations.bucket.MultiBucketsAggregation.Bucket;
import org.opensearch.search.aggregations.support.AggregationPath;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * {@link Bucket} ordering strategy. Buckets can be order either as
 * "complete" buckets using {@link #comparator()} or against a combination
 * of the buckets internals with its ordinal with
 * {@link #partiallyBuiltBucketComparator(ToLongFunction, Aggregator)}.
 */
public abstract class BucketOrder implements ToXContentObject, Writeable {
    /**
     * Creates a bucket ordering strategy that sorts buckets by their document counts (ascending or descending).
     *
     * @param asc direction to sort by: {@code true} for ascending, {@code false} for descending.
     */
    public static BucketOrder count(boolean asc) {
        return asc ? InternalOrder.COUNT_ASC : InternalOrder.COUNT_DESC;
    }

    /**
     * Creates a bucket ordering strategy that sorts buckets by their keys (ascending or descending). This may be
     * used as a tie-breaker to avoid non-deterministic ordering.
     *
     * @param asc direction to sort by: {@code true} for ascending, {@code false} for descending.
     */
    public static BucketOrder key(boolean asc) {
        return asc ? InternalOrder.KEY_ASC : InternalOrder.KEY_DESC;
    }

    /**
     * Creates a bucket ordering strategy which sorts buckets based on a single-valued sub-aggregation.
     *
     * @param path path to the sub-aggregation to sort on.
     * @param asc  direction to sort by: {@code true} for ascending, {@code false} for descending.
     * @see AggregationPath
     */
    public static BucketOrder aggregation(String path, boolean asc) {
        return new InternalOrder.Aggregation(path, asc);
    }

    /**
     * Creates a bucket ordering strategy which sorts buckets based on a metric from a multi-valued sub-aggregation.
     *
     * @param path       path to the sub-aggregation to sort on.
     * @param metricName name of the value of the multi-value metric to sort on.
     * @param asc        direction to sort by: {@code true} for ascending, {@code false} for descending.
     * @see AggregationPath
     */
    public static BucketOrder aggregation(String path, String metricName, boolean asc) {
        return new InternalOrder.Aggregation(path + "." + metricName, asc);
    }

    /**
     * Creates a bucket ordering strategy which sorts buckets based on multiple criteria. A tie-breaker may be added to
     * avoid non-deterministic ordering.
     *
     * @param orders a list of {@link BucketOrder} objects to sort on, in order of priority.
     */
    public static BucketOrder compound(List<BucketOrder> orders) {
        return new InternalOrder.CompoundOrder(orders);
    }

    /**
     * Creates a bucket ordering strategy which sorts buckets based on multiple criteria. A tie-breaker may be added to
     * avoid non-deterministic ordering.
     *
     * @param orders a list of {@link BucketOrder} parameters to sort on, in order of priority.
     */
    public static BucketOrder compound(BucketOrder... orders) {
        return compound(Arrays.asList(orders));
    }

    /**
     * Validate an aggregation against an {@linkplain Aggregator}.
     * @throws AggregationExecutionException when the ordering is invalid
     *         for this {@linkplain Aggregator}.
     */
    public final void validate(Aggregator aggregator) throws AggregationExecutionException{
        /*
         * Building partiallyBuiltBucketComparator and throwing it away is enough
         * to validate this order because doing so checks all of the appropriate
         * paths.
         */
        partiallyBuiltBucketComparator(null, aggregator);
    }

    /**
     * A builds comparator comparing buckets partially built buckets by
     * delegating comparison of the results of any "child" aggregations to
     * the provided {@linkplain Aggregator}.
     * <p>
     * Warning: This is fairly difficult to use and impossible to use cleanly.
     * In addition, this exists primarily to return the "top n" buckets based
     * on the results of a sub aggregation. The trouble is that could end up
     * throwing away buckets on the data nodes that should ultimately be kept
     * after reducing all of the results. If you know that this is coming it
     * is fine, but most folks that use "generic" sorts don't. In other words:
     * before you use this method think super duper hard if you want to have
     * these kinds of issues. The terms agg does an folks get into trouble
     * with it all the time.
     * </p>
     */
    public abstract <T extends Bucket> Comparator<T> partiallyBuiltBucketComparator(ToLongFunction<T> ordinalReader, Aggregator aggregator);

    /**
     * Build a comparator for fully built buckets.
     */
    public abstract Comparator<Bucket> comparator();

    /**
     * @return unique internal ID used for reading/writing this order from/to a stream.
     * @see InternalOrder.Streams
     */
    abstract byte id();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        InternalOrder.Streams.writeOrder(this, out);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
