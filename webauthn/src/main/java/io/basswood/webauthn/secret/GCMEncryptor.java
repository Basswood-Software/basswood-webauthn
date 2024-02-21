package io.basswood.webauthn.secret;

import io.basswood.webauthn.exception.CryptoException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author shamualr
 * @since 1.0
 */
public class GCMEncryptor implements AESEncryptor {
    public static final int GCM_IV_LENGTH = 12;
    public static final int DEFAULT_TAG_LENGTH = 128;

    private SecretKey key;
    private byte[] initializationVector;
    private byte[] associatedData;
    private int tagLength;
    private SecureRandom secureRandom;

    public GCMEncryptor(SecretKey key, byte[] associatedData) {
        this(key, new SecureRandom(), associatedData, DEFAULT_TAG_LENGTH);
    }

    public GCMEncryptor(SecretKey key, SecureRandom secureRandom, byte[] associatedData) {
        this(key, secureRandom, associatedData, DEFAULT_TAG_LENGTH);
    }

    public GCMEncryptor(SecretKey key, SecureRandom secureRandom, byte[] associatedData, int tagLength) {
        this.key = key;
        this.secureRandom = secureRandom;
        this.associatedData = associatedData;
        this.initializationVector = new byte[GCM_IV_LENGTH];
        this.tagLength = tagLength;
        secureRandom.nextBytes(initializationVector);
    }

    @Override
    public void encrypt(InputStream in, OutputStream out) {
        try {
            byte[] ciphered = encrypt(in.readAllBytes());
            out.write(ciphered);
        } catch (IOException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public void decrypt(InputStream in, OutputStream out) {
        try {
            byte[] plain = decrypt(in.readAllBytes());
            out.write(plain);
        } catch (IOException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] encrypt(byte[] plainData) {
        byte[] iv = getInitializationVector();
        try {
            Cipher cipher = Cipher.getInstance(transformation());
            GCMParameterSpec parameterSpec = new GCMParameterSpec(getTagLength(), iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            byte[] cipherText = cipher.doFinal(plainData);
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            return byteBuffer.array();
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                 NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] decrypt(byte[] cipherData) {
        try {
            Cipher cipher = Cipher.getInstance(transformation());
            GCMParameterSpec gcmIv = new GCMParameterSpec(getTagLength(), cipherData, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmIv);
            if (associatedData != null) {
                cipher.updateAAD(associatedData);
            }
            byte[] plainText = cipher.doFinal(cipherData, GCM_IV_LENGTH, cipherData.length - GCM_IV_LENGTH);
            return plainText;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }


    @Override
    public Cipher initCipher(int cipherMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFeedbackMode() {
        return "GCM";
    }

    @Override
    public String getPadding() {
        return "NoPadding";
    }

    @Override
    public SecretKey getKey() {
        return key;
    }

    @Override
    public byte[] getInitializationVector() {
        return initializationVector;
    }

    @Override
    public byte[] getAssociatedData() {
        return associatedData;
    }

    public int getTagLength() {
        return tagLength;
    }

    public void setTagLength(int tagLength) {
        this.tagLength = tagLength;
    }
}
