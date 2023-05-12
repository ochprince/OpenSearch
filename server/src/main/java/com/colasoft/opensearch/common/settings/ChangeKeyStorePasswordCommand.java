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

package com.colasoft.opensearch.common.settings;

import joptsimple.OptionSet;
import com.colasoft.opensearch.cli.ExitCodes;
import com.colasoft.opensearch.cli.Terminal;
import com.colasoft.opensearch.cli.UserException;
import com.colasoft.opensearch.env.Environment;

/**
 * A sub-command for the keystore cli which changes the password.
 *
 * @opensearch.internal
 */
class ChangeKeyStorePasswordCommand extends BaseKeyStoreCommand {

    ChangeKeyStorePasswordCommand() {
        super("Changes the password of a keystore", true);
    }

    @Override
    protected void executeCommand(Terminal terminal, OptionSet options, Environment env) throws Exception {
        try (SecureString newPassword = readPassword(terminal, true)) {
            final KeyStoreWrapper keyStore = getKeyStore();
            keyStore.save(env.configFile(), newPassword.getChars());
            terminal.println("OpenSearch keystore password changed successfully.");
        } catch (SecurityException e) {
            throw new UserException(ExitCodes.DATA_ERROR, e.getMessage());
        }
    }
}
