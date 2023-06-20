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

package com.colasoft.opensearch.painless;

import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.ActionResponse;
import com.colasoft.opensearch.client.Client;
import com.colasoft.opensearch.cluster.metadata.IndexNameExpressionResolver;
import com.colasoft.opensearch.cluster.node.DiscoveryNodes;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.SetOnce;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.IndexScopedSettings;
import com.colasoft.opensearch.common.settings.Setting;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.settings.SettingsFilter;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.env.NodeEnvironment;
import com.colasoft.opensearch.painless.action.PainlessContextAction;
import com.colasoft.opensearch.painless.action.PainlessExecuteAction;
import com.colasoft.opensearch.painless.spi.PainlessExtension;
import com.colasoft.opensearch.painless.spi.Whitelist;
import com.colasoft.opensearch.painless.spi.WhitelistLoader;
import com.colasoft.opensearch.plugins.ActionPlugin;
import com.colasoft.opensearch.plugins.ExtensiblePlugin;
import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.ScriptPlugin;
import com.colasoft.opensearch.repositories.RepositoriesService;
import com.colasoft.opensearch.rest.RestController;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.script.IngestScript;
import com.colasoft.opensearch.script.ScoreScript;
import com.colasoft.opensearch.script.ScriptContext;
import com.colasoft.opensearch.script.ScriptEngine;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.search.aggregations.pipeline.MovingFunctionScript;
import com.colasoft.opensearch.threadpool.ThreadPool;
import com.colasoft.opensearch.watcher.ResourceWatcherService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers Painless as a plugin.
 */
public final class PainlessPlugin extends Plugin implements ScriptPlugin, ExtensiblePlugin, ActionPlugin {

    private static final Map<ScriptContext<?>, List<Whitelist>> allowlists;

    /*
     * Contexts from Core that need custom allowlists can add them to the map below.
     * Allowlist resources should be added as appropriately named, separate files
     * under Painless' resources
     */
    static {
        Map<ScriptContext<?>, List<Whitelist>> map = new HashMap<>();

        // Moving Function Pipeline Agg
        List<Whitelist> movFn = new ArrayList<>(Whitelist.BASE_WHITELISTS);
        movFn.add(WhitelistLoader.loadFromResourceFiles(Whitelist.class, "com.colasoft.opensearch.aggs.movfn.txt"));
        map.put(MovingFunctionScript.CONTEXT, movFn);

        // Functions used for scoring docs
        List<Whitelist> scoreFn = new ArrayList<>(Whitelist.BASE_WHITELISTS);
        scoreFn.add(WhitelistLoader.loadFromResourceFiles(Whitelist.class, "com.colasoft.opensearch.score.txt"));
        map.put(ScoreScript.CONTEXT, scoreFn);

        // Functions available to ingest pipelines
        List<Whitelist> ingest = new ArrayList<>(Whitelist.BASE_WHITELISTS);
        ingest.add(WhitelistLoader.loadFromResourceFiles(Whitelist.class, "com.colasoft.opensearch.ingest.txt"));
        map.put(IngestScript.CONTEXT, ingest);

        allowlists = map;
    }

    private final SetOnce<PainlessScriptEngine> painlessScriptEngine = new SetOnce<>();

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> contexts) {
        Map<ScriptContext<?>, List<Whitelist>> contextsWithAllowlists = new HashMap<>();
        for (ScriptContext<?> context : contexts) {
            // we might have a context that only uses the base allowlists, so would not have been filled in by reloadSPI
            List<Whitelist> contextAllowlists = allowlists.get(context);
            if (contextAllowlists == null) {
                contextAllowlists = new ArrayList<>(Whitelist.BASE_WHITELISTS);
            }
            contextsWithAllowlists.put(context, contextAllowlists);
        }
        painlessScriptEngine.set(new PainlessScriptEngine(settings, contextsWithAllowlists));
        return painlessScriptEngine.get();
    }

    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver expressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        // this is a hack to bind the painless script engine in guice (all components are added to guice), so that
        // the painless context api. this is a temporary measure until transport actions do no require guice
        return Collections.singletonList(painlessScriptEngine.get());
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(CompilerSettings.REGEX_ENABLED, CompilerSettings.REGEX_LIMIT_FACTOR);
    }

    @Override
    public void loadExtensions(ExtensionLoader loader) {
        loader.loadExtensions(PainlessExtension.class)
            .stream()
            .flatMap(extension -> extension.getContextWhitelists().entrySet().stream())
            .forEach(entry -> {
                List<Whitelist> existing = allowlists.computeIfAbsent(entry.getKey(), c -> new ArrayList<>(Whitelist.BASE_WHITELISTS));
                existing.addAll(entry.getValue());
            });
    }

    @Override
    public List<ScriptContext<?>> getContexts() {
        return Collections.singletonList(PainlessExecuteAction.PainlessTestScript.CONTEXT);
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> actions = new ArrayList<>();
        actions.add(new ActionHandler<>(PainlessExecuteAction.INSTANCE, PainlessExecuteAction.TransportAction.class));
        actions.add(new ActionHandler<>(PainlessContextAction.INSTANCE, PainlessContextAction.TransportAction.class));
        return actions;
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        List<RestHandler> handlers = new ArrayList<>();
        handlers.add(new PainlessExecuteAction.RestAction());
        handlers.add(new PainlessContextAction.RestAction());
        return handlers;
    }
}
