package io.basswood.webauthn.secret;

import io.basswood.webauthn.exception.CryptoException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author shamualr
 * @since 1.0
 */
public interface AESEncryptor {
    int BUFFER_SIZE = 1024;

    default void encrypt(InputStream in, OutputStream out) {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
        byte[] data = new byte[BUFFER_SIZE];
        try (CipherOutputStream cout = new CipherOutputStream(out, cipher)) {
            int count = in.read(data);
            while (count > 0) {
                cout.write(data, 0, count);
                count = in.read(data);
            }
        } catch (IOException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    default void decrypt(InputStream in, OutputStream out) {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
        byte[] data = new byte[BUFFER_SIZE];
        try (CipherInputStream cin = new CipherInputStream(in, cipher)) {
            int count = cin.read(data);
            while (count > 0) {
                out.write(data, 0, count);
                count = cin.read(data);
            }
        } catch (IOException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    default byte[] encrypt(byte[] plainData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encrypt(new ByteArrayInputStream(plainData), outputStream);
        return outputStream.toByteArray();
    }

    default byte[] decrypt(byte[] cipherData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        decrypt(new ByteArrayInputStream(cipherData), outputStream);
        return outputStream.toByteArray();
    }

    default String transformation() {
        return String.format("AES/%s/%s", getFeedbackMode(), getPadding());
    }

    Cipher initCipher(int cipherMode);

    String getFeedbackMode();

    String getPadding();

    SecretKey getKey();

    byte[] getInitializationVector();

    byte[] getAssociatedData();

}
