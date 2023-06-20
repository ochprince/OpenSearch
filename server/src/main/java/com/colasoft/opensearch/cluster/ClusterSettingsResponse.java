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

package com.colasoft.opensearch.cluster;

import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.common.io.stream.StreamOutput;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.transport.TransportResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * PluginSettings Response for Extensibility
 *
 * @opensearch.internal
 */
public class ClusterSettingsResponse extends TransportResponse {
    private final Settings clusterSettings;

    public ClusterSettingsResponse(ClusterService clusterService) {
        this.clusterSettings = clusterService.getSettings();
    }

    public ClusterSettingsResponse(StreamInput in) throws IOException {
        super(in);
        this.clusterSettings = Settings.readSettingsFromStream(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        Settings.writeSettingsToStream(clusterSettings, out);
    }

    @Override
    public String toString() {
        return "ClusterSettingsResponse{" + "clusterSettings=" + clusterSettings + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterSettingsResponse that = (ClusterSettingsResponse) o;
        return Objects.equals(clusterSettings, that.clusterSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterSettings);
    }

}
