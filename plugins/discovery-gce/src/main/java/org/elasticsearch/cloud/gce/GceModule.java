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

package org.elasticsearch.cloud.gce;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class GceModule extends AbstractModule {
    // pkg private so tests can override with mock
    static Class<? extends GceComputeService> computeServiceImpl = GceComputeServiceImpl.class;
    static Class<? extends GceMetadataService> metadataServiceImpl = GceMetadataServiceImpl.class;

    protected final Settings settings;
    protected final ESLogger logger = Loggers.getLogger(GceModule.class);

    public GceModule(Settings settings) {
        this.settings = settings;
    }

    public static Class<? extends GceComputeService> getComputeServiceImpl() {
        return computeServiceImpl;
    }

    public static Class<? extends GceMetadataService> getMetadataServiceImpl() {
        return metadataServiceImpl;
    }

    @Override
    protected void configure() {
        logger.debug("configure GceModule (bind compute and metadata services)");
        bind(GceComputeService.class).to(computeServiceImpl).asEagerSingleton();
        bind(GceMetadataService.class).to(metadataServiceImpl).asEagerSingleton();
    }
}
