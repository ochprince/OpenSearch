/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.pipeline.common;

import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.SearchPipelinePlugin;
import com.colasoft.opensearch.search.pipeline.Processor;

import java.util.Map;

/**
 * Plugin providing common search request/response processors for use in search pipelines.
 */
public class SearchPipelineCommonModulePlugin extends Plugin implements SearchPipelinePlugin {

    /**
     * No constructor needed, but build complains if we don't have a constructor with JavaDoc.
     */
    public SearchPipelineCommonModulePlugin() {}

    @Override
    public Map<String, Processor.Factory> getProcessors(Processor.Parameters parameters) {
        return Map.of(FilterQueryRequestProcessor.TYPE, new FilterQueryRequestProcessor.Factory(parameters.namedXContentRegistry));
    }
}
