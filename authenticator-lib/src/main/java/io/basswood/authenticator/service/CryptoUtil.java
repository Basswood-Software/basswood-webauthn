package io.basswood.authenticator.service;

import com.google.common.hash.Hashing;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.exception.RootException;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class CryptoUtil {
    /**
     * Credit: Copied from com.yubico.webauthn.Crypto class.
     *
     * @param bytes
     * @return
     */
    public static ByteArray sha256(ByteArray bytes) {
        //noinspection UnstableApiUsage

        return new ByteArray(Hashing.sha256().hashBytes(bytes.getBytes()).asBytes());
    }

    /**
     * Credit: Copied from com.yubico.webauthn.Crypto class.
     *
     * @param str
     * @return
     */
    public static ByteArray sha256(String str) {
        return sha256(new ByteArray(str.getBytes(StandardCharsets.UTF_8)));
    }

    public static ByteArray signWithRSAKey(ByteArray message, RSAPrivateKey rsaPrivateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initSign(rsaPrivateKey);
            sig.update(message.getBytes());
            return new ByteArray(sig.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RootException(e);
        }
    }

    public static ByteArray signWithECKey(ByteArray message, ECPrivateKey ecPrivateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initSign(ecPrivateKey);
            sig.update(message.getBytes());
            return new ByteArray(sig.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RootException(e);
        }
    }

    public static <T extends JWK> ByteArray signWithKey(ByteArray message, JWK jwk) {
        try {
            if (jwk instanceof RSAKey) {
                return signWithRSAKey(message, jwk.toRSAKey().toRSAPrivateKey());
            } else if (jwk instanceof ECKey) {
                return signWithECKey(message, jwk.toECKey().toECPrivateKey());
            }
        } catch (JOSEException ex) {
            throw new RootException(ex);
        }
        throw new RootException("Key type:" + jwk.getClass().getName() + " not supported");
    }

    public static boolean verifySignatureWithRSAKey(ByteArray signature, ByteArray message, RSAPublicKey rsaPublicKey) {
        try {
            Signature sign = Signature.getInstance("SHA256WithRSA");
            sign.initVerify(rsaPublicKey);
            sign.update(message.getBytes());
            return sign.verify(signature.getBytes());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RootException(e);
        }
    }

    public static boolean verifySignatureWithECKey(ByteArray signature, ByteArray message, ECPublicKey ecPublicKey) {
        try {
            Signature sign = Signature.getInstance("SHA256withECDSA");
            sign.initVerify(ecPublicKey);
            sign.update(message.getBytes());
            return sign.verify(signature.getBytes());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RootException(e);
        }
    }
}
