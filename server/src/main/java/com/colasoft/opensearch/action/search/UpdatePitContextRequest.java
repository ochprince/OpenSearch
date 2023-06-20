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

package com.colasoft.opensearch.action.search;

import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.search.internal.ShardSearchContextId;
import com.colasoft.opensearch.transport.TransportRequest;

import java.io.IOException;

/**
 * Request used to update PIT reader contexts with pitId, keepAlive and creationTime
 */
public class UpdatePitContextRequest extends TransportRequest {
    private final String pitId;
    private final long keepAlive;

    private final long creationTime;
    private final ShardSearchContextId searchContextId;

    public UpdatePitContextRequest(ShardSearchContextId searchContextId, String pitId, long keepAlive, long creationTime) {
        this.pitId = pitId;
        this.searchContextId = searchContextId;
        this.keepAlive = keepAlive;
        this.creationTime = creationTime;
    }

    UpdatePitContextRequest(StreamInput in) throws IOException {
        super(in);
        pitId = in.readString();
        keepAlive = in.readLong();
        creationTime = in.readLong();
        searchContextId = new ShardSearchContextId(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(pitId);
        out.writeLong(keepAlive);
        out.writeLong(creationTime);
        searchContextId.writeTo(out);
    }

    public ShardSearchContextId getSearchContextId() {
        return searchContextId;
    }

    public String getPitId() {
        return pitId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getKeepAlive() {
        return keepAlive;
    }
}
