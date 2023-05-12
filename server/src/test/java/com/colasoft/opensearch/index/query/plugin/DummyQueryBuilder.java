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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package com.colasoft.opensearch.index.query.plugin;

import org.apache.lucene.search.Query;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.index.query.AbstractQueryBuilder;
import com.colasoft.opensearch.index.query.QueryShardContext;
import com.colasoft.opensearch.index.query.plugin.DummyQueryParserPlugin.DummyQuery;

import java.io.IOException;

public class DummyQueryBuilder extends AbstractQueryBuilder<DummyQueryBuilder> {
    public static final String NAME = "dummy";

    public DummyQueryBuilder() {}

    public DummyQueryBuilder(StreamInput in) throws IOException {
        super(in);
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        // only the superclass has state
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME).endObject();
    }

    public static DummyQueryBuilder fromXContent(XContentParser parser) throws IOException {
        XContentParser.Token token = parser.nextToken();
        assert token == XContentParser.Token.END_OBJECT;
        return new DummyQueryBuilder();
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        return new DummyQuery();
    }

    @Override
    protected int doHashCode() {
        return 0;
    }

    @Override
    protected boolean doEquals(DummyQueryBuilder other) {
        return true;
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}
