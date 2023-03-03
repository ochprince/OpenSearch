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

package com.colasoft.opensearch.cluster.coordination;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import com.colasoft.opensearch.cli.ExitCodes;
import com.colasoft.opensearch.cli.Terminal;
import com.colasoft.opensearch.cli.UserException;
import com.colasoft.opensearch.cluster.ClusterState;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.common.collect.Tuple;
import com.colasoft.opensearch.common.regex.Regex;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.gateway.PersistedClusterStateService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Removes custom settings
 *
 * @opensearch.internal
 */
public class RemoveSettingsCommand extends OpenSearchNodeCommand {

    static final String SETTINGS_REMOVED_MSG = "Settings were successfully removed from the cluster state";
    static final String CONFIRMATION_MSG = DELIMITER
        + "\n"
        + "You should only run this tool if you have incompatible settings in the\n"
        + "cluster state that prevent the cluster from forming.\n"
        + "This tool can cause data loss and its use should be your last resort.\n"
        + "\n"
        + "Do you want to proceed?\n";

    private final OptionSpec<String> arguments;

    public RemoveSettingsCommand() {
        super("Removes persistent settings from the cluster state");
        arguments = parser.nonOptions("setting names");
    }

    @Override
    protected void processNodePaths(Terminal terminal, Path[] dataPaths, int nodeLockId, OptionSet options, Environment env)
        throws IOException, UserException {
        final List<String> settingsToRemove = arguments.values(options);
        if (settingsToRemove.isEmpty()) {
            throw new UserException(ExitCodes.USAGE, "Must supply at least one setting to remove");
        }

        final PersistedClusterStateService persistedClusterStateService = createPersistedClusterStateService(env.settings(), dataPaths);

        terminal.println(Terminal.Verbosity.VERBOSE, "Loading cluster state");
        final Tuple<Long, ClusterState> termAndClusterState = loadTermAndClusterState(persistedClusterStateService, env);
        final ClusterState oldClusterState = termAndClusterState.v2();
        final Settings oldPersistentSettings = oldClusterState.metadata().persistentSettings();
        terminal.println(Terminal.Verbosity.VERBOSE, "persistent settings: " + oldPersistentSettings);
        final Settings.Builder newPersistentSettingsBuilder = Settings.builder().put(oldPersistentSettings);
        for (String settingToRemove : settingsToRemove) {
            boolean matched = false;
            for (String settingKey : oldPersistentSettings.keySet()) {
                if (Regex.simpleMatch(settingToRemove, settingKey)) {
                    newPersistentSettingsBuilder.remove(settingKey);
                    if (matched == false) {
                        terminal.println("The following settings will be removed:");
                    }
                    matched = true;
                    terminal.println(settingKey + ": " + oldPersistentSettings.get(settingKey));
                }
            }
            if (matched == false) {
                throw new UserException(
                    ExitCodes.USAGE,
                    "No persistent cluster settings matching [" + settingToRemove + "] were found on this node"
                );
            }
        }
        final ClusterState newClusterState = ClusterState.builder(oldClusterState)
            .metadata(Metadata.builder(oldClusterState.metadata()).persistentSettings(newPersistentSettingsBuilder.build()).build())
            .build();
        terminal.println(
            Terminal.Verbosity.VERBOSE,
            "[old cluster state = " + oldClusterState + ", new cluster state = " + newClusterState + "]"
        );

        confirm(terminal, CONFIRMATION_MSG);

        try (PersistedClusterStateService.Writer writer = persistedClusterStateService.createWriter()) {
            writer.writeFullStateAndCommit(termAndClusterState.v1(), newClusterState);
        }

        terminal.println(SETTINGS_REMOVED_MSG);
    }
}
