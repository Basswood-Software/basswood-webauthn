package io.basswood.webauthn.secret;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

class SecretManagerTest {
    private static final String keyStoreResource = "/secrets/basswood-not-for-production-keystore.p12";
    private static final String keyStoreConfigResource = "/secrets/keystore-config.json";

    private SecretManager keystoreService;
    private String expectedDBPassword;

    @BeforeEach
    void setup() throws IOException {
        InputStream keyStoreInput = SecretManagerTest.class.getResourceAsStream(keyStoreResource);
        InputStream keyStoreConfigInput = SecretManagerTest.class.getResourceAsStream(keyStoreConfigResource);
        ObjectMapper objectMapper = new ObjectMapper();
        KeystoreConfig keystoreConfig = objectMapper.readValue(keyStoreConfigInput, KeystoreConfig.class);
        keystoreService = SecretManager.build(keyStoreInput, keystoreConfig);
        expectedDBPassword = "basswood";
    }

    @Test
    void testKeyStoreService() {
        Assertions.assertEquals(expectedDBPassword, keystoreService.getDatabasePassword());
        Assertions.assertNotNull(keystoreService.getAesKey());
    }
}