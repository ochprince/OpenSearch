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

package org.opensearch.common.recycler;

/**
 * Base filter recycler.
 *
 * @opensearch.internal
 */
abstract class FilterRecycler<T> implements Recycler<T> {

    /** Get the delegate instance to forward calls to. */
    protected abstract Recycler<T> getDelegate();

    /** Wrap a recycled reference. */
    protected Recycler.V<T> wrap(Recycler.V<T> delegate) {
        return delegate;
    }

    @Override
    public Recycler.V<T> obtain() {
        return wrap(getDelegate().obtain());
    }

}
