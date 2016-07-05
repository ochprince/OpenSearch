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
package org.elasticsearch.rest.action.search.template;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.admin.cluster.storedscripts.RestDeleteStoredScriptAction;
import org.elasticsearch.script.Template;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

public class RestDeleteSearchTemplateAction extends RestDeleteStoredScriptAction {

    @Inject
    public RestDeleteSearchTemplateAction(Settings settings, RestController controller) {
        super(settings, controller, false);
        controller.registerHandler(DELETE, "/_search/template/{id}", this);
    }

    @Override
    protected String getScriptLang(RestRequest request) {
        return Template.DEFAULT_LANG;
    }
}