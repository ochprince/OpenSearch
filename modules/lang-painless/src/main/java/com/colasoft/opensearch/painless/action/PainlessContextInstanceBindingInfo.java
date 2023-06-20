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

package com.colasoft.opensearch.painless.action;

import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.core.xcontent.ConstructingObjectParser;
import com.colasoft.opensearch.core.xcontent.ToXContent;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.painless.lookup.PainlessInstanceBinding;
import com.colasoft.opensearch.painless.lookup.PainlessLookupUtility;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PainlessContextInstanceBindingInfo implements Writeable, ToXContentObject {

    public static final ParseField DECLARING = new ParseField("declaring");
    public static final ParseField NAME = new ParseField("name");
    public static final ParseField RTN = new ParseField("return");
    public static final ParseField PARAMETERS = new ParseField("parameters");

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<PainlessContextInstanceBindingInfo, Void> PARSER = new ConstructingObjectParser<>(
        PainlessContextInstanceBindingInfo.class.getCanonicalName(),
        (v) -> new PainlessContextInstanceBindingInfo((String) v[0], (String) v[1], (String) v[2], (List<String>) v[3])
    );

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), DECLARING);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), NAME);
        PARSER.declareString(ConstructingObjectParser.constructorArg(), RTN);
        PARSER.declareStringArray(ConstructingObjectParser.constructorArg(), PARAMETERS);
    }

    private final String declaring;
    private final String name;
    private final String rtn;
    private final List<String> parameters;

    public PainlessContextInstanceBindingInfo(PainlessInstanceBinding painlessInstanceBinding) {
        this(
            painlessInstanceBinding.javaMethod.getDeclaringClass().getName(),
            painlessInstanceBinding.javaMethod.getName(),
            painlessInstanceBinding.returnType.getName(),
            painlessInstanceBinding.typeParameters.stream().map(Class::getName).collect(Collectors.toList())

        );
    }

    public PainlessContextInstanceBindingInfo(String declaring, String name, String rtn, List<String> parameters) {
        this.declaring = Objects.requireNonNull(declaring);
        this.name = Objects.requireNonNull(name);
        this.rtn = Objects.requireNonNull(rtn);
        this.parameters = Collections.unmodifiableList(Objects.requireNonNull(parameters));
    }

    public PainlessContextInstanceBindingInfo(StreamInput in) throws IOException {
        declaring = in.readString();
        name = in.readString();
        rtn = in.readString();
        parameters = Collections.unmodifiableList(in.readStringList());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(declaring);
        out.writeString(name);
        out.writeString(rtn);
        out.writeStringCollection(parameters);
    }

    public static PainlessContextInstanceBindingInfo fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.field(DECLARING.getPreferredName(), declaring);
        builder.field(NAME.getPreferredName(), name);
        builder.field(RTN.getPreferredName(), rtn);
        builder.field(PARAMETERS.getPreferredName(), parameters);
        builder.endObject();

        return builder;
    }

    public String getSortValue() {
        return PainlessLookupUtility.buildPainlessMethodKey(name, parameters.size());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PainlessContextInstanceBindingInfo that = (PainlessContextInstanceBindingInfo) o;
        return Objects.equals(declaring, that.declaring)
            && Objects.equals(name, that.name)
            && Objects.equals(rtn, that.rtn)
            && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaring, name, rtn, parameters);
    }

    @Override
    public String toString() {
        return "PainlessContextInstanceBindingInfo{"
            + "declaring='"
            + declaring
            + '\''
            + ", name='"
            + name
            + '\''
            + ", rtn='"
            + rtn
            + '\''
            + ", parameters="
            + parameters
            + '}';
    }

    public String getDeclaring() {
        return declaring;
    }

    public String getName() {
        return name;
    }

    public String getRtn() {
        return rtn;
    }

    public List<String> getParameters() {
        return parameters;
    }
}
