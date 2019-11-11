/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.eql;

public abstract class EqlServerException extends EqlException {

    protected EqlServerException(String message, Object... args) {
        super(message, args);
    }

    protected EqlServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    protected EqlServerException(String message, Throwable cause) {
        super(message, cause);
    }

    protected EqlServerException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }

    protected EqlServerException(Throwable cause) {
        super(cause);
    }
}
