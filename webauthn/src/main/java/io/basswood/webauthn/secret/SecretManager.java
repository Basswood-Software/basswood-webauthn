package io.basswood.webauthn.secret;


import io.basswood.webauthn.exception.KeystoreException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * @author shamualr
 * @since 1.0
 */
public class SecretManager {

    private KeyStore keyStore;
    private KeystoreConfig keystoreConfig;

    public SecretManager(KeyStore keyStore, KeystoreConfig keystoreConfig) {
        this.keyStore = keyStore;
        this.keystoreConfig = keystoreConfig;
    }

    public SecretKey getAesKey() {
        return getSecretKey(keystoreConfig.aesKeyAlias(), keystoreConfig.aesKeyPassword().toCharArray());
    }

    public String getDatabasePassword() {
        return getSecretKeyContentAsString(keystoreConfig.dbKeyAlias(), keystoreConfig.dbKeyPassword().toCharArray());
    }

    public SecretKey getSecretKey(String keyAlias, char[] keyPassword) {
        try {
            Key key = keyStore.getKey(keyAlias, keyPassword);
            if(key == null){
                throw new KeystoreException("The key with alias:" + keyAlias + " no found in the keystore");
            }
            if (!SecretKey.class.isAssignableFrom(key.getClass())) {
                throw new KeystoreException("The key with alias:" + keyAlias + " is not a secret key");
            }
            return (SecretKey) key;
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new KeystoreException("Failed to retrieve AES secret key", e);
        }
    }

    public String getSecretKeyContentAsString(String keyAlias, char[] keyPassword) {
        try {
            Key key = keyStore.getKey(keyAlias, keyPassword);
            if(key == null){
                throw new KeystoreException("The key with alias:" + keyAlias + " no found in the keystore");
            }
            if (!SecretKey.class.isAssignableFrom(key.getClass())) {
                throw new KeystoreException("The key with alias:" + keyAlias + " is not a secret key");
            }
            return new String(key.getEncoded(), StandardCharsets.UTF_8);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new KeystoreException("Failed to retrieve secret key content as password", e);
        }
    }


    public static SecretManager build(InputStream keyStoreStream, KeystoreConfig keystoreConfig) {
        try {
            KeyStore keyStore = KeyStore.getInstance(keystoreConfig.storeType());
            keyStore.load(keyStoreStream, keystoreConfig.storepass().toCharArray());
            SecretManager secretManager = new SecretManager(keyStore, keystoreConfig);
            //validate
            Key key = secretManager.getAesKey();
            key.getClass().isAssignableFrom(SecretKey.class);
            if (!SecretKey.class.isAssignableFrom(key.getClass())) {
                throw new KeystoreException("The key with alias:" + keystoreConfig.aesKeyAlias() + " is not a secret key");
            }
            String databasePassword = secretManager.getDatabasePassword();
            return secretManager;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeystoreException("Failed to build KeystoreService instance", e);
        }
    }
}
