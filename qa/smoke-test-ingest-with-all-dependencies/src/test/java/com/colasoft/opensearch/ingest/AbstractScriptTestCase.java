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

package com.colasoft.opensearch.ingest;

import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.script.Script;
import com.colasoft.opensearch.script.ScriptEngine;
import com.colasoft.opensearch.script.ScriptModule;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.script.ScriptType;
import com.colasoft.opensearch.script.TemplateScript;
import com.colasoft.opensearch.script.mustache.MustacheScriptEngine;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import org.junit.Before;

import java.util.Collections;
import java.util.Map;

import static com.colasoft.opensearch.script.Script.DEFAULT_TEMPLATE_LANG;

public abstract class AbstractScriptTestCase extends OpenSearchTestCase {

    protected ScriptService scriptService;

    @Before
    public void init() throws Exception {
        MustacheScriptEngine engine = new MustacheScriptEngine();
        Map<String, ScriptEngine> engines = Collections.singletonMap(engine.getType(), engine);
        scriptService = new ScriptService(Settings.EMPTY, engines, ScriptModule.CORE_CONTEXTS);
    }

    protected TemplateScript.Factory compile(String template) {
        Script script = new Script(ScriptType.INLINE, DEFAULT_TEMPLATE_LANG, template, Collections.emptyMap());
        return scriptService.compile(script, TemplateScript.CONTEXT);
    }
}
