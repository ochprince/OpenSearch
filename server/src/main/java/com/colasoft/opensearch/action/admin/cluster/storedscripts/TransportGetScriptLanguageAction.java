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

package com.colasoft.opensearch.action.admin.cluster.storedscripts;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.support.ActionFilters;
import com.colasoft.opensearch.action.support.HandledTransportAction;
import com.colasoft.opensearch.common.inject.Inject;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.tasks.Task;
import com.colasoft.opensearch.transport.TransportService;

/**
 * Transport action for getting script language
 *
 * @opensearch.internal
 */
public class TransportGetScriptLanguageAction extends HandledTransportAction<GetScriptLanguageRequest, GetScriptLanguageResponse> {
    private final ScriptService scriptService;

    @Inject
    public TransportGetScriptLanguageAction(TransportService transportService, ActionFilters actionFilters, ScriptService scriptService) {
        super(GetScriptLanguageAction.NAME, transportService, actionFilters, GetScriptLanguageRequest::new);
        this.scriptService = scriptService;
    }

    @Override
    protected void doExecute(Task task, GetScriptLanguageRequest request, ActionListener<GetScriptLanguageResponse> listener) {
        listener.onResponse(new GetScriptLanguageResponse(scriptService.getScriptLanguages()));
    }
}
