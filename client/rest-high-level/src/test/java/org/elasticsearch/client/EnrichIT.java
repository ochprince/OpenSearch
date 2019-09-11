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
package org.elasticsearch.client;

import org.elasticsearch.client.core.AcknowledgedResponse;
import org.elasticsearch.client.enrich.DeletePolicyRequest;
import org.elasticsearch.client.enrich.GetPolicyRequest;
import org.elasticsearch.client.enrich.GetPolicyResponse;
import org.elasticsearch.client.enrich.PutPolicyRequest;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EnrichIT extends ESRestHighLevelClientTestCase {

    public void testCRUD() throws Exception {
        final EnrichClient enrichClient = highLevelClient().enrich();
        PutPolicyRequest putPolicyRequest = new PutPolicyRequest("my-policy", "match",
            Collections.singletonList("my-index"), "enrich_key", Collections.singletonList("enrich_value"));
        AcknowledgedResponse putPolicyResponse = execute(putPolicyRequest, enrichClient::putPolicy, enrichClient::putPolicyAsync);
        assertThat(putPolicyResponse.isAcknowledged(), is(true));

        GetPolicyRequest getPolicyRequest = randomBoolean() ? new GetPolicyRequest("my-policy") : new GetPolicyRequest();
        GetPolicyResponse getPolicyResponse = execute(getPolicyRequest, enrichClient::getPolicy, enrichClient::getPolicyAsync);
        assertThat(getPolicyResponse.getPolicies().size(), equalTo(1));
        assertThat(getPolicyResponse.getPolicies().get(0).getType(), equalTo(putPolicyRequest.getType()));
        assertThat(getPolicyResponse.getPolicies().get(0).getIndices(), equalTo(putPolicyRequest.getIndices()));
        assertThat(getPolicyResponse.getPolicies().get(0).getMatchField(), equalTo(putPolicyRequest.getMatchField()));
        assertThat(getPolicyResponse.getPolicies().get(0).getEnrichFields(), equalTo(putPolicyRequest.getEnrichFields()));

        DeletePolicyRequest deletePolicyRequest = new DeletePolicyRequest("my-policy");
        AcknowledgedResponse deletePolicyResponse =
            execute(deletePolicyRequest, enrichClient::deletePolicy, enrichClient::deletePolicyAsync);
        assertThat(deletePolicyResponse.isAcknowledged(), is(true));

        getPolicyRequest = new GetPolicyRequest();
        getPolicyResponse = execute(getPolicyRequest, enrichClient::getPolicy, enrichClient::getPolicyAsync);
        assertThat(getPolicyResponse.getPolicies().size(), equalTo(0));
    }

}
