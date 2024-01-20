package io.basswood.webauthn.secret;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

class EncryptionServiceTest {
    private KeyStore keyStore;
    private SecretKey aesKey;
    private String keyStorePassword;
    private String aesKeyAlias;
    private String aesKeyPassword;
    private String dbKeyAlias;
    private String dbPassword;
    private String dbKeyPassword;
    private EncryptionService encryptionService;
    private SecretManager keystoreService;
    private String originalPlainData;

    @BeforeEach
    void setup() throws Exception {
        originalPlainData = "tT23%5_H\"XJ9YWfb";
        keyStorePassword = "one ring to rule them all";
        aesKeyPassword = "azh nagh krimpatul";
        aesKeyAlias = "oidcms";
        dbKeyAlias = "basswooddbpassword";
        dbKeyPassword = "azh nagh krimpatul";
        dbPassword = "basswoodmasterdbpassword";

        KeystoreConfig keystoreConfig = new KeystoreConfig(
                "PKCS12",
                keyStorePassword,
                aesKeyPassword,
                aesKeyAlias,
                dbKeyPassword,
                dbKeyAlias);

        keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, keyStorePassword.toCharArray());
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        aesKey = keygen.generateKey();
        keyStore.setKeyEntry(aesKeyAlias, aesKey, aesKeyPassword.toCharArray(), null);

        SecretKeySpec secretDBKey = new SecretKeySpec(dbPassword.getBytes(StandardCharsets.UTF_8), "AES");
        keyStore.setKeyEntry(dbKeyAlias, secretDBKey, dbKeyPassword.toCharArray(), null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyStore.store(baos, keyStorePassword.toCharArray());
        ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
        keystoreService = SecretManager.build(inputStream, keystoreConfig);
        encryptionService = new EncryptionService(keystoreService.getAesKey());
    }

    @Test
    void testKeystoreService() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        Assertions.assertEquals(dbPassword, keystoreService.getDatabasePassword());
        Assertions.assertEquals(aesKey, keystoreService.getAesKey());
    }

    @Test
    void testEncryptDecrypt() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        String cipherBase64 = encryptionService.encrypt(originalPlainData);
        String actualPlainData = encryptionService.decrypt(cipherBase64);
        Assertions.assertEquals(originalPlainData, actualPlainData);

        Key key = keyStore.getKey(aesKeyAlias, aesKeyPassword.toCharArray());
        Assertions.assertTrue(key instanceof SecretKey);
        Assertions.assertTrue(SecretKey.class.isAssignableFrom(key.getClass()));
    }
}