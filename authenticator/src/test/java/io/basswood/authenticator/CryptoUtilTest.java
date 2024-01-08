package io.basswood.authenticator;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.service.CryptoUtil;
import io.basswood.authenticator.service.KeySerializationSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptoUtilTest {
    @Test
    void signatureTestRSA() throws Exception {
        String message = "One quick brown fox jump over the lazy dog";
        RSAKey rsaKey = KeySerializationSupport.randomRSAKeyPair();
        RSAPrivateKey rsaPrivateKey = rsaKey.toRSAPrivateKey();
        RSAPublicKey rsaPublicKey = rsaKey.toRSAPublicKey();
        // sign
        Signature sig = Signature.getInstance("SHA256WithRSA");
        sig.initSign(rsaPrivateKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signature = sig.sign();

        // verification
        sig.initVerify(rsaPublicKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        assertTrue(sig.verify(signature));

        assertTrue(
                CryptoUtil.verifySignatureWithRSAKey(
                        CryptoUtil.signWithRSAKey(new ByteArray(message.getBytes(StandardCharsets.UTF_8)), rsaPrivateKey),
                        new ByteArray(message.getBytes(StandardCharsets.UTF_8)),
                        rsaPublicKey
                )
        );
    }


    @Test
    void signatureTestEC() throws Exception {
        String message = "One quick brown fox jump over the lazy dog";
        ECKey ecKey = KeySerializationSupport.randomECKeyPair();
        ECPrivateKey ecPrivateKey = ecKey.toECPrivateKey();
        ECPublicKey ecPublicKey = ecKey.toECPublicKey();
        // sign
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(ecPrivateKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signature = sig.sign();

        // verification
        sig.initVerify(ecPublicKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        assertTrue(sig.verify(signature));

        assertTrue(
                CryptoUtil.verifySignatureWithECKey(
                        CryptoUtil.signWithECKey(new ByteArray(message.getBytes(StandardCharsets.UTF_8)), ecPrivateKey),
                        new ByteArray(message.getBytes(StandardCharsets.UTF_8)),
                        ecPublicKey
                )
        );
    }
}