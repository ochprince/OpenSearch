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
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.common.inject;

import com.colasoft.opensearch.common.inject.internal.Errors;
import com.colasoft.opensearch.common.inject.spi.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;
import static com.colasoft.opensearch.common.util.set.Sets.newHashSet;

/**
 * Indicates that there was a runtime failure while providing an instance.
 *
 * @author kevinb@google.com (Kevin Bourrillion)
 * @author jessewilson@google.com (Jesse Wilson)
 * @since 2.0
 *
 * @opensearch.internal
 */
public final class ProvisionException extends RuntimeException {
    private final Set<Message> messages;

    /**
     * Creates a ConfigurationException containing {@code messages}.
     */
    public ProvisionException(Iterable<Message> messages) {
        this.messages = unmodifiableSet(newHashSet(messages));
        if (this.messages.isEmpty()) {
            throw new IllegalArgumentException();
        }
        initCause(Errors.getOnlyCause(this.messages));
    }

    public ProvisionException(String message, Throwable cause) {
        super(cause);
        this.messages = singleton(new Message(Collections.emptyList(), message, cause));
    }

    public ProvisionException(String message) {
        this.messages = singleton(new Message(message));
    }

    /**
     * Returns messages for the errors that caused this exception.
     */
    public Collection<Message> getErrorMessages() {
        return messages;
    }

    @Override
    public String getMessage() {
        return Errors.format("Guice provision errors", messages);
    }
}
