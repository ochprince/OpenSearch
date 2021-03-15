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

package org.opensearch.action.admin.cluster.settings;

import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.action.support.master.MasterNodeReadRequest;
import org.opensearch.action.admin.cluster.state.ClusterStateRequest;

/**
 * This request is specific to the REST client. {@link ClusterStateRequest}
 * is used on the transport layer.
 */
public class ClusterGetSettingsRequest extends MasterNodeReadRequest<ClusterGetSettingsRequest> {
    private boolean includeDefaults = false;

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    /**
     * When include_defaults is set, return default settings which are normally suppressed.
     */
    public ClusterGetSettingsRequest includeDefaults(boolean includeDefaults) {
        this.includeDefaults = includeDefaults;
        return this;
    }

    public boolean includeDefaults() {
        return includeDefaults;
    }
}
