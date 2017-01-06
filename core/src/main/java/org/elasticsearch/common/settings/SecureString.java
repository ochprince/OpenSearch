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

package org.elasticsearch.common.settings;

import java.util.Arrays;
import java.util.Objects;

/**
 * A String implementations which allows clearing the underlying char array.
 */
public final class SecureString implements CharSequence, AutoCloseable {

    private char[] chars;

    /**
     * Constructs a new SecureString which controls the passed in char array.
     *
     * Note: When this instance is closed, the array will be zeroed out.
     */
    public SecureString(char[] chars) {
        this.chars = Objects.requireNonNull(chars);
    }

    /** Constant time equality to avoid potential timing attacks. */
    @Override
    public synchronized boolean equals(Object o) {
        ensureNotClosed();
        if (this == o) return true;
        if (o == null || o instanceof CharSequence == false) return false;
        CharSequence that = (CharSequence) o;
        if (chars.length != that.length()) {
            return false;
        }

        int equals = 0;
        for (int i = 0; i < chars.length; i++) {
            equals |= chars[i] ^ that.charAt(i);
        }

        return equals == 0;
    }

    @Override
    public synchronized int hashCode() {
        return Arrays.hashCode(chars);
    }

    @Override
    public synchronized int length() {
        ensureNotClosed();
        return chars.length;
    }

    @Override
    public synchronized char charAt(int index) {
        ensureNotClosed();
        return chars[index];
    }

    @Override
    public SecureString subSequence(int start, int end) {
        throw new UnsupportedOperationException("Cannot get subsequence of SecureString");
    }

    /**
     * Convert to a {@link String}. This should only be used with APIs that do not take {@link CharSequence}.
     */
    @Override
    public synchronized String toString() {
        return new String(chars);
    }

    /**
     * Closes the string by clearing the underlying char array.
     */
    @Override
    public synchronized void close() {
        Arrays.fill(chars, '\0');
        chars = null;
    }

    /** Throw an exception if this string has been closed, indicating something is trying to access the data after being closed. */
    private void ensureNotClosed() {
        if (chars == null) {
            throw new IllegalStateException("SecureString has already been closed");
        }
    }
}
