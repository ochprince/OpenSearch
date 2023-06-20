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

package com.colasoft.opensearch.cluster.action.index;

import com.colasoft.opensearch.LegacyESVersion;
import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.admin.indices.mapping.put.AutoPutMappingAction;
import com.colasoft.opensearch.action.admin.indices.mapping.put.PutMappingRequest;
import com.colasoft.opensearch.action.support.clustermanager.ClusterManagerNodeRequest;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.client.IndicesAdminClient;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Setting.Property;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.TimeValue;
import com.colasoft.opensearch.common.util.concurrent.RunOnce;
import com.colasoft.opensearch.common.util.concurrent.UncategorizedExecutionException;
import com.colasoft.opensearch.common.xcontent.XContentType;
import com.colasoft.opensearch.index.Index;
import com.colasoft.opensearch.index.mapper.Mapping;

import java.util.concurrent.Semaphore;

/**
 * Called by shards in the cluster when their mapping was dynamically updated and it needs to be updated
 * in the cluster state meta data (and broadcast to all members).
 *
 * @opensearch.internal
 */
public class MappingUpdatedAction {

    public static final Setting<TimeValue> INDICES_MAPPING_DYNAMIC_TIMEOUT_SETTING = Setting.positiveTimeSetting(
        "indices.mapping.dynamic_timeout",
        TimeValue.timeValueSeconds(30),
        Property.Dynamic,
        Property.NodeScope
    );

    public static final Setting<Integer> INDICES_MAX_IN_FLIGHT_UPDATES_SETTING = Setting.intSetting(
        "indices.mapping.max_in_flight_updates",
        10,
        1,
        1000,
        Property.Dynamic,
        Property.NodeScope
    );

    private IndicesAdminClient client;
    private volatile TimeValue dynamicMappingUpdateTimeout;
    private final AdjustableSemaphore semaphore;
    private final ClusterService clusterService;

    @Inject
    public MappingUpdatedAction(Settings settings, ClusterSettings clusterSettings, ClusterService clusterService) {
        this.dynamicMappingUpdateTimeout = INDICES_MAPPING_DYNAMIC_TIMEOUT_SETTING.get(settings);
        this.semaphore = new AdjustableSemaphore(INDICES_MAX_IN_FLIGHT_UPDATES_SETTING.get(settings), true);
        this.clusterService = clusterService;
        clusterSettings.addSettingsUpdateConsumer(INDICES_MAPPING_DYNAMIC_TIMEOUT_SETTING, this::setDynamicMappingUpdateTimeout);
        clusterSettings.addSettingsUpdateConsumer(INDICES_MAX_IN_FLIGHT_UPDATES_SETTING, this::setMaxInFlightUpdates);
    }

    private void setDynamicMappingUpdateTimeout(TimeValue dynamicMappingUpdateTimeout) {
        this.dynamicMappingUpdateTimeout = dynamicMappingUpdateTimeout;
    }

    private void setMaxInFlightUpdates(int maxInFlightUpdates) {
        semaphore.setMaxPermits(maxInFlightUpdates);
    }

    public void setClient(Client client) {
        this.client = client.admin().indices();
    }

    /**
     * Update mappings on the cluster-manager node, waiting for the change to be committed,
     * but not for the mapping update to be applied on all nodes. The timeout specified by
     * {@code timeout} is the cluster-manager node timeout ({@link ClusterManagerNodeRequest#clusterManagerNodeTimeout()}),
     * potentially waiting for a cluster-manager node to be available.
     */
    public void updateMappingOnClusterManager(Index index, Mapping mappingUpdate, ActionListener<Void> listener) {

        final RunOnce release = new RunOnce(() -> semaphore.release());
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            listener.onFailure(e);
            return;
        }
        boolean successFullySent = false;
        try {
            sendUpdateMapping(index, mappingUpdate, ActionListener.runBefore(listener, release::run));
            successFullySent = true;
        } finally {
            if (successFullySent == false) {
                release.run();
            }
        }
    }

    /**
     * Update mappings on the cluster-manager node, waiting for the change to be committed,
     * but not for the mapping update to be applied on all nodes. The timeout specified by
     * {@code timeout} is the cluster-manager node timeout ({@link ClusterManagerNodeRequest#clusterManagerNodeTimeout()}),
     * potentially waiting for a cluster-manager node to be available.
     *
     * @deprecated As of 2.2, because supporting inclusive language, replaced by {@link #updateMappingOnClusterManager(Index, Mapping, ActionListener)}
     */
    @Deprecated
    public void updateMappingOnMaster(Index index, Mapping mappingUpdate, ActionListener<Void> listener) {
        updateMappingOnClusterManager(index, mappingUpdate, listener);
    }

    // used by tests
    int blockedThreads() {
        return semaphore.getQueueLength();
    }

    // can be overridden by tests
    protected void sendUpdateMapping(Index index, Mapping mappingUpdate, ActionListener<Void> listener) {
        PutMappingRequest putMappingRequest = new PutMappingRequest();
        putMappingRequest.setConcreteIndex(index);
        putMappingRequest.source(mappingUpdate.toString(), XContentType.JSON);
        putMappingRequest.clusterManagerNodeTimeout(dynamicMappingUpdateTimeout);
        putMappingRequest.timeout(TimeValue.ZERO);
        if (clusterService.state().nodes().getMinNodeVersion().onOrAfter(LegacyESVersion.V_7_9_0)) {
            client.execute(
                AutoPutMappingAction.INSTANCE,
                putMappingRequest,
                ActionListener.wrap(r -> listener.onResponse(null), listener::onFailure)
            );
        } else {
            client.putMapping(
                putMappingRequest,
                ActionListener.wrap(r -> listener.onResponse(null), e -> listener.onFailure(unwrapException(e)))
            );
        }
    }

    // todo: this explicit unwrap should not be necessary, but is until guessRootCause is fixed to allow wrapped non-es exception.
    private static Exception unwrapException(Exception cause) {
        return cause instanceof OpenSearchException ? unwrapEsException((OpenSearchException) cause) : cause;
    }

    private static RuntimeException unwrapEsException(OpenSearchException esEx) {
        Throwable root = esEx.unwrapCause();
        if (root instanceof RuntimeException) {
            return (RuntimeException) root;
        }
        return new UncategorizedExecutionException("Failed execution", root);
    }

    /**
     * An adjustable semaphore
     *
     * @opensearch.internal
     */
    static class AdjustableSemaphore extends Semaphore {

        private final Object maxPermitsMutex = new Object();
        private int maxPermits;

        AdjustableSemaphore(int maxPermits, boolean fair) {
            super(maxPermits, fair);
            this.maxPermits = maxPermits;
        }

        void setMaxPermits(int permits) {
            synchronized (maxPermitsMutex) {
                final int diff = Math.subtractExact(permits, maxPermits);
                if (diff > 0) {
                    // add permits
                    release(diff);
                } else if (diff < 0) {
                    // remove permits
                    reducePermits(Math.negateExact(diff));
                }

                maxPermits = permits;
            }
        }
    }
}
