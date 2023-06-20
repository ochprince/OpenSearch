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

package com.colasoft.opensearch.action.admin.cluster.remotestore.restore;

import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.common.Nullable;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ConstructingObjectParser;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.rest.RestStatus;
import com.colasoft.opensearch.snapshots.RestoreInfo;

import java.io.IOException;
import java.util.Objects;

import static com.colasoft.opensearch.core.xcontent.ConstructingObjectParser.optionalConstructorArg;

/**
 * Contains information about remote store restores
 *
 * @opensearch.internal
 */
public final class RestoreRemoteStoreResponse extends ActionResponse implements ToXContentObject {

    @Nullable
    private final RestoreInfo restoreInfo;

    public RestoreRemoteStoreResponse(@Nullable RestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }

    public RestoreRemoteStoreResponse(StreamInput in) throws IOException {
        super(in);
        restoreInfo = RestoreInfo.readOptionalRestoreInfo(in);
    }

    /**
     * Returns restore information if remote store restore was completed before this method returned, null otherwise
     *
     * @return restore information or null
     */
    public RestoreInfo getRestoreInfo() {
        return restoreInfo;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeOptionalWriteable(restoreInfo);
    }

    public RestStatus status() {
        if (restoreInfo == null) {
            return RestStatus.ACCEPTED;
        }
        return restoreInfo.status();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (restoreInfo != null) {
            builder.field("remote_store");
            restoreInfo.toXContent(builder, params);
        } else {
            builder.field("accepted", true);
        }
        builder.endObject();
        return builder;
    }

    public static final ConstructingObjectParser<RestoreRemoteStoreResponse, Void> PARSER = new ConstructingObjectParser<>(
        "restore_remote_store",
        true,
        v -> {
            RestoreInfo restoreInfo = (RestoreInfo) v[0];
            Boolean accepted = (Boolean) v[1];
            assert (accepted == null && restoreInfo != null) || (accepted != null && accepted && restoreInfo == null) : "accepted: ["
                + accepted
                + "], restoreInfo: ["
                + restoreInfo
                + "]";
            return new RestoreRemoteStoreResponse(restoreInfo);
        }
    );

    static {
        PARSER.declareObject(
            optionalConstructorArg(),
            (parser, context) -> RestoreInfo.fromXContent(parser),
            new ParseField("remote_store")
        );
        PARSER.declareBoolean(optionalConstructorArg(), new ParseField("accepted"));
    }

    public static RestoreRemoteStoreResponse fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestoreRemoteStoreResponse that = (RestoreRemoteStoreResponse) o;
        return Objects.equals(restoreInfo, that.restoreInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restoreInfo);
    }

    @Override
    public String toString() {
        return "RestoreRemoteStoreResponse{" + "restoreInfo=" + restoreInfo + '}';
    }
}
