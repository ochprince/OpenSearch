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

package com.colasoft.opensearch.index.reindex;

import org.junit.Before;
import com.colasoft.opensearch.action.ActionRequest;
import com.colasoft.opensearch.action.delete.DeleteRequest;
import com.colasoft.opensearch.action.index.IndexRequest;
import com.colasoft.opensearch.index.reindex.AbstractAsyncBulkByScrollAction.OpType;
import com.colasoft.opensearch.index.reindex.AbstractAsyncBulkByScrollAction.RequestWrapper;
import com.colasoft.opensearch.script.ScriptService;
import com.colasoft.opensearch.script.UpdateScript;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAsyncBulkByScrollActionScriptTestCase<
    Request extends AbstractBulkIndexByScrollRequest<Request>,
    Response extends BulkByScrollResponse> extends AbstractAsyncBulkByScrollActionTestCase<Request, Response> {

    protected ScriptService scriptService;

    @Before
    public void setupScriptService() {
        scriptService = mock(ScriptService.class);
    }

    @SuppressWarnings("unchecked")
    protected <T extends ActionRequest> T applyScript(Consumer<Map<String, Object>> scriptBody) {
        IndexRequest index = new IndexRequest("index").id("1").source(singletonMap("foo", "bar"));
        ScrollableHitSource.Hit doc = new ScrollableHitSource.BasicHit("test", "id", 0);
        UpdateScript.Factory factory = (params, ctx) -> new UpdateScript(Collections.emptyMap(), ctx) {
            @Override
            public void execute() {
                scriptBody.accept(getCtx());
            }
        };
        when(scriptService.compile(any(), eq(UpdateScript.CONTEXT))).thenReturn(factory);
        AbstractAsyncBulkByScrollAction<Request, ?> action = action(scriptService, request().setScript(mockScript("")));
        RequestWrapper<?> result = action.buildScriptApplier().apply(AbstractAsyncBulkByScrollAction.wrap(index), doc);
        return (result != null) ? (T) result.self() : null;
    }

    public void testScriptAddingJunkToCtxIsError() {
        try {
            applyScript((Map<String, Object> ctx) -> ctx.put("junk", "junk"));
            fail("Expected error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), equalTo("Invalid fields added to context [junk]"));
        }
    }

    public void testChangeSource() {
        IndexRequest index = applyScript((Map<String, Object> ctx) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) ctx.get("_source");
            source.put("bar", "cat");
        });
        assertEquals("cat", index.sourceAsMap().get("bar"));
    }

    public void testSetOpTypeDelete() throws Exception {
        DeleteRequest delete = applyScript((Map<String, Object> ctx) -> ctx.put("op", OpType.DELETE.toString()));
        assertThat(delete.index(), equalTo("index"));
        assertThat(delete.id(), equalTo("1"));
    }

    public void testSetOpTypeUnknown() throws Exception {
        IllegalArgumentException e = expectThrows(
            IllegalArgumentException.class,
            () -> applyScript((Map<String, Object> ctx) -> ctx.put("op", "unknown"))
        );
        assertThat(e.getMessage(), equalTo("Operation type [unknown] not allowed, only [noop, index, delete] are allowed"));
    }

    protected abstract AbstractAsyncBulkByScrollAction<Request, ?> action(ScriptService scriptService, Request request);
}
