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

package org.elasticsearch.ingest;

import java.util.Map;

/**
 * A processor implementation may modify the data belonging to a document.
 * Whether changes are made and what exactly is modified is up to the implementation.
 */
public abstract class AbstractProcessorFactory<P extends Processor> implements Processor.Factory<P> {
    public static final String TAG_KEY = "tag";

    @Override
    public P create(ProcessorsRegistry registry, Map<String, Object> config) throws Exception {
        String tag = ConfigurationUtils.readOptionalStringProperty(null, null, config, TAG_KEY);
        return doCreate(registry, tag, config);
    }

    protected abstract P doCreate(ProcessorsRegistry registry, String tag, Map<String, Object> config) throws Exception;
}
