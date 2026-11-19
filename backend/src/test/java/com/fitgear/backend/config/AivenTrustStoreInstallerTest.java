package com.fitgear.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Aiven CA -> JVM truststore conversion that Render's plaintext
 * secret files require (PEM in, PKCS12 out).
 */
class AivenTrustStoreInstallerTest {

    private static final String TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    private static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    private String originalStore;
    private String originalType;
    private String originalPassword;

    @BeforeEach
    void saveProperties() {
        originalStore = System.getProperty(TRUST_STORE);
        originalType = System.getProperty(TRUST_STORE_TYPE);
        originalPassword = System.getProperty(TRUST_STORE_PASSWORD);
        System.clearProperty(TRUST_STORE);
        System.clearProperty(TRUST_STORE_TYPE);
        System.clearProperty(TRUST_STORE_PASSWORD);
    }

    @AfterEach
    void restoreProperties() throws Exception {
        String generated = System.getProperty(TRUST_STORE);
        if (generated != null && generated.contains("fitgear-truststore-")) {
            Files.deleteIfExists(Path.of(generated));
        }
        restore(TRUST_STORE, originalStore);
        restore(TRUST_STORE_TYPE, originalType);
        restore(TRUST_STORE_PASSWORD, originalPassword);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void missingPemIsNoOp() {
        AivenTrustStoreInstaller.installIfPresent(Path.of("definitely-not-here.pem"));
        assertNull(System.getProperty(TRUST_STORE));
    }

    @Test
    void validPemInstallsMergedTruststore() throws Exception {
        Path pem = Path.of(getClass().getResource("/aiven-truststore-test-ca.pem").toURI());
        AivenTrustStoreInstaller.installIfPresent(pem);

        String storePath = System.getProperty(TRUST_STORE);
        assertNotNull(storePath, "trustStore system property must be set");
        assertEquals("PKCS12", System.getProperty(TRUST_STORE_TYPE));
        assertNotNull(System.getProperty(TRUST_STORE_PASSWORD));

        try (InputStream in = Files.newInputStream(Path.of(storePath))) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, System.getProperty(TRUST_STORE_PASSWORD).toCharArray());

            assertTrue(Collections.list(store.aliases()).contains("aiven-ca-1"),
                    "Aiven CA must be present in the generated truststore");
            // The JDK's public trust anchors must survive so Razorpay's HTTPS
            // (api.razorpay.com) keeps verifying.
            assertTrue(Collections.list(store.aliases()).size() > 1,
                    "JVM cacerts anchors must be merged into the truststore");
        }
    }

    @Test
    void preexistingTrustStorePropertyIsRespected() throws Exception {
        System.setProperty(TRUST_STORE, "/somewhere/else.p12");
        Path pem = Path.of(getClass().getResource("/aiven-truststore-test-ca.pem").toURI());
        AivenTrustStoreInstaller.installIfPresent(pem);
        assertEquals("/somewhere/else.p12", System.getProperty(TRUST_STORE));
    }

    @Test
    void malformedPemFailsClosed() throws Exception {
        Path broken = Files.createTempFile("broken-ca-", ".pem");
        try {
            Files.writeString(broken, "this is not a certificate");
            assertThrows(IllegalStateException.class, () -> AivenTrustStoreInstaller.installIfPresent(broken));
        } finally {
            Files.deleteIfExists(broken);
        }
    }
}
