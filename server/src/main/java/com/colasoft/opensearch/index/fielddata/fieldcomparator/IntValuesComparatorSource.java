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
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.index.fielddata.fieldcomparator;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.comparators.IntComparator;
import org.apache.lucene.util.BitSet;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.common.util.BigArrays;
import com.colasoft.opensearch.index.fielddata.FieldData;
import com.colasoft.opensearch.index.fielddata.IndexFieldData;
import com.colasoft.opensearch.index.fielddata.IndexNumericFieldData;
import com.colasoft.opensearch.search.DocValueFormat;
import com.colasoft.opensearch.search.MultiValueMode;
import com.colasoft.opensearch.search.sort.BucketedSort;
import com.colasoft.opensearch.search.sort.SortOrder;

import java.io.IOException;

/**
 * Comparator source for int values.
 *
 * @opensearch.internal
 */
public class IntValuesComparatorSource extends IndexFieldData.XFieldComparatorSource {

    private final IndexNumericFieldData indexFieldData;

    public IntValuesComparatorSource(
        IndexNumericFieldData indexFieldData,
        @Nullable Object missingValue,
        MultiValueMode sortMode,
        Nested nested
    ) {
        super(missingValue, sortMode, nested);
        this.indexFieldData = indexFieldData;
    }

    @Override
    public SortField.Type reducedType() {
        return SortField.Type.INT;
    }

    private NumericDocValues getNumericDocValues(LeafReaderContext context, int missingValue) throws IOException {
        final SortedNumericDocValues values = indexFieldData.load(context).getLongValues();
        if (nested == null) {
            return FieldData.replaceMissing(sortMode.select(values), missingValue);
        }
        final BitSet rootDocs = nested.rootDocs(context);
        final DocIdSetIterator innerDocs = nested.innerDocs(context);
        final int maxChildren = nested.getNestedSort() != null ? nested.getNestedSort().getMaxChildren() : Integer.MAX_VALUE;
        return sortMode.select(values, missingValue, rootDocs, innerDocs, context.reader().maxDoc(), maxChildren);
    }

    @Override
    public FieldComparator<?> newComparator(String fieldname, int numHits, boolean enableSkipping, boolean reversed) {
        assert indexFieldData == null || fieldname.equals(indexFieldData.getFieldName());

        final int iMissingValue = (Integer) missingObject(missingValue, reversed);
        // NOTE: it's important to pass null as a missing value in the constructor so that
        // the comparator doesn't check docsWithField since we replace missing values in select()
        return new IntComparator(numHits, null, null, reversed, false) {
            @Override
            public LeafFieldComparator getLeafComparator(LeafReaderContext context) throws IOException {
                return new IntLeafComparator(context) {
                    @Override
                    protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
                        return IntValuesComparatorSource.this.getNumericDocValues(context, iMissingValue);
                    }
                };
            }
        };
    }

    @Override
    public BucketedSort newBucketedSort(
        BigArrays bigArrays,
        SortOrder sortOrder,
        DocValueFormat format,
        int bucketSize,
        BucketedSort.ExtraData extra
    ) {
        return new BucketedSort.ForInts(bigArrays, sortOrder, format, bucketSize, extra) {
            private final int iMissingValue = (Integer) missingObject(missingValue, sortOrder == SortOrder.DESC);

            @Override
            public Leaf forLeaf(LeafReaderContext ctx) throws IOException {
                return new Leaf(ctx) {
                    private final NumericDocValues docValues = getNumericDocValues(ctx, iMissingValue);
                    private int docValue;

                    @Override
                    protected boolean advanceExact(int doc) throws IOException {
                        if (docValues.advanceExact(doc)) {
                            docValue = (int) docValues.longValue();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected int docValue() {
                        return docValue;
                    }
                };
            }
        };
    }
}
