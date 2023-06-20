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

package com.colasoft.opensearch.index.translog.transfer;

import com.colasoft.opensearch.action.ActionListener;
import com.colasoft.opensearch.action.LatchedActionListener;
import com.colasoft.opensearch.cluster.metadata.RepositoryMetadata;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.settings.ClusterSettings;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.env.TestEnvironment;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.repositories.Repository;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreRepository;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreTestUtil;
import com.colasoft.opensearch.repositories.fs.FsRepository;
import com.colasoft.opensearch.test.OpenSearchTestCase;
import com.colasoft.opensearch.threadpool.TestThreadPool;
import com.colasoft.opensearch.threadpool.ThreadPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlobStoreTransferServiceTests extends OpenSearchTestCase {

    private ThreadPool threadPool;

    private BlobStoreRepository repository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        repository = createRepository();
        threadPool = new TestThreadPool(getClass().getName());
    }

    public void testUploadBlob() throws IOException {
        Path testFile = createTempFile();
        Files.write(testFile, randomByteArrayOfLength(128), StandardOpenOption.APPEND);
        FileSnapshot.TransferFileSnapshot transferFileSnapshot = new FileSnapshot.TransferFileSnapshot(testFile, randomNonNegativeLong());
        TransferService transferService = new BlobStoreTransferService(repository.blobStore(), threadPool);
        transferService.uploadBlob(transferFileSnapshot, repository.basePath());
    }

    public void testUploadBlobFromByteArray() throws IOException {
        FileSnapshot.TransferFileSnapshot transferFileSnapshot = new FileSnapshot.TransferFileSnapshot(
            "dummy_name",
            randomByteArrayOfLength(128),
            1
        );
        TransferService transferService = new BlobStoreTransferService(repository.blobStore(), threadPool);
        transferService.uploadBlob(transferFileSnapshot, repository.basePath());
    }

    public void testUploadBlobAsync() throws IOException, InterruptedException {
        Path testFile = createTempFile();
        Files.write(testFile, randomByteArrayOfLength(128), StandardOpenOption.APPEND);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        FileSnapshot.TransferFileSnapshot transferFileSnapshot = new FileSnapshot.TransferFileSnapshot(testFile, randomNonNegativeLong());
        CountDownLatch latch = new CountDownLatch(1);
        TransferService transferService = new BlobStoreTransferService(repository.blobStore(), threadPool);
        transferService.uploadBlobAsync(
            ThreadPool.Names.TRANSLOG_TRANSFER,
            transferFileSnapshot,
            repository.basePath(),
            new LatchedActionListener<>(new ActionListener<>() {
                @Override
                public void onResponse(FileSnapshot.TransferFileSnapshot fileSnapshot) {
                    assert succeeded.compareAndSet(false, true);
                    assertEquals(transferFileSnapshot.getPrimaryTerm(), fileSnapshot.getPrimaryTerm());
                    assertEquals(transferFileSnapshot.getName(), fileSnapshot.getName());
                }

                @Override
                public void onFailure(Exception e) {
                    throw new AssertionError("Failed to perform uploadBlobAsync", e);
                }
            }, latch)
        );
        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        assertTrue(succeeded.get());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        repository.stop();
        ThreadPool.terminate(threadPool, 10, TimeUnit.SECONDS);
    }

    /** Create a {@link Repository} with a random name **/
    private BlobStoreRepository createRepository() {
        Settings settings = Settings.builder().put("location", randomAlphaOfLength(10)).build();
        RepositoryMetadata repositoryMetadata = new RepositoryMetadata(randomAlphaOfLength(10), FsRepository.TYPE, settings);
        final ClusterService clusterService = BlobStoreTestUtil.mockClusterService(repositoryMetadata);
        final FsRepository repository = new FsRepository(
            repositoryMetadata,
            createEnvironment(),
            xContentRegistry(),
            clusterService,
            new RecoverySettings(Settings.EMPTY, new ClusterSettings(Settings.EMPTY, ClusterSettings.BUILT_IN_CLUSTER_SETTINGS))
        ) {
            @Override
            protected void assertSnapshotOrGenericThread() {
                // eliminate thread name check as we create repo manually
            }
        };
        clusterService.addStateApplier(event -> repository.updateState(event.state()));
        // Apply state once to initialize repo properly like RepositoriesService would
        repository.updateState(clusterService.state());
        repository.start();
        return repository;
    }

    /** Create a {@link Environment} with random path.home and path.repo **/
    private Environment createEnvironment() {
        Path home = createTempDir();
        return TestEnvironment.newEnvironment(
            Settings.builder()
                .put(Environment.PATH_HOME_SETTING.getKey(), home.toAbsolutePath())
                .put(Environment.PATH_REPO_SETTING.getKey(), home.resolve("repo").toAbsolutePath())
                .build()
        );
    }
}
