/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.dataframe.evaluation.classification;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.Evaluation;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Evaluation of classification results.
 */
public class Classification implements Evaluation {

    public static final ParseField NAME = new ParseField("classification");

    private static final ParseField ACTUAL_FIELD = new ParseField("actual_field");
    private static final ParseField PREDICTED_FIELD = new ParseField("predicted_field");
    private static final ParseField METRICS = new ParseField("metrics");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<Classification, Void> PARSER = new ConstructingObjectParser<>(
        NAME.getPreferredName(), a -> new Classification((String) a[0], (String) a[1], (List<ClassificationMetric>) a[2]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), ACTUAL_FIELD);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), PREDICTED_FIELD);
        PARSER.declareNamedObjects(ConstructingObjectParser.optionalConstructorArg(),
            (p, c, n) -> p.namedObject(ClassificationMetric.class, n, c), METRICS);
    }

    public static Classification fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    /**
     * The field containing the actual value
     * The value of this field is assumed to be numeric
     */
    private final String actualField;

    /**
     * The field containing the predicted value
     * The value of this field is assumed to be numeric
     */
    private final String predictedField;

    /**
     * The list of metrics to calculate
     */
    private final List<ClassificationMetric> metrics;

    public Classification(String actualField, String predictedField, @Nullable List<ClassificationMetric> metrics) {
        this.actualField = ExceptionsHelper.requireNonNull(actualField, ACTUAL_FIELD);
        this.predictedField = ExceptionsHelper.requireNonNull(predictedField, PREDICTED_FIELD);
        this.metrics = initMetrics(metrics);
    }

    public Classification(StreamInput in) throws IOException {
        this.actualField = in.readString();
        this.predictedField = in.readString();
        this.metrics = in.readNamedWriteableList(ClassificationMetric.class);
    }

    private static List<ClassificationMetric> initMetrics(@Nullable List<ClassificationMetric> parsedMetrics) {
        List<ClassificationMetric> metrics = parsedMetrics == null ? defaultMetrics() : new ArrayList<>(parsedMetrics);
        if (metrics.isEmpty()) {
            throw ExceptionsHelper.badRequestException("[{}] must have one or more metrics", NAME.getPreferredName());
        }
        Collections.sort(metrics, Comparator.comparing(ClassificationMetric::getName));
        return metrics;
    }

    private static List<ClassificationMetric> defaultMetrics() {
        return Arrays.asList(new MulticlassConfusionMatrix());
    }

    @Override
    public String getName() {
        return NAME.getPreferredName();
    }

    @Override
    public List<ClassificationMetric> getMetrics() {
        return metrics;
    }

    @Override
    public SearchSourceBuilder buildSearch(QueryBuilder userProvidedQueryBuilder) {
        ExceptionsHelper.requireNonNull(userProvidedQueryBuilder, "userProvidedQueryBuilder");
        SearchSourceBuilder searchSourceBuilder =
            newSearchSourceBuilder(Arrays.asList(actualField, predictedField), userProvidedQueryBuilder);
        for (ClassificationMetric metric : metrics) {
            List<AggregationBuilder> aggs = metric.aggs(actualField, predictedField);
            aggs.forEach(searchSourceBuilder::aggregation);
        }
        return searchSourceBuilder;
    }

    @Override
    public void process(SearchResponse searchResponse) {
        ExceptionsHelper.requireNonNull(searchResponse, "searchResponse");
        if (searchResponse.getHits().getTotalHits().value == 0) {
            throw ExceptionsHelper.badRequestException("No documents found containing both [{}, {}] fields", actualField, predictedField);
        }
        for (ClassificationMetric metric : metrics) {
            metric.process(searchResponse.getAggregations());
        }
    }

    @Override
    public String getWriteableName() {
        return NAME.getPreferredName();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(actualField);
        out.writeString(predictedField);
        out.writeNamedWriteableList(metrics);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ACTUAL_FIELD.getPreferredName(), actualField);
        builder.field(PREDICTED_FIELD.getPreferredName(), predictedField);

        builder.startObject(METRICS.getPreferredName());
        for (ClassificationMetric metric : metrics) {
            builder.field(metric.getWriteableName(), metric);
        }
        builder.endObject();

        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Classification that = (Classification) o;
        return Objects.equals(that.actualField, this.actualField)
            && Objects.equals(that.predictedField, this.predictedField)
            && Objects.equals(that.metrics, this.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualField, predictedField, metrics);
    }
}
