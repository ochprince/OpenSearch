/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.indexlifecycle.action;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.indexlifecycle.action.SetPolicyForIndexAction;
import org.elasticsearch.xpack.indexlifecycle.IndexLifecycle;

import java.io.IOException;

public class RestSetPolicyForIndexAction extends BaseRestHandler {

    public RestSetPolicyForIndexAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(RestRequest.Method.PUT, "_" + IndexLifecycle.NAME + "/set_policy/{new_policy}", this);
        controller.registerHandler(RestRequest.Method.PUT, "{index}/_" + IndexLifecycle.NAME + "/set_policy/{new_policy}", this);
    }

    @Override
    public String getName() {
        return "xpack_set_policy_for_index_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        String[] indexes = Strings.splitStringByCommaToArray(restRequest.param("index"));
        String newPolicyName = restRequest.param("new_policy");
        SetPolicyForIndexAction.Request changePolicyRequest = new SetPolicyForIndexAction.Request(newPolicyName, indexes);
        changePolicyRequest.masterNodeTimeout(restRequest.paramAsTime("master_timeout", changePolicyRequest.masterNodeTimeout()));

        return channel -> client.execute(SetPolicyForIndexAction.INSTANCE, changePolicyRequest, new RestToXContentListener<>(channel));
    }
}