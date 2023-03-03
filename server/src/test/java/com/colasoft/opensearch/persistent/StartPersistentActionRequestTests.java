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

package com.colasoft.opensearch.persistent;

import com.colasoft.opensearch.common.UUIDs;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry.Entry;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.persistent.StartPersistentTaskAction.Request;
import com.colasoft.opensearch.persistent.TestPersistentTasksPlugin.TestParams;
import com.colasoft.opensearch.persistent.TestPersistentTasksPlugin.TestPersistentTasksExecutor;
import com.colasoft.opensearch.test.AbstractWireSerializingTestCase;

import java.util.Collections;

public class StartPersistentActionRequestTests extends AbstractWireSerializingTestCase<Request> {

    @Override
    protected Request createTestInstance() {
        TestParams testParams = new TestParams();
        if (randomBoolean()) {
            testParams.setTestParam(randomAlphaOfLengthBetween(1, 20));
        }
        if (randomBoolean()) {
            testParams.setExecutorNodeAttr(randomAlphaOfLengthBetween(1, 20));
        }
        return new Request(UUIDs.base64UUID(), randomAlphaOfLengthBetween(1, 20), testParams);
    }

    @Override
    protected Writeable.Reader<Request> instanceReader() {
        return Request::new;
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(
            Collections.singletonList(new Entry(PersistentTaskParams.class, TestPersistentTasksExecutor.NAME, TestParams::new))
        );
    }
}
