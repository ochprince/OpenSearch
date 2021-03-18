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

package org.opensearch.index.reindex;

import org.elasticsearch.index.reindex.AbstractBulkByScrollRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.ScrollableHitSource;

public abstract class AbstractAsyncBulkByScrollActionMetadataTestCase<
                Request extends AbstractBulkByScrollRequest<Request>,
                Response extends BulkByScrollResponse>
        extends AbstractAsyncBulkByScrollActionTestCase<Request, Response> {

    protected ScrollableHitSource.BasicHit doc() {
        return new ScrollableHitSource.BasicHit("index", "type", "id", 0);
    }

    protected abstract AbstractAsyncBulkByScrollAction<Request, ?> action();
}
