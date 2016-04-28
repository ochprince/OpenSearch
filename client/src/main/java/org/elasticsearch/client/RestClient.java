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

import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public final class RestClient implements Closeable {

    private final Transport transport;

    public RestClient(CloseableHttpClient client, ConnectionPool<? extends Connection> connectionPool, long maxRetryTimeout) {
        this.transport = new Transport<>(client, connectionPool, maxRetryTimeout);
    }

    public ElasticsearchResponse performRequest(Verb verb, String endpoint, Map<String, Object> params, HttpEntity entity)
            throws IOException {
        return transport.performRequest(verb, endpoint, params, entity);
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}
