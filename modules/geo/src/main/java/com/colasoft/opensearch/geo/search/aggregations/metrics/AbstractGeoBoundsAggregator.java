/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.geo.search.aggregations.metrics;

import com.colasoft.opensearch.common.lease.Releasables;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.common.util.DoubleArray;
import com.colasoft.opensearch.search.aggregations.Aggregator;
import com.colasoft.opensearch.search.aggregations.InternalAggregation;
import com.colasoft.opensearch.search.aggregations.metrics.MetricsAggregator;
import com.colasoft.opensearch.search.aggregations.support.ValuesSource;
import com.colasoft.opensearch.search.aggregations.support.ValuesSourceConfig;
import com.colasoft.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

/**
 * Abstract class for doing the {@link GeoBounds} Aggregation over fields of type geo_shape and geo_point.
 *
 * @param <T> Class extending the {@link ValuesSource} which will provide the data on which aggregation will happen.
 * @opensearch.internal
 */
public abstract class AbstractGeoBoundsAggregator<T extends ValuesSource> extends MetricsAggregator {

    protected final T valuesSource;
    protected final boolean wrapLongitude;
    protected DoubleArray tops;
    protected DoubleArray bottoms;
    protected DoubleArray posLefts;
    protected DoubleArray posRights;
    protected DoubleArray negLefts;
    protected DoubleArray negRights;

    @SuppressWarnings("unchecked")
    protected AbstractGeoBoundsAggregator(
        String name,
        SearchContext searchContext,
        Aggregator aggregator,
        ValuesSourceConfig valuesSourceConfig,
        boolean wrapLongitude,
        Map<String, Object> metaData
    ) throws IOException {
        super(name, searchContext, aggregator, metaData);
        this.wrapLongitude = wrapLongitude;
        valuesSource = valuesSourceConfig.hasValues() ? (T) valuesSourceConfig.getValuesSource() : null;

        if (valuesSource != null) {
            final BigArrays bigArrays = context.bigArrays();
            tops = bigArrays.newDoubleArray(1, false);
            tops.fill(0, tops.size(), Double.NEGATIVE_INFINITY);
            bottoms = bigArrays.newDoubleArray(1, false);
            bottoms.fill(0, bottoms.size(), Double.POSITIVE_INFINITY);
            posLefts = bigArrays.newDoubleArray(1, false);
            posLefts.fill(0, posLefts.size(), Double.POSITIVE_INFINITY);
            posRights = bigArrays.newDoubleArray(1, false);
            posRights.fill(0, posRights.size(), Double.NEGATIVE_INFINITY);
            negLefts = bigArrays.newDoubleArray(1, false);
            negLefts.fill(0, negLefts.size(), Double.POSITIVE_INFINITY);
            negRights = bigArrays.newDoubleArray(1, false);
            negRights.fill(0, negRights.size(), Double.NEGATIVE_INFINITY);
        }
    }

    /**
     * Build an empty aggregation.
     */
    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalGeoBounds(
            name,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            wrapLongitude,
            metadata()
        );
    }

    /**
     * Build an aggregation for data that has been collected into owningBucketOrd.
     */
    @Override
    public InternalAggregation buildAggregation(long owningBucketOrdinal) throws IOException {
        if (valuesSource == null) {
            return buildEmptyAggregation();
        }
        double top = tops.get(owningBucketOrdinal);
        double bottom = bottoms.get(owningBucketOrdinal);
        double posLeft = posLefts.get(owningBucketOrdinal);
        double posRight = posRights.get(owningBucketOrdinal);
        double negLeft = negLefts.get(owningBucketOrdinal);
        double negRight = negRights.get(owningBucketOrdinal);
        return new InternalGeoBounds(name, top, bottom, posLeft, posRight, negLeft, negRight, wrapLongitude, metadata());
    }

    @Override
    public void doClose() {
        Releasables.close(tops, bottoms, posLefts, posRights, negLefts, negRights);
    }

    protected void setBucketSize(final long bucket, final BigArrays bigArrays) {
        if (bucket >= tops.size()) {
            long from = tops.size();
            tops = bigArrays.grow(tops, bucket + 1);
            tops.fill(from, tops.size(), Double.NEGATIVE_INFINITY);
            bottoms = bigArrays.resize(bottoms, tops.size());
            bottoms.fill(from, bottoms.size(), Double.POSITIVE_INFINITY);
            posLefts = bigArrays.resize(posLefts, tops.size());
            posLefts.fill(from, posLefts.size(), Double.POSITIVE_INFINITY);
            posRights = bigArrays.resize(posRights, tops.size());
            posRights.fill(from, posRights.size(), Double.NEGATIVE_INFINITY);
            negLefts = bigArrays.resize(negLefts, tops.size());
            negLefts.fill(from, negLefts.size(), Double.POSITIVE_INFINITY);
            negRights = bigArrays.resize(negRights, tops.size());
            negRights.fill(from, negRights.size(), Double.NEGATIVE_INFINITY);
        }
    }
}
