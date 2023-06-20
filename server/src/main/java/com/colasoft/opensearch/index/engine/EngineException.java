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

package com.colasoft.opensearch.index.engine;

import com.colasoft.opensearch.OpenSearchException;
import com.colasoft.opensearch.common.io.stream.StreamInput;
import com.colasoft.opensearch.index.shard.ShardId;

import java.io.IOException;

/**
 * Exception if there are any errors in the engine
 *
 * @opensearch.internal
 */
public class EngineException extends OpenSearchException {

    public EngineException(ShardId shardId, String msg, Object... params) {
        this(shardId, msg, null, params);
    }

    public EngineException(ShardId shardId, String msg, Throwable cause, Object... params) {
        super(msg, cause, params);
        setShard(shardId);
    }

    public EngineException(StreamInput in) throws IOException {
        super(in);
    }
}
