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

package org.opensearch.common.xcontent.support;

import org.opensearch.common.xcontent.DeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentLocation;
import org.opensearch.common.xcontent.XContentType;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wraps a map generated by XContentParser's map() method into XContent Parser
 */
public class MapXContentParser extends AbstractXContentParser {

    private XContentType xContentType;
    private TokenIterator iterator;
    private boolean closed;

    public MapXContentParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler, Map<String, Object> map,
                             XContentType xContentType) {
        super(xContentRegistry, deprecationHandler);
        this.xContentType = xContentType;
        this.iterator = new MapIterator(null, null, map);
    }


    @Override
    protected boolean doBooleanValue() throws IOException {
        if (iterator != null && iterator.currentValue() instanceof Boolean) {
            return (Boolean) iterator.currentValue();
        } else {
            throw new IllegalStateException("Cannot get boolean value for the current token " + currentToken());
        }
    }

    @Override
    protected short doShortValue() throws IOException {
        return numberValue().shortValue();
    }

    @Override
    protected int doIntValue() throws IOException {
        return numberValue().intValue();
    }

    @Override
    protected long doLongValue() throws IOException {
        return numberValue().longValue();
    }

    @Override
    protected float doFloatValue() throws IOException {
        return numberValue().floatValue();
    }

    @Override
    protected double doDoubleValue() throws IOException {
        return numberValue().doubleValue();
    }

    @Override
    public XContentType contentType() {
        return xContentType;
    }

    @Override
    public Token nextToken() throws IOException {
        if (iterator == null) {
            return null;
        } else {
            iterator = iterator.next();
        }
        return currentToken();
    }

    @Override
    public void skipChildren() throws IOException {
        Token token = currentToken();
        if (token == Token.START_OBJECT || token == Token.START_ARRAY) {
            iterator = iterator.skipChildren();
        }
    }

    @Override
    public Token currentToken() {
        if (iterator == null) {
            return null;
        } else {
            return iterator.currentToken();
        }
    }

    @Override
    public String currentName() throws IOException {
        if (iterator == null) {
            return null;
        } else {
            return iterator.currentName();
        }
    }

    @Override
    public String text() throws IOException {
        if (iterator != null) {
            if (currentToken() == Token.VALUE_STRING || currentToken() == Token.VALUE_NUMBER || currentToken() == Token.VALUE_BOOLEAN) {
                return iterator.currentValue().toString();
            } else if (currentToken() == Token.FIELD_NAME) {
                return iterator.currentName();
            } else {
                return null;
            }
        } else {
            throw new IllegalStateException("Cannot get text for the current token " + currentToken());
        }
    }

    @Override
    public CharBuffer charBuffer() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public Object objectText() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public Object objectBytes() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public boolean hasTextCharacters() {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public char[] textCharacters() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public int textLength() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public int textOffset() throws IOException {
        throw new UnsupportedOperationException("use text() instead");
    }

    @Override
    public Number numberValue() throws IOException {
        if (iterator != null && currentToken() == Token.VALUE_NUMBER) {
            return (Number) iterator.currentValue();
        } else {
            throw new IllegalStateException("Cannot get numeric value for the current token " + currentToken());
        }
    }

    @Override
    public NumberType numberType() throws IOException {
        Number number = numberValue();
        if (number instanceof Integer) {
            return NumberType.INT;
        } else if (number instanceof BigInteger) {
            return NumberType.BIG_INTEGER;
        } else if (number instanceof Long) {
            return NumberType.LONG;
        } else if (number instanceof Float) {
            return NumberType.FLOAT;
        } else if (number instanceof Double) {
            return NumberType.DOUBLE;
        } else if (number instanceof BigDecimal) {
            return NumberType.BIG_DECIMAL;
        }
        throw new IllegalStateException("No matching token for number_type [" + number.getClass() + "]");
    }

    @Override
    public byte[] binaryValue() throws IOException {
        if (iterator != null && iterator.currentValue() instanceof byte[]) {
            return (byte[]) iterator.currentValue();
        } else {
            throw new IllegalStateException("Cannot get binary value for the current token " + currentToken());
        }
    }

    @Override
    public XContentLocation getTokenLocation() {
        return new XContentLocation(0, 0);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    /**
     * Iterator over the elements of the map
     */
    private abstract static class TokenIterator {
        protected final TokenIterator parent;
        protected final String name;
        protected Token currentToken;
        protected State state = State.BEFORE;

        TokenIterator(TokenIterator parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        public abstract TokenIterator next();

        public abstract TokenIterator skipChildren();

        public Token currentToken() {
            return currentToken;
        }

        public abstract Object currentValue();

        /**
         * name of the field name of the current element
         */
        public abstract String currentName();

        /**
         * field name that the child element needs to inherit.
         *
         * In most cases this is the same as currentName() except with embedded arrays. In "foo": [[42]] the first START_ARRAY
         * token will have the name "foo", but the second START_ARRAY will have no name.
         */
        public abstract String childName();

        @SuppressWarnings("unchecked")
        TokenIterator processValue(Object value) {
            if (value instanceof Map) {
                return new MapIterator(this, childName(), (Map<String, Object>) value).next();
            } else if (value instanceof List) {
                return new ArrayIterator(this, childName(), (List<Object>) value).next();
            } else if (value instanceof Number) {
                currentToken = Token.VALUE_NUMBER;
            } else if (value instanceof String) {
                currentToken = Token.VALUE_STRING;
            } else if (value instanceof Boolean) {
                currentToken = Token.VALUE_BOOLEAN;
            } else if (value instanceof byte[]) {
                currentToken = Token.VALUE_EMBEDDED_OBJECT;
            } else if (value == null) {
                currentToken = Token.VALUE_NULL;
            }
            return this;
        }

    }

    private enum State {
        BEFORE,
        NAME,
        VALUE,
        AFTER
    }

    /**
     * Iterator over the map
     */
    private static class MapIterator extends TokenIterator {

        private final Iterator<Map.Entry<String, Object>> iterator;

        private Map.Entry<String, Object> entry;

        MapIterator(TokenIterator parent, String name, Map<String, Object> map) {
            super(parent, name);
            iterator = map.entrySet().iterator();
        }

        @Override
        public TokenIterator next() {
            switch (state) {
                case BEFORE:
                    state = State.NAME;
                    currentToken = Token.START_OBJECT;
                    return this;
                case NAME:
                    if (iterator.hasNext()) {
                        state = State.VALUE;
                        entry = iterator.next();
                        currentToken = Token.FIELD_NAME;
                        return this;
                    } else {
                        state = State.AFTER;
                        entry = null;
                        currentToken = Token.END_OBJECT;
                        return this;
                    }
                case VALUE:
                    state = State.NAME;
                    return processValue(entry.getValue());
                case AFTER:
                    currentToken = null;
                    if (parent == null) {
                        return null;
                    } else {
                        return parent.next();
                    }
                default:
                    throw new IllegalArgumentException("Unknown state " + state);

            }
        }

        @Override
        public TokenIterator skipChildren() {
            state = State.AFTER;
            entry = null;
            currentToken = Token.END_OBJECT;
            return this;
        }

        @Override
        public Object currentValue() {
            if (entry == null) {
                throw new IllegalStateException("Cannot get value for non-value token " + currentToken);
            }
            return entry.getValue();
        }

        @Override
        public String currentName() {
            if (entry == null) {
                return name;
            }
            return entry.getKey();
        }

        @Override
        public String childName() {
            return currentName();
        }
    }

    private static class ArrayIterator extends TokenIterator {
        private final Iterator<Object> iterator;

        private Object value;

        private ArrayIterator(TokenIterator parent, String name, List<Object> list) {
            super(parent, name);
            iterator = list.iterator();
        }

        @Override
        public TokenIterator next() {
            switch (state) {
                case BEFORE:
                    state = State.VALUE;
                    currentToken = Token.START_ARRAY;
                    return this;
                case VALUE:
                    if (iterator.hasNext()) {
                        value = iterator.next();
                        return processValue(value);
                    } else {
                        state = State.AFTER;
                        value = null;
                        currentToken = Token.END_ARRAY;
                        return this;
                    }
                case AFTER:
                    currentToken = null;
                    if (parent == null) {
                        return null;
                    } else {
                        return parent.next();
                    }
                default:
                    throw new IllegalArgumentException("Unknown state " + state);
            }
        }

        @Override
        public TokenIterator skipChildren() {
            state = State.AFTER;
            value = null;
            currentToken = Token.END_ARRAY;
            return this;
        }

        @Override
        public Object currentValue() {
            return value;
        }

        @Override
        public String currentName() {
            if (parent == null || (currentToken != Token.START_ARRAY && currentToken != Token.END_ARRAY)) {
                return null;
            } else {
                return name;
            }
        }

        @Override
        public String childName() {
            return null;
        }
    }
}
