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

package com.colasoft.opensearch.common.util.concurrent;

import java.util.concurrent.BlockingQueue;

/**
 * Extends the {@code SizeBlockingQueue} to add the {@code adjustCapacity} method, which will adjust
 * the capacity by a certain amount towards a maximum or minimum.
 *
 * @opensearch.internal
 */
final class ResizableBlockingQueue<E> extends SizeBlockingQueue<E> {

    private volatile int capacity;

    ResizableBlockingQueue(BlockingQueue<E> queue, int initialCapacity) {
        super(queue, initialCapacity);
        this.capacity = initialCapacity;
    }

    @Override
    public int capacity() {
        return this.capacity;
    }

    @Override
    public int remainingCapacity() {
        return Math.max(0, this.capacity());
    }

    /** Resize the limit for the queue, returning the new size limit */
    public synchronized int adjustCapacity(int optimalCapacity, int adjustmentAmount, int minCapacity, int maxCapacity) {
        assert adjustmentAmount > 0 : "adjustment amount should be a positive value";
        assert optimalCapacity >= 0 : "desired capacity cannot be negative";
        assert minCapacity >= 0 : "cannot have min capacity smaller than 0";
        assert maxCapacity >= minCapacity : "cannot have max capacity smaller than min capacity";

        if (optimalCapacity == capacity) {
            // Yahtzee!
            return this.capacity;
        }

        if (optimalCapacity > capacity + adjustmentAmount) {
            // adjust up
            final int newCapacity = Math.min(maxCapacity, capacity + adjustmentAmount);
            this.capacity = newCapacity;
            return newCapacity;
        } else if (optimalCapacity < capacity - adjustmentAmount) {
            // adjust down
            final int newCapacity = Math.max(minCapacity, capacity - adjustmentAmount);
            this.capacity = newCapacity;
            return newCapacity;
        } else {
            return this.capacity;
        }
    }
}
