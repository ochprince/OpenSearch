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

package com.colasoft.opensearch.rest.action.admin.cluster;

import org.junit.Before;
import com.colasoft.opensearch.action.admin.cluster.decommission.awareness.delete.DeleteDecommissionStateRequest;
import com.colasoft.opensearch.rest.RestHandler;
import com.colasoft.opensearch.rest.RestRequest;
import com.colasoft.opensearch.test.rest.RestActionTestCase;

import java.util.List;

public class RestDeleteDecommissionStateActionTests extends RestActionTestCase {

    private RestDeleteDecommissionStateAction action;

    @Before
    public void setupAction() {
        action = new RestDeleteDecommissionStateAction();
        controller().registerHandler(action);
    }

    public void testRoutes() {
        List<RestHandler.Route> routes = action.routes();
        RestHandler.Route route = routes.get(0);
        assertEquals(route.getMethod(), RestRequest.Method.DELETE);
        assertEquals("/_cluster/decommission/awareness", route.getPath());
    }

    public void testCreateRequest() {
        DeleteDecommissionStateRequest request = action.createRequest();
        assertNotNull(request);
    }
}
