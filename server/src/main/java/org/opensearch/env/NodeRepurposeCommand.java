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

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.opensearch.OpenSearchException;
import org.opensearch.cli.Terminal;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.coordination.OpenSearchNodeCommand;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.set.Sets;
import org.opensearch.core.internal.io.IOUtils;
import org.opensearch.gateway.MetadataStateFormat;
import org.opensearch.gateway.PersistedClusterStateService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.opensearch.env.NodeEnvironment.INDICES_FOLDER;

/**
 * Command to repurpose a node
 *
 * @opensearch.internal
 */
public class NodeRepurposeCommand extends OpenSearchNodeCommand {

    static final String ABORTED_BY_USER_MSG = OpenSearchNodeCommand.ABORTED_BY_USER_MSG;
    static final String FAILED_TO_OBTAIN_NODE_LOCK_MSG = OpenSearchNodeCommand.FAILED_TO_OBTAIN_NODE_LOCK_MSG;
    static final String NO_CLEANUP = "Node has node.data=true -> no clean up necessary";
    static final String NO_DATA_TO_CLEAN_UP_FOUND = "No data to clean-up found";
    static final String NO_SHARD_DATA_TO_CLEAN_UP_FOUND = "No shard data to clean-up found";

    public NodeRepurposeCommand() {
        super("Repurpose this node to another cluster-manager/data role, cleaning up any excess persisted data");
    }

    void testExecute(Terminal terminal, OptionSet options, Environment env) throws Exception {
        execute(terminal, options, env);
    }

    @Override
    protected boolean validateBeforeLock(Terminal terminal, Environment env) {
        Settings settings = env.settings();
        if (DiscoveryNode.isDataNode(settings)) {
            terminal.println(Terminal.Verbosity.NORMAL, NO_CLEANUP);
            return false;
        }

        return true;
    }

    @Override
    protected void processNodePaths(Terminal terminal, Path[] dataPaths, int nodeLockId, OptionSet options, Environment env)
        throws IOException {
        assert DiscoveryNode.isDataNode(env.settings()) == false;

        if (DiscoveryNode.isMasterNode(env.settings()) == false) {
            processNoClusterManagerNoDataNode(terminal, dataPaths, env);
        } else {
            processClusterManagerNoDataNode(terminal, dataPaths, env);
        }
    }

    private void processNoClusterManagerNoDataNode(Terminal terminal, Path[] dataPaths, Environment env) throws IOException {
        NodeEnvironment.NodePath[] nodePaths = toNodePaths(dataPaths);

        terminal.println(Terminal.Verbosity.VERBOSE, "Collecting shard data paths");
        List<Path> shardDataPaths = NodeEnvironment.collectShardDataPaths(nodePaths);

        terminal.println(Terminal.Verbosity.VERBOSE, "Collecting index metadata paths");
        List<Path> indexMetadataPaths = NodeEnvironment.collectIndexMetadataPaths(nodePaths);

        Set<Path> indexPaths = uniqueParentPaths(shardDataPaths, indexMetadataPaths);

        final PersistedClusterStateService persistedClusterStateService = createPersistedClusterStateService(env.settings(), dataPaths);

        final Metadata metadata = loadClusterState(terminal, env, persistedClusterStateService).metadata();
        if (indexPaths.isEmpty() && metadata.indices().isEmpty()) {
            terminal.println(Terminal.Verbosity.NORMAL, NO_DATA_TO_CLEAN_UP_FOUND);
            return;
        }

        final Set<String> indexUUIDs = Sets.union(
            indexUUIDsFor(indexPaths),
            StreamSupport.stream(metadata.indices().values().spliterator(), false)
                .map(imd -> imd.value.getIndexUUID())
                .collect(Collectors.toSet())
        );

        outputVerboseInformation(terminal, indexPaths, indexUUIDs, metadata);

        terminal.println(noClusterManagerMessage(indexUUIDs.size(), shardDataPaths.size(), indexMetadataPaths.size()));
        outputHowToSeeVerboseInformation(terminal);

        terminal.println("Node is being re-purposed as no-cluster-manager and no-data. Clean-up of index data will be performed.");
        confirm(terminal, "Do you want to proceed?");

        removePaths(terminal, indexPaths); // clean-up shard dirs
        // clean-up all metadata dirs
        MetadataStateFormat.deleteMetaState(dataPaths);
        IOUtils.rm(Stream.of(dataPaths).map(path -> path.resolve(INDICES_FOLDER)).toArray(Path[]::new));

        terminal.println("Node successfully repurposed to no-cluster-manager and no-data.");
    }

    private void processClusterManagerNoDataNode(Terminal terminal, Path[] dataPaths, Environment env) throws IOException {
        NodeEnvironment.NodePath[] nodePaths = toNodePaths(dataPaths);

        terminal.println(Terminal.Verbosity.VERBOSE, "Collecting shard data paths");
        List<Path> shardDataPaths = NodeEnvironment.collectShardDataPaths(nodePaths);
        if (shardDataPaths.isEmpty()) {
            terminal.println(NO_SHARD_DATA_TO_CLEAN_UP_FOUND);
            return;
        }

        final PersistedClusterStateService persistedClusterStateService = createPersistedClusterStateService(env.settings(), dataPaths);

        final Metadata metadata = loadClusterState(terminal, env, persistedClusterStateService).metadata();

        final Set<Path> indexPaths = uniqueParentPaths(shardDataPaths);
        final Set<String> indexUUIDs = indexUUIDsFor(indexPaths);

        outputVerboseInformation(terminal, shardDataPaths, indexUUIDs, metadata);

        terminal.println(shardMessage(shardDataPaths.size(), indexUUIDs.size()));
        outputHowToSeeVerboseInformation(terminal);

        terminal.println("Node is being re-purposed as cluster-manager and no-data. Clean-up of shard data will be performed.");
        confirm(terminal, "Do you want to proceed?");

        removePaths(terminal, shardDataPaths); // clean-up shard dirs

        terminal.println("Node successfully repurposed to cluster-manager and no-data.");
    }

    private ClusterState loadClusterState(Terminal terminal, Environment env, PersistedClusterStateService psf) throws IOException {
        terminal.println(Terminal.Verbosity.VERBOSE, "Loading cluster state");
        return clusterState(env, psf.loadBestOnDiskState());
    }

    private void outputVerboseInformation(Terminal terminal, Collection<Path> pathsToCleanup, Set<String> indexUUIDs, Metadata metadata) {
        if (terminal.isPrintable(Terminal.Verbosity.VERBOSE)) {
            terminal.println(Terminal.Verbosity.VERBOSE, "Paths to clean up:");
            pathsToCleanup.forEach(p -> terminal.println(Terminal.Verbosity.VERBOSE, "  " + p.toString()));
            terminal.println(Terminal.Verbosity.VERBOSE, "Indices affected:");
            indexUUIDs.forEach(uuid -> terminal.println(Terminal.Verbosity.VERBOSE, "  " + toIndexName(uuid, metadata)));
        }
    }

    private void outputHowToSeeVerboseInformation(Terminal terminal) {
        if (terminal.isPrintable(Terminal.Verbosity.VERBOSE) == false) {
            terminal.println("Use -v to see list of paths and indices affected");
        }
    }

    private String toIndexName(String uuid, Metadata metadata) {
        if (metadata != null) {
            for (ObjectObjectCursor<String, IndexMetadata> indexMetadata : metadata.indices()) {
                if (indexMetadata.value.getIndexUUID().equals(uuid)) {
                    return indexMetadata.value.getIndex().getName();
                }
            }
        }
        return "no name for uuid: " + uuid;
    }

    private Set<String> indexUUIDsFor(Set<Path> indexPaths) {
        return indexPaths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.toSet());
    }

    static String noClusterManagerMessage(int indexes, int shards, int indexMetadata) {
        return "Found " + indexes + " indices (" + shards + " shards and " + indexMetadata + " index meta data) to clean up";
    }

    static String shardMessage(int shards, int indices) {
        return "Found " + shards + " shards in " + indices + " indices to clean up";
    }

    private void removePaths(Terminal terminal, Collection<Path> paths) {
        terminal.println(Terminal.Verbosity.VERBOSE, "Removing data");
        paths.forEach(this::removePath);
    }

    private void removePath(Path path) {
        try {
            IOUtils.rm(path);
        } catch (IOException e) {
            throw new OpenSearchException("Unable to clean up path: " + path + ": " + e.getMessage());
        }
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private final Set<Path> uniqueParentPaths(Collection<Path>... paths) {
        // equals on Path is good enough here due to the way these are collected.
        return Arrays.stream(paths).flatMap(Collection::stream).map(Path::getParent).collect(Collectors.toSet());
    }

    // package-private for testing
    OptionParser getParser() {
        return parser;
    }
}
