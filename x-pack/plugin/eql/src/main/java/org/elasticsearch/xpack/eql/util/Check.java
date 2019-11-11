/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.eql.util;

import org.elasticsearch.xpack.eql.EqlIllegalArgumentException;

/**
 * Utility class used for checking various conditions at runtime, inside EQL (hence the specific exception) with
 * minimum amount of code
 */
public abstract class Check {

    public static void isTrue(boolean expression, String message, Object... values) {
        if (!expression) {
            throw new EqlIllegalArgumentException(message, values);
        }
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new EqlIllegalArgumentException(message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new EqlIllegalArgumentException(message);
        }
    }

    public static void notNull(Object object, String message, Object... values) {
        if (object == null) {
            throw new EqlIllegalArgumentException(message, values);
        }
    }
}
