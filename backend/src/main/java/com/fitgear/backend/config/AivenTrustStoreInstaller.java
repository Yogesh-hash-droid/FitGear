package com.fitgear.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collections;
import java.util.List;

/**
 * Installs the Aiven project CA certificate as the JVM default truststore so
 * that MySQL connections can run with {@code sslMode=VERIFY_CA} (the server
 * certificate is verified against the Aiven CA - no verification bypasses).
 *
 * <p>Why this exists: Render secret files are <b>plaintext</b>, so a binary
 * PKCS12 truststore cannot be uploaded to the dashboard. Instead the operator
 * pastes the <b>PEM</b> downloaded from the Aiven console (text) as a secret
 * file, and this class converts it at startup.</p>
 *
 * <p>Safety details:</p>
 * <ul>
 *   <li>The Aiven CA is <b>merged into a copy of the JVM's own {@code cacerts}</b>,
 *       so existing outbound TLS (e.g. the Razorpay API) keeps working.</li>
 *   <li>Multi-certificate PEM bundles are supported (Aiven ships both current
 *       and new CA certificates during a CA rotation).</li>
 *   <li>The generated PKCS12 uses a random per-boot password that only lives in
 *       memory + a JVM system property; it is never logged or committed.</li>
 *   <li>If the PEM file exists but cannot be parsed, the application refuses to
 *       start (fail closed) rather than connecting without verification.</li>
 * </ul>
 *
 * <p>Configuration: set {@code AIVEN_CA_PEM_PATH} to override the default
 * secret-file location {@code /etc/secrets/aiven-ca.pem}. When the file is
 * absent (local development) this is a no-op.</p>
 */
public final class AivenTrustStoreInstaller {

    private static final Logger log = LoggerFactory.getLogger(AivenTrustStoreInstaller.class);

    public static final String DEFAULT_PEM_PATH = "/etc/secrets/aiven-ca.pem";
    static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    static final String TRUST_STORE_TYPE_PROPERTY = "javax.net.ssl.trustStoreType";
    static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String CACERTS_PASSWORD = "changeit";
    private static final SecureRandom RANDOM = new SecureRandom();

    private AivenTrustStoreInstaller() {
    }

    /** Entry point invoked from {@code FitGearApplication#main}. */
    public static void installIfConfigured() {
        String configured = System.getenv("AIVEN_CA_PEM_PATH");
        Path pem = Path.of(configured == null || configured.isBlank() ? DEFAULT_PEM_PATH : configured);
        installIfPresent(pem);
    }

    /** Visible for testing: installs the truststore when the PEM file exists. */
    static void installIfPresent(Path pem) {
        if (System.getProperty(TRUST_STORE_PROPERTY) != null) {
            log.info("javax.net.ssl.trustStore already set - skipping Aiven CA installer");
            return;
        }
        if (!Files.isRegularFile(pem)) {
            log.info("No Aiven CA certificate at {} - using JVM default truststore", pem);
            return;
        }
        try {
            List<Certificate> aivenCerts = readPem(pem);
            if (aivenCerts.isEmpty()) {
                throw new IllegalStateException("No X.509 certificates found in " + pem);
            }

            KeyStore merged = KeyStore.getInstance("PKCS12");
            merged.load(null, null);

            // Start from the JVM's own trust anchors so Razorpay (and any other
            // outbound HTTPS) keeps verifying against the public PKI.
            int anchorCount = copyJdkTrustAnchors(merged);

            for (int i = 0; i < aivenCerts.size(); i++) {
                merged.setCertificateEntry("aiven-ca-" + (i + 1), aivenCerts.get(i));
            }

            char[] password = randomPassword();
            Path store = Files.createTempFile("fitgear-truststore-", ".p12");
            try (var out = Files.newOutputStream(store)) {
                merged.store(out, password);
            }
            store.toFile().deleteOnExit();

            System.setProperty(TRUST_STORE_PROPERTY, store.toAbsolutePath().toString());
            System.setProperty(TRUST_STORE_TYPE_PROPERTY, "PKCS12");
            System.setProperty(TRUST_STORE_PASSWORD_PROPERTY, new String(password));

            log.info("Installed Aiven CA ({} certificate(s)) merged with {} JVM trust anchor(s) as the default truststore",
                    aivenCerts.size(), anchorCount);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not install the Aiven CA truststore from " + pem
                            + " - refusing to start without certificate verification", e);
        }
    }

    private static List<Certificate> readPem(Path pem) throws Exception {
        try (InputStream in = Files.newInputStream(pem)) {
            // CertificateFactory understands PEM (and tolerates multi-cert bundles).
            return List.copyOf(CertificateFactory.getInstance("X.509").generateCertificates(in));
        }
    }

    private static int copyJdkTrustAnchors(KeyStore target) throws Exception {
        Path cacerts = Path.of(System.getProperty("java.home"), "lib", "security", "cacerts");
        if (!Files.isRegularFile(cacerts)) {
            throw new IllegalStateException("JVM cacerts not found at " + cacerts);
        }
        KeyStore source = null;
        Exception lastFailure = null;
        for (String type : new String[]{KeyStore.getDefaultType(), "PKCS12", "JKS"}) {
            try {
                KeyStore candidate = KeyStore.getInstance(type);
                try (InputStream in = Files.newInputStream(cacerts)) {
                    candidate.load(in, CACERTS_PASSWORD.toCharArray());
                }
                source = candidate;
                break;
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        if (source == null) {
            throw new IllegalStateException("Could not read JVM cacerts at " + cacerts, lastFailure);
        }

        int copied = 0;
        for (String alias : Collections.list(source.aliases())) {
            if (source.isCertificateEntry(alias)) {
                target.setCertificateEntry(alias, source.getCertificate(alias));
                copied++;
            }
        }
        if (copied == 0) {
            throw new IllegalStateException("JVM cacerts at " + cacerts + " contains no certificate entries");
        }
        return copied;
    }

    private static char[] randomPassword() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString().toCharArray();
    }
}
