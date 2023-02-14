/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.translog.transfer;

import org.opensearch.action.ActionListener;
import org.opensearch.index.translog.transfer.FileSnapshot.TransferFileSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Interface for the translog transfer service responsible for interacting with a remote store
 *
 * @opensearch.internal
 */
public interface TransferService {

    /**
     * Uploads the {@link TransferFileSnapshot} async, once the upload is complete the callback is invoked
     * @param threadpoolName threadpool type which will be used to upload blobs asynchronously
     * @param fileSnapshot the file snapshot to upload
     * @param remotePath the remote path where upload should be made
     * @param listener the callback to be invoked once upload completes successfully/fails
     */
    void uploadBlobAsync(
        String threadpoolName,
        final TransferFileSnapshot fileSnapshot,
        Iterable<String> remotePath,
        ActionListener<TransferFileSnapshot> listener
    );

    /**
     * Uploads the {@link TransferFileSnapshot} blob
     * @param fileSnapshot the file snapshot to upload
     * @param remotePath the remote path where upload should be made
     * @throws IOException the exception while transferring the data
     */
    void uploadBlob(final TransferFileSnapshot fileSnapshot, Iterable<String> remotePath) throws IOException;

    void deleteBlobs(Iterable<String> path, List<String> fileNames) throws IOException;

    /**
     * Deletes the list of files in async and uses the listener to propagate success or failure.
     * @param threadpoolName threadpool type which will be used to perform the deletion asynchronously.
     * @param path the path where the deletion would occur on remote store.
     * @param fileNames list of all files that are to be deleted within the path.
     * @param listener the callback to be invoked once delete completes successfully/fails.
     */
    void deleteBlobsAsync(String threadpoolName, Iterable<String> path, List<String> fileNames, ActionListener<Void> listener);

    /**
     *  Deletes all contents with-in a path.
     * @param path the path in remote which needs to be deleted completely.
     * @throws IOException the exception while transferring the data.
     */
    void delete(Iterable<String> path) throws IOException;

    /**
     * Deletes all contents with-in a path and invokes the listener on success or failure.
     *
     * @param threadpoolName threadpool type which will be used to perform the deletion asynchronously.
     * @param path           path in remote store.
     * @param listener       the callback to be invoked once delete completes successfully/fails.
     */
    void deleteAsync(String threadpoolName, Iterable<String> path, ActionListener<Void> listener);

    /**
     * Lists the files
     * @param path : the path to list
     * @return : the lists of files
     * @throws IOException the exception while listing the path
     */
    Set<String> listAll(Iterable<String> path) throws IOException;

    /**
     * Lists the files and invokes the listener on success or failure
     * @param threadpoolName threadpool type which will be used to list all files asynchronously.
     * @param path the path to list
     * @param listener the callback to be invoked once list operation completes successfully/fails.
     */
    void listAllAsync(String threadpoolName, Iterable<String> path, ActionListener<Set<String>> listener);

    /**
     * Lists the folders inside the path.
     * @param path : the path
     * @return list of folders inside the path
     * @throws IOException the exception while listing folders inside the path
     */
    Set<String> listFolders(Iterable<String> path) throws IOException;

    /**
     * Invokes the listener with the list of folders inside the path. For exception, invokes the {@code listener.onFailure}.
     *
     * @param threadpoolName threadpool type which will be used to perform the deletion asynchronously.
     * @param path           path in remote store
     * @param listener       the callback to be invoked once list folders succeeds or fails.
     */
    void listFoldersAsync(String threadpoolName, Iterable<String> path, ActionListener<Set<String>> listener);

    /**
     *
     * @param path  the remote path from where download should be made
     * @param fileName the name of the file
     * @return inputstream of the remote file
     * @throws IOException the exception while reading the data
     */
    InputStream downloadBlob(Iterable<String> path, String fileName) throws IOException;

}
