package io.basswood.webauthn.secret;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The Java program creates a keystore containing an AES secret key and another secret key containing the database password.
 *
 * Example Usage:
 * java ./src/main/java/io/basswood/identity/common/util/KeystoreUtil.java \ <br/>
 * --storeType=PKCS12 \<br/>
 * --storepass=basswood \<br/>
 * --aesKeyPassword=basswood \<br/>
 * --aesKeyAlias=basswoodkey \<br/>
 * --dbKeyPassword=basswood \<br/>
 * --dbKeyAlias=basswooddbpassword \<br/>
 * --dbpassword=basswood \<br/>
 * --keystore=basswood-keystore.p12 \<br/>
 * --keystoreConfig=basswood-keystore-config.json<br/>
 *
 * @author shamualr
 * @since 1.0
 */
public class KeystoreUtil {
    private static final String DEFAULT_KEY_ALGORITHM = "AES";
    private static final int DEFAULT_KEY_SIZE = 256;
    private static final String OPTION_NAME_STORE_TYPE = "--storeType";
    private static final String OPTION_NAME_STORE_PASS = "--storepass";
    private static final String OPTION_NAME_AES_KEY_PASS = "--aesKeyPassword";
    private static final String OPTION_NAME_AES_KEY_ALIAS = "--aesKeyAlias";
    private static final String OPTION_NAME_DB_KEY_PASS = "--dbKeyPassword";
    private static final String OPTION_NAME_DB_KEY_ALIAS = "--dbKeyAlias";
    private static final String OPTION_NAME_DB_PASS = "--dbpassword";
    private static final String OPTION_NAME_KEY_STORE = "--keystore";
    private static final String OPTION_NAME_KEY_STORE_CONFIG = "--keystoreConfig";
    private static final String OPTION_NAME_HELP = "--help";
    private static Set<String> OPTIONS = new LinkedHashSet<>(
            Arrays.asList(
                    OPTION_NAME_STORE_TYPE,
                    OPTION_NAME_STORE_PASS,
                    OPTION_NAME_AES_KEY_PASS,
                    OPTION_NAME_AES_KEY_ALIAS,
                    OPTION_NAME_DB_KEY_PASS,
                    OPTION_NAME_DB_KEY_ALIAS,
                    OPTION_NAME_DB_PASS,
                    OPTION_NAME_KEY_STORE,
                    OPTION_NAME_KEY_STORE_CONFIG,
                    OPTION_NAME_HELP
            )
    );
    private static Set<String> JSON_KEYS = new LinkedHashSet<>(
            Arrays.asList(
                    OPTION_NAME_STORE_TYPE,
                    OPTION_NAME_STORE_PASS,
                    OPTION_NAME_AES_KEY_PASS,
                    OPTION_NAME_AES_KEY_ALIAS,
                    OPTION_NAME_DB_KEY_PASS,
                    OPTION_NAME_DB_KEY_ALIAS
            )
    );
    public static final String JSON_KEY_VALUE = "\"%s\":\"%s\"";

    public static void main(String[] args) {
        Map<String, String> optionMap = processOptions(args);
        String storeType = optionMap.get(OPTION_NAME_STORE_TYPE);
        String storePass = optionMap.get(OPTION_NAME_STORE_PASS);
        // create keyStore
        KeyStore keyStore = null;
        try {
            keyStore = createKeyStore(storeType, storePass);
        } catch (Exception e) {
            System.err.println("Failed to create keystore");
            e.printStackTrace(System.err);
            System.exit(7);
        }
        // create SecretKey
        SecretKey secretKey = null;
        try {
            secretKey = generateSecretKey();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to create secret key");
            e.printStackTrace(System.err);
            System.exit(8);
        }
        // Save key in keyStore
        String aesKeyAlias = optionMap.get(OPTION_NAME_AES_KEY_ALIAS);
        String aesKeyPass = optionMap.get(OPTION_NAME_AES_KEY_PASS);
        try {
            keyStore.setKeyEntry(aesKeyAlias, secretKey, aesKeyPass.toCharArray(), null);
        } catch (KeyStoreException e) {
            System.err.println("Failed to store secret key");
            e.printStackTrace(System.err);
            System.exit(9);
        }

        // Save database password as key
        String dbKeyAlias = optionMap.get(OPTION_NAME_DB_KEY_ALIAS);
        String dbKeyPass = optionMap.get(OPTION_NAME_DB_KEY_PASS);
        String dbPass = optionMap.get(OPTION_NAME_DB_PASS);
        SecretKeySpec secretDBKey = new SecretKeySpec(dbPass.getBytes(StandardCharsets.UTF_8), DEFAULT_KEY_ALGORITHM);
        try {
            keyStore.setKeyEntry(dbKeyAlias, secretDBKey, dbKeyPass.toCharArray(), null);
        } catch (KeyStoreException e) {
            System.err.println("Failed to store db password as key");
            e.printStackTrace(System.err);
            System.exit(10);
        }
        // Save Keystore and KeystoreConfig
        saveKeystore(keyStore, storePass, optionMap);
        saveKeystoreConfigFile(optionMap);
    }

    private static Map<String, String> processOptions(String[] options) {
        int requiredNumberOfOptions = OPTIONS.size() - 1; // excluding --help
        if (options == null || (options.length < requiredNumberOfOptions)) {
            printUsage();
            System.exit(1);
        }
        Map<String, String> optionMap = new LinkedHashMap<>();
        for (String option : options) {
            if (OPTION_NAME_HELP.equals(option)) {
                printUsage();
                System.exit(2);
            }
            String[] split = option.split("=");
            if (split.length != 2) {
                System.err.println("Unsupported option " + option);
                printUsage();
                System.exit(3);
            }
            String optionName = split[0];
            String optionValue = split[1];
            if (!OPTIONS.contains(optionName)) {
                System.err.println("Unsupported option " + optionName);
                printUsage();
                System.exit(4);
            }

            if (optionValue == null || optionValue.trim().isEmpty()) {
                System.err.println("value for option " + optionName + " cannot be null or empty ");
                printUsage();
                System.exit(5);
            }
            optionMap.put(optionName, optionValue);
        }
        if (optionMap.size() != requiredNumberOfOptions) {
            System.err.println("All required options must be provided.");
            printUsage();
            System.exit(6);
        }

        File file = new File(optionMap.get(OPTION_NAME_KEY_STORE));
        if (file.exists()) {
            System.err.println("A keystore file with same name: " + optionMap.get(OPTION_NAME_KEY_STORE) + " already exists.");
            System.exit(7);
        }
        file = new File(optionMap.get(OPTION_NAME_KEY_STORE_CONFIG));
        if (file.exists()) {
            System.err.println("A keystore config file with same name: " + optionMap.get(OPTION_NAME_KEY_STORE_CONFIG) + " already exists.");
            System.exit(8);
        }
        return optionMap;
    }

    private static KeyStore createKeyStore(String keyStoreType, String storePassword) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keystore;
        keystore = KeyStore.getInstance(keyStoreType);
        keystore.load(null, storePassword.toCharArray());
        return keystore;
    }

    private static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keygen = KeyGenerator.getInstance(DEFAULT_KEY_ALGORITHM);
        keygen.init(DEFAULT_KEY_SIZE);
        SecretKey secretKey = keygen.generateKey();
        return secretKey;
    }

    private static void saveKeystore(KeyStore keyStore, String storePass, Map<String, String> optionMap) {
        File keyStoreFile = new File(optionMap.get(OPTION_NAME_KEY_STORE));
        try (FileOutputStream outputStream = new FileOutputStream(keyStoreFile)) {
            keyStore.store(outputStream, storePass.toCharArray());
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            System.err.println("Failed to save keystore file at:" + keyStoreFile);
            e.printStackTrace(System.err);
            System.exit(11);
        }

    }

    private static void saveKeystoreConfigFile(Map<String, String> optionMap) {
        StringBuilder jsonBuilder = new StringBuilder();
        JSON_KEYS.forEach(prop -> {
            String key = prop.substring(2);
            String value = optionMap.get(prop);
            jsonBuilder.append(",").append(String.format(JSON_KEY_VALUE, key, value));
        });
        String json = "{" + jsonBuilder.substring(1) + "}";
        File keyStoreConfigFile = new File(optionMap.get(OPTION_NAME_KEY_STORE_CONFIG));
        try (PrintWriter printWriter = new PrintWriter(new FileOutputStream(keyStoreConfigFile))) {
            printWriter.print(json);
            printWriter.flush();
        } catch (IOException e) {
            System.err.println("Failed to save keystore config file at:" + keyStoreConfigFile);
            e.printStackTrace(System.err);
            System.exit(12);
        }
    }

    private static void printUsage() {
        System.err.println(
                new StringBuilder()
                        .append("java KeystoreUtil.java \\").append("\n")
                        .append(OPTION_NAME_STORE_TYPE).append("=PKCS12 \\").append("\n")
                        .append(OPTION_NAME_STORE_PASS).append("=<storepass> \\").append("\n")
                        .append(OPTION_NAME_AES_KEY_PASS).append("=<aesKeyPassword> \\").append("\n")
                        .append(OPTION_NAME_AES_KEY_ALIAS).append("=<aesKeyAlias> \\").append("\n")
                        .append(OPTION_NAME_DB_KEY_PASS).append("=<dbKeyPassword> \\").append("\n")
                        .append(OPTION_NAME_DB_KEY_ALIAS).append("=<dbKeyAlias> \\").append("\n")
                        .append(OPTION_NAME_DB_PASS).append("=<dbpassword> \\").append("\n")
                        .append(OPTION_NAME_KEY_STORE).append("=<keyStoreFile> \\").append("\n")
                        .append(OPTION_NAME_KEY_STORE_CONFIG).append("=<keyStoreConfigFile.json> \\").append("\n")
                        .append(OPTION_NAME_HELP)
        );
    }
}
