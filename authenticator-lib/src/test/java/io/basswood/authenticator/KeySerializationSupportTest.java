package io.basswood.authenticator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.upokecenter.cbor.CBORObject;
import io.basswood.authenticator.service.KeySerializationSupport;
import org.junit.jupiter.api.Test;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeySerializationSupportTest {
    @Test
    void testRSAKeySerialization() throws JOSEException {
        RSAKey rsaKey = KeySerializationSupport.randomRSAKeyPair();
        RSAPublicKey expected = rsaKey.toRSAPublicKey();
        CBORObject cose = KeySerializationSupport.toRSAPublicKeyCOSE(rsaKey.toPublicJWK());
        RSAPublicKey actual = KeySerializationSupport.fromRSACOSE(cose);
        assertEquals(expected.getModulus(), actual.getModulus());
        assertEquals(expected.getPublicExponent(), actual.getPublicExponent());
        assertEquals(expected.getFormat(), actual.getFormat());
        assertEquals(expected.getAlgorithm(), actual.getAlgorithm());
        assertArrayEquals(expected.getEncoded(), actual.getEncoded());
    }

    @Test
    void testECKeySerialization() throws JOSEException {
        ECKey ecKey = KeySerializationSupport.randomECKeyPair();
        ECPublicKey expected = ecKey.toECPublicKey();
        CBORObject cose = KeySerializationSupport.toECPublicKeyCOSE(ecKey.toPublicJWK());
        ECPublicKey actual = KeySerializationSupport.fromECCOSE(cose);
        assertEquals(expected.getW().getAffineX(), actual.getW().getAffineX());
        assertEquals(expected.getW().getAffineY(), actual.getW().getAffineY());
        assertEquals(expected.getFormat(), actual.getFormat());
        assertEquals(expected.getAlgorithm(), actual.getAlgorithm());
        assertArrayEquals(expected.getEncoded(), actual.getEncoded());
    }
}