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

package com.colasoft.opensearch.search.sort.plugin;

import static com.colasoft.opensearch.core.xcontent.ConstructingObjectParser.constructorArg;

import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ConstructingObjectParser;
import com.colasoft.opensearch.core.xcontent.ObjectParser;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.QueryRewriteContext;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.search.sort.BucketedSort;
import com.colasoft.opensearch.search.sort.SortBuilder;
import com.colasoft.opensearch.search.sort.SortBuilders;
import com.colasoft.opensearch.search.sort.SortFieldAndFormat;
import com.colasoft.opensearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.Objects;

/**
 * Custom sort builder that just rewrites to a basic field sort
 */
public class CustomSortBuilder extends SortBuilder<CustomSortBuilder> {
    public static String NAME = "_custom";
    public static ParseField SORT_FIELD = new ParseField("sort_field");

    public final String field;
    public final SortOrder order;

    public CustomSortBuilder(String field, SortOrder order) {
        this.field = field;
        this.order = order;
    }

    public CustomSortBuilder(StreamInput in) throws IOException {
        this.field = in.readString();
        this.order = in.readOptionalWriteable(SortOrder::readFromStream);
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(field);
        out.writeOptionalWriteable(order);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public SortBuilder<?> rewrite(final QueryRewriteContext ctx) throws IOException {
        return SortBuilders.fieldSort(field).order(order);
    }

    @Override
    protected SortFieldAndFormat build(final QueryShardContext context) throws IOException {
        throw new IllegalStateException("rewrite");
    }

    @Override
    public BucketedSort buildBucketedSort(final QueryShardContext context, final int bucketSize, final BucketedSort.ExtraData extra)
        throws IOException {
        throw new IllegalStateException("rewrite");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        CustomSortBuilder other = (CustomSortBuilder) object;
        return Objects.equals(field, other.field) && Objects.equals(order, other.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, order);
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        builder.startObject();
        builder.startObject(NAME);
        builder.field(SORT_FIELD.getPreferredName(), field);
        builder.field(ORDER_FIELD.getPreferredName(), order);
        builder.endObject();
        builder.endObject();
        return builder;
    }

    public static CustomSortBuilder fromXContent(XContentParser parser, String elementName) {
        return PARSER.apply(parser, null);
    }

    private static final ConstructingObjectParser<CustomSortBuilder, Void> PARSER = new ConstructingObjectParser<>(
        NAME,
        a -> new CustomSortBuilder((String) a[0], (SortOrder) a[1])
    );

    static {
        PARSER.declareField(constructorArg(), XContentParser::text, SORT_FIELD, ObjectParser.ValueType.STRING);
        PARSER.declareField(constructorArg(), p -> SortOrder.fromString(p.text()), ORDER_FIELD, ObjectParser.ValueType.STRING);
    }
}
