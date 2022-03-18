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

package org.opensearch.env;

import joptsimple.OptionSet;
import org.opensearch.OpenSearchException;
import org.opensearch.Version;
import org.opensearch.cli.MockTerminal;
import org.opensearch.cli.Terminal;
import org.opensearch.cluster.ClusterName;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.coordination.OpenSearchNodeCommand;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.node.DiscoveryNodeRole;
import org.opensearch.common.CheckedConsumer;
import org.opensearch.common.CheckedRunnable;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.BigArrays;
import org.opensearch.gateway.PersistedClusterStateService;
import org.opensearch.index.Index;
import org.opensearch.test.OpenSearchTestCase;
import org.hamcrest.Matcher;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Stream;

import static org.opensearch.env.NodeRepurposeCommand.NO_CLEANUP;
import static org.opensearch.env.NodeRepurposeCommand.NO_DATA_TO_CLEAN_UP_FOUND;
import static org.opensearch.env.NodeRepurposeCommand.NO_SHARD_DATA_TO_CLEAN_UP_FOUND;
import static org.opensearch.test.NodeRoles.masterNode;
import static org.opensearch.test.NodeRoles.nonDataNode;
import static org.opensearch.test.NodeRoles.nonMasterNode;
import static org.opensearch.test.NodeRoles.removeRoles;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class NodeRepurposeCommandTests extends OpenSearchTestCase {

    private static final Index INDEX = new Index("testIndex", "testUUID");
    private Settings dataMasterSettings;
    private Environment environment;
    private Path[] nodePaths;
    private Settings dataNoMasterSettings;
    private Settings noDataNoMasterSettings;
    private Settings noDataMasterSettings;

    @Before
    public void createNodePaths() throws IOException {
        dataMasterSettings = buildEnvSettings(Settings.EMPTY);
        environment = TestEnvironment.newEnvironment(dataMasterSettings);
        try (NodeEnvironment nodeEnvironment = new NodeEnvironment(dataMasterSettings, environment)) {
            nodePaths = nodeEnvironment.nodeDataPaths();
            final String nodeId = randomAlphaOfLength(10);
            try (
                PersistedClusterStateService.Writer writer = new PersistedClusterStateService(
                    nodePaths,
                    nodeId,
                    xContentRegistry(),
                    BigArrays.NON_RECYCLING_INSTANCE,
                    new ClusterSettings(dataMasterSettings, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS),
                    () -> 0L
                ).createWriter()
            ) {
                writer.writeFullStateAndCommit(1L, ClusterState.EMPTY_STATE);
            }
        }
        dataNoMasterSettings = nonMasterNode(dataMasterSettings);
        noDataNoMasterSettings = removeRoles(
            dataMasterSettings,
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DiscoveryNodeRole.DATA_ROLE, DiscoveryNodeRole.CLUSTER_MANAGER_ROLE)))
        );

        noDataMasterSettings = masterNode(nonDataNode(dataMasterSettings));
    }

    public void testEarlyExitNoCleanup() throws Exception {
        createIndexDataFiles(dataMasterSettings, randomInt(10), randomBoolean());

        verifyNoQuestions(dataMasterSettings, containsString(NO_CLEANUP));
        verifyNoQuestions(dataNoMasterSettings, containsString(NO_CLEANUP));
    }

    public void testNothingToCleanup() throws Exception {
        verifyNoQuestions(noDataNoMasterSettings, containsString(NO_DATA_TO_CLEAN_UP_FOUND));
        verifyNoQuestions(noDataMasterSettings, containsString(NO_SHARD_DATA_TO_CLEAN_UP_FOUND));

        Environment environment = TestEnvironment.newEnvironment(noDataMasterSettings);
        if (randomBoolean()) {
            try (NodeEnvironment env = new NodeEnvironment(noDataMasterSettings, environment)) {
                try (
                    PersistedClusterStateService.Writer writer = OpenSearchNodeCommand.createPersistedClusterStateService(
                        Settings.EMPTY,
                        env.nodeDataPaths()
                    ).createWriter()
                ) {
                    writer.writeFullStateAndCommit(1L, ClusterState.EMPTY_STATE);
                }
            }
        }

        verifyNoQuestions(noDataNoMasterSettings, containsString(NO_DATA_TO_CLEAN_UP_FOUND));
        verifyNoQuestions(noDataMasterSettings, containsString(NO_SHARD_DATA_TO_CLEAN_UP_FOUND));

        createIndexDataFiles(dataMasterSettings, 0, randomBoolean());

        verifyNoQuestions(noDataMasterSettings, containsString(NO_SHARD_DATA_TO_CLEAN_UP_FOUND));

    }

    public void testLocked() throws IOException {
        try (NodeEnvironment env = new NodeEnvironment(dataMasterSettings, TestEnvironment.newEnvironment(dataMasterSettings))) {
            assertThat(
                expectThrows(OpenSearchException.class, () -> verifyNoQuestions(noDataNoMasterSettings, null)).getMessage(),
                containsString(NodeRepurposeCommand.FAILED_TO_OBTAIN_NODE_LOCK_MSG)
            );
        }
    }

    public void testCleanupAll() throws Exception {
        int shardCount = randomIntBetween(1, 10);
        boolean verbose = randomBoolean();
        boolean hasClusterState = randomBoolean();
        createIndexDataFiles(dataMasterSettings, shardCount, hasClusterState);

        String messageText = NodeRepurposeCommand.noMasterMessage(1, environment.dataFiles().length * shardCount, 0);

        Matcher<String> outputMatcher = allOf(
            containsString(messageText),
            conditionalNot(containsString("testIndex"), verbose == false || hasClusterState == false),
            conditionalNot(containsString("no name for uuid: testUUID"), verbose == false || hasClusterState)
        );

        verifyUnchangedOnAbort(noDataNoMasterSettings, outputMatcher, verbose);

        // verify test setup
        expectThrows(IllegalStateException.class, () -> new NodeEnvironment(noDataNoMasterSettings, environment).close());

        verifySuccess(noDataNoMasterSettings, outputMatcher, verbose);

        // verify cleaned.
        new NodeEnvironment(noDataNoMasterSettings, environment).close();
    }

    public void testCleanupShardData() throws Exception {
        int shardCount = randomIntBetween(1, 10);
        boolean verbose = randomBoolean();
        boolean hasClusterState = randomBoolean();
        createIndexDataFiles(dataMasterSettings, shardCount, hasClusterState);

        Matcher<String> matcher = allOf(
            containsString(NodeRepurposeCommand.shardMessage(environment.dataFiles().length * shardCount, 1)),
            conditionalNot(containsString("testUUID"), verbose == false),
            conditionalNot(containsString("testIndex"), verbose == false || hasClusterState == false),
            conditionalNot(containsString("no name for uuid: testUUID"), verbose == false || hasClusterState)
        );

        verifyUnchangedOnAbort(noDataMasterSettings, matcher, verbose);

        // verify test setup
        expectThrows(IllegalStateException.class, () -> new NodeEnvironment(noDataMasterSettings, environment).close());

        verifySuccess(noDataMasterSettings, matcher, verbose);

        // verify clean.
        new NodeEnvironment(noDataMasterSettings, environment).close();
    }

    static void verifySuccess(Settings settings, Matcher<String> outputMatcher, boolean verbose) throws Exception {
        withTerminal(verbose, outputMatcher, terminal -> {
            terminal.addTextInput(randomFrom("y", "Y"));
            executeRepurposeCommand(terminal, settings, 0);
            assertThat(terminal.getOutput(), containsString("Node successfully repurposed"));
        });
    }

    private void verifyUnchangedOnAbort(Settings settings, Matcher<String> outputMatcher, boolean verbose) throws Exception {
        withTerminal(verbose, outputMatcher, terminal -> {
            terminal.addTextInput(randomFrom("yy", "Yy", "n", "yes", "true", "N", "no"));
            verifyUnchangedDataFiles(() -> {
                OpenSearchException exception = expectThrows(
                    OpenSearchException.class,
                    () -> executeRepurposeCommand(terminal, settings, 0)
                );
                assertThat(exception.getMessage(), containsString(NodeRepurposeCommand.ABORTED_BY_USER_MSG));
            });
        });
    }

    private void verifyNoQuestions(Settings settings, Matcher<String> outputMatcher) throws Exception {
        withTerminal(false, outputMatcher, terminal -> { executeRepurposeCommand(terminal, settings, 0); });
    }

    private static void withTerminal(boolean verbose, Matcher<String> outputMatcher, CheckedConsumer<MockTerminal, Exception> consumer)
        throws Exception {
        MockTerminal terminal = new MockTerminal();
        if (verbose) {
            terminal.setVerbosity(Terminal.Verbosity.VERBOSE);
        }

        consumer.accept(terminal);

        assertThat(terminal.getOutput(), outputMatcher);

        expectThrows(IllegalStateException.class, "Must consume input", () -> terminal.readText(""));
    }

    private static void executeRepurposeCommand(MockTerminal terminal, Settings settings, int ordinal) throws Exception {
        NodeRepurposeCommand nodeRepurposeCommand = new NodeRepurposeCommand();
        OptionSet options = nodeRepurposeCommand.getParser()
            .parse(ordinal != 0 ? new String[] { "--ordinal", Integer.toString(ordinal) } : new String[0]);
        Environment env = TestEnvironment.newEnvironment(settings);
        nodeRepurposeCommand.testExecute(terminal, options, env);
    }

    private void createIndexDataFiles(Settings settings, int shardCount, boolean writeClusterState) throws IOException {
        int shardDataDirNumber = randomInt(10);
        Environment environment = TestEnvironment.newEnvironment(settings);
        try (NodeEnvironment env = new NodeEnvironment(settings, environment)) {
            if (writeClusterState) {
                try (
                    PersistedClusterStateService.Writer writer = OpenSearchNodeCommand.createPersistedClusterStateService(
                        Settings.EMPTY,
                        env.nodeDataPaths()
                    ).createWriter()
                ) {
                    writer.writeFullStateAndCommit(
                        1L,
                        ClusterState.builder(ClusterName.DEFAULT)
                            .metadata(
                                Metadata.builder()
                                    .put(
                                        IndexMetadata.builder(INDEX.getName())
                                            .settings(
                                                Settings.builder()
                                                    .put("index.version.created", Version.CURRENT)
                                                    .put(IndexMetadata.SETTING_INDEX_UUID, INDEX.getUUID())
                                            )
                                            .numberOfShards(1)
                                            .numberOfReplicas(1)
                                    )
                                    .build()
                            )
                            .build()
                    );
                }
            }
            for (Path path : env.indexPaths(INDEX)) {
                for (int i = 0; i < shardCount; ++i) {
                    Files.createDirectories(path.resolve(Integer.toString(shardDataDirNumber)));
                    shardDataDirNumber += randomIntBetween(1, 10);
                }
            }
        }
    }

    private void verifyUnchangedDataFiles(CheckedRunnable<? extends Exception> runnable) throws Exception {
        long before = digestPaths();
        runnable.run();
        long after = digestPaths();
        assertEquals("Must not touch files", before, after);
    }

    private long digestPaths() {
        // use a commutative digest to avoid dependency on file system order.
        return Arrays.stream(environment.dataFiles()).mapToLong(this::digestPath).sum();
    }

    private long digestPath(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths.mapToLong(this::digestSinglePath).sum();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long digestSinglePath(Path path) {
        if (Files.isDirectory(path)) return path.toString().hashCode();
        else return path.toString().hashCode() + digest(readAllBytes(path));

    }

    private byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long digest(byte[] bytes) {
        long result = 0;
        for (byte b : bytes) {
            result *= 31;
            result += b;
        }
        return result;
    }

    static <T> Matcher<T> conditionalNot(Matcher<T> matcher, boolean condition) {
        return condition ? not(matcher) : matcher;
    }
}
