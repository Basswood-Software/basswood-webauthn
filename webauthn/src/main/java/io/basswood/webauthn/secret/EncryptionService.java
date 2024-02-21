package io.basswood.webauthn.secret;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author shamualr
 * @since 1.0
 */
public class EncryptionService {
    private static EncryptionService INSTANCE;
    private GCMEncryptor gcmEncryptor;

    public EncryptionService(SecretKey key) {
        this.gcmEncryptor = new GCMEncryptor(key, null);
    }

    public String encrypt(String plain) {
        byte[] cipher = gcmEncryptor.encrypt(plain.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipher);
    }

    public String decrypt(String cipherBase64) {
        byte[] cipher = Base64.getDecoder().decode(cipherBase64);
        byte[] plain = gcmEncryptor.decrypt(cipher);
        return new String(plain, StandardCharsets.UTF_8);
    }

    public static EncryptionService build(SecretKey key){
        if(INSTANCE == null){
            INSTANCE = new EncryptionService(key);
        }
        return INSTANCE;
    }

    public static EncryptionService getInstance(){
        return INSTANCE;
    }
}
