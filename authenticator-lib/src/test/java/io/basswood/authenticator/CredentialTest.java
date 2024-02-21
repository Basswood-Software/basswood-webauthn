package io.basswood.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.model.Credential;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialTest {
    @Test
    void testRSACredentialSerialization() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Credential<RSAKey> rsaKeyCredential = new Credential<>(
                new ByteArray("123".getBytes(StandardCharsets.UTF_8)),
                new ByteArray("https;//amdocs.com".getBytes(StandardCharsets.UTF_8)),
                RSAKey.class
        );
        String serializedKey = mapper.writeValueAsString(rsaKeyCredential);
        Credential credential = mapper.readValue(serializedKey, Credential.class);
        assertEquals(rsaKeyCredential.getCredentialId(), credential.getCredentialId());
        assertEquals(rsaKeyCredential.getUserId(), credential.getUserId());
        assertEquals(rsaKeyCredential.getRpId(), credential.getRpId());
        assertEquals(rsaKeyCredential.getKey().getKeyType(), credential.getKey().getKeyType());
        assertEquals(rsaKeyCredential.getKey().getKeyUse(), credential.getKey().getKeyUse());
        assertEquals(rsaKeyCredential.getKey().getKeyID(), credential.getKey().getKeyID());
        assertEquals(rsaKeyCredential.getKey().getAlgorithm(), credential.getKey().getAlgorithm());
        assertEquals(rsaKeyCredential.getKey().getClass(), credential.getKey().getClass());
        assertTrue(rsaKeyCredential.getKey() instanceof RSAKey);
        assertTrue(credential.getKey() instanceof RSAKey);
        RSAKey expected = (RSAKey) rsaKeyCredential.getKey();
        RSAKey actual = (RSAKey) credential.getKey();
        assertEquals(expected.getModulus(), actual.getModulus());
        assertEquals(expected.getPublicExponent(), actual.getPublicExponent());
        assertEquals(expected.getPrivateExponent(), actual.getPrivateExponent());
    }

    @Test
    void testECCredentialSerialization() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Credential<ECKey> ecKeyCredential = new Credential<>(
                new ByteArray("123".getBytes(StandardCharsets.UTF_8)),
                new ByteArray("https;//amdocs.com".getBytes(StandardCharsets.UTF_8)),
                ECKey.class
        );
        String serializedKey = mapper.writeValueAsString(ecKeyCredential);
        Credential credential = mapper.readValue(serializedKey, Credential.class);
        assertEquals(ecKeyCredential.getCredentialId(), credential.getCredentialId());
        assertEquals(ecKeyCredential.getUserId(), credential.getUserId());
        assertEquals(ecKeyCredential.getRpId(), credential.getRpId());
        assertEquals(ecKeyCredential.getKey().getKeyType(), credential.getKey().getKeyType());
        assertEquals(ecKeyCredential.getKey().getKeyUse(), credential.getKey().getKeyUse());
        assertEquals(ecKeyCredential.getKey().getKeyID(), credential.getKey().getKeyID());
        assertEquals(ecKeyCredential.getKey().getAlgorithm(), credential.getKey().getAlgorithm());
        assertEquals(ecKeyCredential.getKey().getClass(), credential.getKey().getClass());
        assertTrue(ecKeyCredential.getKey() instanceof ECKey);
        assertTrue(credential.getKey() instanceof ECKey);
        ECKey expected = (ECKey) ecKeyCredential.getKey();
        ECKey actual = (ECKey) credential.getKey();
        assertEquals(expected.getCurve(), actual.getCurve());
        assertEquals(expected.getX(), actual.getX());
        assertEquals(expected.getY(), actual.getY());
    }
}