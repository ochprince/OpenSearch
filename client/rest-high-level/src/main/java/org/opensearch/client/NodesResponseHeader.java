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

package org.opensearch.client;

import org.opensearch.OpenSearchException;
import org.opensearch.action.support.nodes.BaseNodesResponse;
import org.opensearch.common.Nullable;
import org.opensearch.common.ParseField;
import org.opensearch.common.xcontent.ConstructingObjectParser;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.rest.action.RestActions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A utility class to parse the Nodes Header returned by
 * {@link RestActions#buildNodesHeader(XContentBuilder, ToXContent.Params, BaseNodesResponse)}.
 */
public final class NodesResponseHeader {

    public static final ParseField TOTAL = new ParseField("total");
    public static final ParseField SUCCESSFUL = new ParseField("successful");
    public static final ParseField FAILED = new ParseField("failed");
    public static final ParseField FAILURES = new ParseField("failures");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<NodesResponseHeader, Void> PARSER = new ConstructingObjectParser<>(
        "nodes_response_header",
        true,
        (a) -> {
            int i = 0;
            int total = (Integer) a[i++];
            int successful = (Integer) a[i++];
            int failed = (Integer) a[i++];
            List<OpenSearchException> failures = (List<OpenSearchException>) a[i++];
            return new NodesResponseHeader(total, successful, failed, failures);
        }
    );

    static {
        PARSER.declareInt(ConstructingObjectParser.constructorArg(), TOTAL);
        PARSER.declareInt(ConstructingObjectParser.constructorArg(), SUCCESSFUL);
        PARSER.declareInt(ConstructingObjectParser.constructorArg(), FAILED);
        PARSER.declareObjectArray(
            ConstructingObjectParser.optionalConstructorArg(),
            (p, c) -> OpenSearchException.fromXContent(p),
            FAILURES
        );
    }

    private final int total;
    private final int successful;
    private final int failed;
    private final List<OpenSearchException> failures;

    public NodesResponseHeader(int total, int successful, int failed, @Nullable List<OpenSearchException> failures) {
        this.total = total;
        this.successful = successful;
        this.failed = failed;
        this.failures = failures == null ? Collections.emptyList() : failures;
    }

    public static NodesResponseHeader fromXContent(XContentParser parser, Void context) throws IOException {
        return PARSER.parse(parser, context);
    }

    /** the total number of nodes that the operation was carried on */
    public int getTotal() {
        return total;
    }

    /** the number of nodes that the operation has failed on */
    public int getFailed() {
        return failed;
    }

    /** the number of nodes that the operation was successful on */
    public int getSuccessful() {
        return successful;
    }

    /**
     * Get the failed node exceptions.
     *
     * @return Never {@code null}. Can be empty.
     */
    public List<OpenSearchException> getFailures() {
        return failures;
    }

    /**
     * Determine if there are any node failures in {@link #failures}.
     *
     * @return {@code true} if {@link #failures} contains at least 1 exception.
     */
    public boolean hasFailures() {
        return failures.isEmpty() == false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodesResponseHeader that = (NodesResponseHeader) o;
        return total == that.total && successful == that.successful && failed == that.failed && Objects.equals(failures, that.failures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, successful, failed, failures);
    }

}
