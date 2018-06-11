/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ssl;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.TestEnvironment;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.core.ssl.CertParsingUtils;
import org.elasticsearch.xpack.core.ssl.PemUtils;
import org.elasticsearch.xpack.core.ssl.RestrictedTrustManager;
import org.elasticsearch.xpack.core.ssl.SSLService;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.SocketException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for SSL trust restrictions
 *
 * @see RestrictedTrustManager
 */
@ESIntegTestCase.ClusterScope(numDataNodes = 1, numClientNodes = 0, supportsDedicatedMasters = false)
@TestLogging("org.elasticsearch.xpack.ssl.RestrictedTrustManager:DEBUG")
public class SSLTrustRestrictionsTests extends SecurityIntegTestCase {

    private static final int RESOURCE_RELOAD_MILLIS = 3;
    private static final TimeValue MAX_WAIT_RELOAD = TimeValue.timeValueSeconds(1);

    private static Path configPath;
    private static Settings nodeSSL;

    private static CertificateInfo ca;
    private static CertificateInfo trustedCert;
    private static CertificateInfo untrustedCert;
    private static Path restrictionsPath;
    private static Path restrictionsTmpPath;

    @Override
    protected int maxNumberOfNodes() {
        // We are trying to test the SSL configuration for which clients/nodes may join a cluster
        // We prefer the cluster to only have 1 node, so that the SSL checking doesn't happen until the test methods run
        // (That's not _quite_ true, because the base setup code checks the cluster using transport client, but it's the best we can do)
        return 1;
    }

    @BeforeClass
    public static void setupCertificates() throws Exception {
        configPath = createTempDir();
        Path caCertPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/ca.crt").toURI());
        X509Certificate caCert = CertParsingUtils.readX509Certificates(Collections.singletonList(caCertPath))[0];
        Path caKeyPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/ca.key").toURI());
        PrivateKey caKey = PemUtils.readPrivateKey(caKeyPath, ""::toCharArray);
        ca = new CertificateInfo(caKey, caKeyPath, caCert, caCertPath);

        Path trustedCertPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/trusted.crt").toURI());
        X509Certificate trustedX509Certificate = CertParsingUtils.readX509Certificates(Collections.singletonList(trustedCertPath))[0];
        Path trustedKeyPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/trusted.key").toURI());
        PrivateKey trustedKey = PemUtils.readPrivateKey(trustedKeyPath, ""::toCharArray);
        trustedCert = new CertificateInfo(trustedKey, trustedKeyPath, trustedX509Certificate, trustedCertPath);

        Path untrustedCertPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/untrusted.crt").toURI());
        X509Certificate untrustedX509Certificate = CertParsingUtils.readX509Certificates(Collections.singletonList(untrustedCertPath))[0];
        Path untrustedKeyPath = PathUtils.get(SSLTrustRestrictionsTests.class.getResource
                ("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/nodes/untrusted.key").toURI());
        PrivateKey untrustedKey = PemUtils.readPrivateKey(untrustedKeyPath, ""::toCharArray);
        untrustedCert = new CertificateInfo(untrustedKey, untrustedKeyPath, untrustedX509Certificate, untrustedCertPath);

        nodeSSL = Settings.builder()
                .put("xpack.security.transport.ssl.enabled", true)
                .put("xpack.security.transport.ssl.verification_mode", "certificate")
                .putList("xpack.ssl.certificate_authorities", ca.getCertPath().toString())
                .put("xpack.ssl.key", trustedCert.getKeyPath())
                .put("xpack.ssl.certificate", trustedCert.getCertPath())
                .build();
    }

    @AfterClass
    public static void cleanup() {
        configPath = null;
        nodeSSL = null;
        ca = null;
        trustedCert = null;
        untrustedCert = null;
    }

    @Override
    public Settings nodeSettings(int nodeOrdinal) {

        Settings parentSettings = super.nodeSettings(nodeOrdinal);
        Settings.Builder builder = Settings.builder()
                .put(parentSettings.filter((s) -> s.startsWith("xpack.ssl.") == false))
                .put(nodeSSL);

        restrictionsPath = configPath.resolve("trust_restrictions.yml");
        restrictionsTmpPath = configPath.resolve("trust_restrictions.tmp");

        writeRestrictions("*.trusted");
        builder.put("xpack.ssl.trust_restrictions.path", restrictionsPath);
        builder.put("resource.reload.interval.high", RESOURCE_RELOAD_MILLIS + "ms");

        return builder.build();
    }

    private void writeRestrictions(String trustedPattern) {
        try {
            Files.write(restrictionsTmpPath, Collections.singleton("trust.subject_name: \"" + trustedPattern + "\""));
            try {
                Files.move(restrictionsTmpPath, restrictionsPath, REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(restrictionsTmpPath, restrictionsPath, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ElasticsearchException("failed to write restrictions", e);
        }
    }

    @Override
    protected Settings transportClientSettings() {
        Settings parentSettings = super.transportClientSettings();
        Settings.Builder builder = Settings.builder()
                .put(parentSettings.filter((s) -> s.startsWith("xpack.ssl.") == false))
                .put(nodeSSL);
        return builder.build();
    }

    @Override
    protected boolean transportSSLEnabled() {
        return true;
    }

    public void testCertificateWithTrustedNameIsAccepted() throws Exception {
        writeRestrictions("*.trusted");
        try {
            tryConnect(trustedCert);
        } catch (SSLHandshakeException | SocketException ex) {
            logger.warn(new ParameterizedMessage("unexpected handshake failure with certificate [{}] [{}]",
                    trustedCert.certificate.getSubjectDN(), trustedCert.certificate.getSubjectAlternativeNames()), ex);
            fail("handshake should have been successful, but failed with " + ex);
        }
    }

    public void testCertificateWithUntrustedNameFails() throws Exception {
        writeRestrictions("*.trusted");
        try {
            tryConnect(untrustedCert);
            fail("handshake should have failed, but was successful");
        } catch (SSLHandshakeException | SocketException ex) {
            // expected
        }
    }

    public void testRestrictionsAreReloaded() throws Exception {
        writeRestrictions("*");
        assertBusy(() -> {
            try {
                tryConnect(untrustedCert);
            } catch (SSLHandshakeException | SocketException ex) {
                fail("handshake should have been successful, but failed with " + ex);
            }
        }, MAX_WAIT_RELOAD.millis(), TimeUnit.MILLISECONDS);

        writeRestrictions("*.trusted");
        assertBusy(() -> {
            try {
                tryConnect(untrustedCert);
                fail("handshake should have failed, but was successful");
            } catch (SSLHandshakeException | SocketException ex) {
                // expected
            }
        }, MAX_WAIT_RELOAD.millis(), TimeUnit.MILLISECONDS);
    }

    private void tryConnect(CertificateInfo certificate) throws Exception {
        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put("xpack.ssl.key", certificate.getKeyPath())
                .put("xpack.ssl.certificate", certificate.getCertPath())
                .putList("xpack.ssl.certificate_authorities", ca.getCertPath().toString())
                .put("xpack.ssl.verification_mode", "certificate")
                .build();

        String node = randomFrom(internalCluster().getNodeNames());
        SSLService sslService = new SSLService(settings, TestEnvironment.newEnvironment(settings));
        SSLSocketFactory sslSocketFactory = sslService.sslSocketFactory(settings);
        TransportAddress address = internalCluster().getInstance(Transport.class, node).boundAddress().publishAddress();
        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(address.getAddress(), address.getPort())) {
            assertThat(socket.isConnected(), is(true));
            // The test simply relies on this (synchronously) connecting (or not), so we don't need a handshake handler
            socket.startHandshake();
        }
    }

    private static class CertificateInfo {
        private final PrivateKey key;
        private final Path keyPath;
        private final X509Certificate certificate;
        private final Path certPath;

        private CertificateInfo(PrivateKey key, Path keyPath, X509Certificate certificate, Path certPath) {
            this.key = key;
            this.keyPath = keyPath;
            this.certificate = certificate;
            this.certPath = certPath;
        }

        private PrivateKey getKey() {
            return key;
        }

        private Path getKeyPath() {
            return keyPath;
        }

        private X509Certificate getCertificate() {
            return certificate;
        }

        private Path getCertPath() {
            return certPath;
        }
    }
}
