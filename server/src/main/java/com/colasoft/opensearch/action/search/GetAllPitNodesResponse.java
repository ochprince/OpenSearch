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

import com.colasoft.opensearch.action.FailedNodeException;
import com.colasoft.opensearch.action.support.nodes.BaseNodesResponse;
import com.colasoft.opensearch.cluster.ClusterName;
import com.colasoft.opensearch.core.ParseField;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.core.xcontent.ConstructingObjectParser;
import com.colasoft.opensearch.core.xcontent.ToXContentObject;
import com.colasoft.opensearch.core.xcontent.XContentBuilder;
import com.colasoft.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.colasoft.opensearch.core.xcontent.ConstructingObjectParser.constructorArg;

/**
 * This class transforms active PIT objects from all nodes to unique PIT objects
 */
public class GetAllPitNodesResponse extends BaseNodesResponse<GetAllPitNodeResponse> implements ToXContentObject {

    /**
     * List of unique PITs across all nodes
     */
    private final Set<ListPitInfo> pitInfos = new HashSet<>();

    public GetAllPitNodesResponse(StreamInput in) throws IOException {
        super(in);
    }

    public GetAllPitNodesResponse(
        ClusterName clusterName,
        List<GetAllPitNodeResponse> getAllPitNodeResponseList,
        List<FailedNodeException> failures
    ) {
        super(clusterName, getAllPitNodeResponseList, failures);
        Set<String> uniquePitIds = new HashSet<>();
        pitInfos.addAll(
            getAllPitNodeResponseList.stream()
                .flatMap(p -> p.getPitInfos().stream().filter(t -> uniquePitIds.add(t.getPitId())))
                .collect(Collectors.toList())
        );
    }

    /**
     * Copy constructor that explicitly sets the list pit infos
     */
    public GetAllPitNodesResponse(List<ListPitInfo> listPitInfos, GetAllPitNodesResponse response) {
        super(response.getClusterName(), response.getNodes(), response.failures());
        pitInfos.addAll(listPitInfos);
    }

    public GetAllPitNodesResponse(
        List<ListPitInfo> listPitInfos,
        ClusterName clusterName,
        List<GetAllPitNodeResponse> getAllPitNodeResponseList,
        List<FailedNodeException> failures
    ) {
        super(clusterName, getAllPitNodeResponseList, failures);
        pitInfos.addAll(listPitInfos);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.startArray("pits");
        for (ListPitInfo pit : pitInfos) {
            pit.toXContent(builder, params);
        }
        builder.endArray();
        if (!failures().isEmpty()) {
            builder.startArray("failures");
            for (FailedNodeException e : failures()) {
                e.toXContent(builder, params);
            }
        }
        builder.endObject();
        return builder;
    }

    @Override
    public List<GetAllPitNodeResponse> readNodesFrom(StreamInput in) throws IOException {
        return in.readList(GetAllPitNodeResponse::new);
    }

    @Override
    public void writeNodesTo(StreamOutput out, List<GetAllPitNodeResponse> nodes) throws IOException {
        out.writeList(nodes);
    }

    public List<ListPitInfo> getPitInfos() {
        return Collections.unmodifiableList(new ArrayList<>(pitInfos));
    }

    private static final ConstructingObjectParser<GetAllPitNodesResponse, Void> PARSER = new ConstructingObjectParser<>(
        "get_all_pits_response",
        true,
        (Object[] parsedObjects) -> {
            @SuppressWarnings("unchecked")
            List<ListPitInfo> listPitInfos = (List<ListPitInfo>) parsedObjects[0];
            List<FailedNodeException> failures = null;
            if (parsedObjects.length > 1) {
                failures = (List<FailedNodeException>) parsedObjects[1];
            }
            if (failures == null) {
                failures = new ArrayList<>();
            }
            return new GetAllPitNodesResponse(listPitInfos, new ClusterName(""), new ArrayList<>(), failures);
        }
    );
    static {
        PARSER.declareObjectArray(constructorArg(), ListPitInfo.PARSER, new ParseField("pits"));
    }

    public static GetAllPitNodesResponse fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }
}
