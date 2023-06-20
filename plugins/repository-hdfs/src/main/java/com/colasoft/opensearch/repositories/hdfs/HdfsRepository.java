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

package com.colasoft.opensearch.repositories.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.AbstractFileSystem;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.UnsupportedFileSystemException;
import org.apache.hadoop.hdfs.client.HdfsClientConfigKeys;
import org.apache.hadoop.io.retry.FailoverProxyProvider;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.colasoft.opensearch.SpecialPermission;
import com.colasoft.opensearch.cluster.metadata.RepositoryMetadata;
import com.colasoft.opensearch.cluster.service.ClusterService;
import com.colasoft.opensearch.common.Strings;
import com.colasoft.opensearch.common.SuppressForbidden;
import com.colasoft.opensearch.common.blobstore.BlobPath;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.common.unit.ByteSizeValue;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.env.Environment;
import com.colasoft.opensearch.indices.recovery.RecoverySettings;
import com.colasoft.opensearch.repositories.blobstore.BlobStoreRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

public final class HdfsRepository extends BlobStoreRepository {

    private static final Logger logger = LogManager.getLogger(HdfsRepository.class);

    private static final String CONF_SECURITY_PRINCIPAL = "security.principal";

    private final Environment environment;
    private final ByteSizeValue chunkSize;
    private final BlobPath basePath = BlobPath.cleanPath();
    private final URI uri;
    private final String pathSetting;

    public HdfsRepository(
        final RepositoryMetadata metadata,
        final Environment environment,
        final NamedXContentRegistry namedXContentRegistry,
        final ClusterService clusterService,
        final RecoverySettings recoverySettings
    ) {
        super(metadata, metadata.settings().getAsBoolean("compress", false), namedXContentRegistry, clusterService, recoverySettings);

        this.environment = environment;
        this.chunkSize = metadata.settings().getAsBytesSize("chunk_size", null);

        String uriSetting = getMetadata().settings().get("uri");
        if (Strings.hasText(uriSetting) == false) {
            throw new IllegalArgumentException("No 'uri' defined for hdfs snapshot/restore");
        }
        uri = URI.create(uriSetting);
        if ("hdfs".equalsIgnoreCase(uri.getScheme()) == false) {
            throw new IllegalArgumentException(
                String.format(
                    Locale.ROOT,
                    "Invalid scheme [%s] specified in uri [%s]; only 'hdfs' uri allowed for hdfs snapshot/restore",
                    uri.getScheme(),
                    uriSetting
                )
            );
        }
        if (Strings.hasLength(uri.getPath()) && uri.getPath().equals("/") == false) {
            throw new IllegalArgumentException(
                String.format(
                    Locale.ROOT,
                    "Use 'path' option to specify a path [%s], not the uri [%s] for hdfs snapshot/restore",
                    uri.getPath(),
                    uriSetting
                )
            );
        }

        pathSetting = getMetadata().settings().get("path");
        // get configuration
        if (pathSetting == null) {
            throw new IllegalArgumentException("No 'path' defined for hdfs snapshot/restore");
        }
    }

    private HdfsBlobStore createBlobstore(URI uri, String path, Settings repositorySettings) {
        Configuration hadoopConfiguration = new Configuration(repositorySettings.getAsBoolean("load_defaults", true));
        hadoopConfiguration.setClassLoader(HdfsRepository.class.getClassLoader());
        hadoopConfiguration.reloadConfiguration();

        final Settings confSettings = repositorySettings.getByPrefix("conf.");
        for (String key : confSettings.keySet()) {
            logger.debug("Adding configuration to HDFS Client Configuration : {} = {}", key, confSettings.get(key));
            hadoopConfiguration.set(key, confSettings.get(key));
        }

        // Disable FS cache
        hadoopConfiguration.setBoolean("fs.hdfs.impl.disable.cache", true);

        // Create a hadoop user
        UserGroupInformation ugi = login(hadoopConfiguration, repositorySettings);

        // Sense if HA is enabled
        // HA requires elevated permissions during regular usage in the event that a failover operation
        // occurs and a new connection is required.
        String host = uri.getHost();
        String configKey = HdfsClientConfigKeys.Failover.PROXY_PROVIDER_KEY_PREFIX + "." + host;
        Class<?> ret = hadoopConfiguration.getClass(configKey, null, FailoverProxyProvider.class);
        boolean haEnabled = ret != null;

        // Create the filecontext with our user information
        // This will correctly configure the filecontext to have our UGI as its internal user.
        FileContext fileContext = ugi.doAs((PrivilegedAction<FileContext>) () -> {
            try {
                AbstractFileSystem fs = AbstractFileSystem.get(uri, hadoopConfiguration);
                return FileContext.getFileContext(fs, hadoopConfiguration);
            } catch (UnsupportedFileSystemException e) {
                throw new UncheckedIOException(e);
            }
        });

        logger.debug(
            "Using file-system [{}] for URI [{}], path [{}]",
            fileContext.getDefaultFileSystem(),
            fileContext.getDefaultFileSystem().getUri(),
            path
        );

        try {
            return new HdfsBlobStore(fileContext, path, bufferSize, isReadOnly(), haEnabled);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(Locale.ROOT, "Cannot create HDFS repository for uri [%s]", uri), e);
        }
    }

    private UserGroupInformation login(Configuration hadoopConfiguration, Settings repositorySettings) {
        // Validate the authentication method:
        AuthenticationMethod authMethod = SecurityUtil.getAuthenticationMethod(hadoopConfiguration);
        if (authMethod.equals(AuthenticationMethod.SIMPLE) == false && authMethod.equals(AuthenticationMethod.KERBEROS) == false) {
            throw new RuntimeException("Unsupported authorization mode [" + authMethod + "]");
        }

        // Check if the user added a principal to use, and that there is a keytab file provided
        String kerberosPrincipal = repositorySettings.get(CONF_SECURITY_PRINCIPAL);

        // Check to see if the authentication method is compatible
        if (kerberosPrincipal != null && authMethod.equals(AuthenticationMethod.SIMPLE)) {
            logger.warn(
                "Hadoop authentication method is set to [SIMPLE], but a Kerberos principal is "
                    + "specified. Continuing with [KERBEROS] authentication."
            );
            SecurityUtil.setAuthenticationMethod(AuthenticationMethod.KERBEROS, hadoopConfiguration);
        } else if (kerberosPrincipal == null && authMethod.equals(AuthenticationMethod.KERBEROS)) {
            throw new RuntimeException(
                "HDFS Repository does not support [KERBEROS] authentication without "
                    + "a valid Kerberos principal and keytab. Please specify a principal in the repository settings with ["
                    + CONF_SECURITY_PRINCIPAL
                    + "]."
            );
        }

        // Now we can initialize the UGI with the configuration.
        UserGroupInformation.setConfiguration(hadoopConfiguration);

        // Debugging
        logger.debug("Hadoop security enabled: [{}]", UserGroupInformation.isSecurityEnabled());
        logger.debug("Using Hadoop authentication method: [{}]", SecurityUtil.getAuthenticationMethod(hadoopConfiguration));

        // UserGroupInformation (UGI) instance is just a Hadoop specific wrapper around a Java Subject
        try {
            if (UserGroupInformation.isSecurityEnabled()) {
                String principal = preparePrincipal(kerberosPrincipal);
                String keytab = HdfsSecurityContext.locateKeytabFile(environment).toString();
                logger.debug("Using kerberos principal [{}] and keytab located at [{}]", principal, keytab);
                return UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab);
            }
            return UserGroupInformation.getCurrentUser();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not retrieve the current user information", e);
        }
    }

    // Convert principals of the format 'service/_HOST@REALM' by subbing in the local address for '_HOST'.
    private static String preparePrincipal(String originalPrincipal) {
        String finalPrincipal = originalPrincipal;
        // Don't worry about host name resolution if they don't have the _HOST pattern in the name.
        if (originalPrincipal.contains("_HOST")) {
            try {
                finalPrincipal = SecurityUtil.getServerPrincipal(originalPrincipal, getHostName());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            if (originalPrincipal.equals(finalPrincipal) == false) {
                logger.debug(
                    "Found service principal. Converted original principal name [{}] to server principal [{}]",
                    originalPrincipal,
                    finalPrincipal
                );
            }
        }
        return finalPrincipal;
    }

    @SuppressForbidden(reason = "InetAddress.getLocalHost(); Needed for filling in hostname for a kerberos principal name pattern.")
    private static String getHostName() {
        try {
            /*
             * This should not block since it should already be resolved via Log4J and Netty. The
             * host information is cached by the JVM and the TTL for the cache entry is infinite
             * when the SecurityManager is activated.
             */
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not locate host information", e);
        }
    }

    @Override
    protected HdfsBlobStore createBlobStore() {
        // initialize our blobstore using elevated privileges.
        SpecialPermission.check();
        final HdfsBlobStore blobStore = AccessController.doPrivileged(
            (PrivilegedAction<HdfsBlobStore>) () -> createBlobstore(uri, pathSetting, getMetadata().settings())
        );
        return blobStore;
    }

    @Override
    public BlobPath basePath() {
        return basePath;
    }

    @Override
    protected ByteSizeValue chunkSize() {
        return chunkSize;
    }
}
