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

package com.colasoft.opensearch.gradle.test.rest;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;

import javax.inject.Inject;

/**
 * Custom extension to configure the {@link CopyRestApiTask}
 */
public class RestResourcesExtension {

    final RestResourcesSpec restApi;
    final RestResourcesSpec restTests;

    @Inject
    public RestResourcesExtension(ObjectFactory objects) {
        restApi = new RestResourcesSpec(objects);
        restTests = new RestResourcesSpec(objects);
    }

    void restApi(Action<? super RestResourcesSpec> spec) {
        spec.execute(restApi);
    }

    void restTests(Action<? super RestResourcesSpec> spec) {
        spec.execute(restTests);
    }

    static class RestResourcesSpec {

        private final ListProperty<String> includeCore;

        RestResourcesSpec(ObjectFactory objects) {
            includeCore = objects.listProperty(String.class);
        }

        public void includeCore(String... include) {
            this.includeCore.addAll(include);
        }

        public ListProperty<String> getIncludeCore() {
            return includeCore;
        }
    }
}
