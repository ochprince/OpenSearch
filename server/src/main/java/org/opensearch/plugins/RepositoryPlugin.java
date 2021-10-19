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

package org.opensearch.plugins;

import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.indices.recovery.RecoverySettings;
import org.opensearch.repositories.Repository;

import java.util.Collections;
import java.util.Map;

/**
 * An extension point for {@link Plugin} implementations to add custom snapshot repositories.
 */
public interface RepositoryPlugin {

    /**
     * Returns repository types added by this plugin.
     *
     * @param env The environment for the local node, which may be used for the local settings and path.repo
     *
     * The key of the returned {@link Map} is the type name of the repository and
     * the value is a factory to construct the {@link Repository} interface.
     */
    default Map<String, Repository.Factory> getRepositories(
        Environment env,
        NamedXContentRegistry namedXContentRegistry,
        ClusterService clusterService,
        RecoverySettings recoverySettings
    ) {
        return Collections.emptyMap();
    }

    /**
     * Returns internal repository types added by this plugin. Internal repositories cannot be registered
     * through the external API.
     *
     * @param env The environment for the local node, which may be used for the local settings and path.repo
     *
     * The key of the returned {@link Map} is the type name of the repository and
     * the value is a factory to construct the {@link Repository} interface.
     */
    default Map<String, Repository.Factory> getInternalRepositories(
        Environment env,
        NamedXContentRegistry namedXContentRegistry,
        ClusterService clusterService,
        RecoverySettings recoverySettings
    ) {
        return Collections.emptyMap();
    }

}
