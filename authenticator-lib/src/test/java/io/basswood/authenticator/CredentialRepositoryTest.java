package io.basswood.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.model.Credential;
import io.basswood.authenticator.model.CredentialRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class CredentialRepositoryTest {
    private ObjectMapper mapper;
    private CredentialRepository credentialRepository;

    private Credential<RSAKey> rsaKeyCredential;
    private Credential<ECKey> ecKeyCredential;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        credentialRepository = new CredentialRepository();
        rsaKeyCredential = new Credential<>(
                new ByteArray("123".getBytes(StandardCharsets.UTF_8)),
                new ByteArray("https://amdocs.com".getBytes(StandardCharsets.UTF_8)),
                RSAKey.class
        );
        ecKeyCredential = new Credential<>(
                new ByteArray("456".getBytes(StandardCharsets.UTF_8)),
                new ByteArray("https;//amdocs.com".getBytes(StandardCharsets.UTF_8)),
                ECKey.class
        );
        credentialRepository.add(rsaKeyCredential);
        credentialRepository.add(ecKeyCredential);
    }


    @Test
    void testSerialization() throws JsonProcessingException {
        String serializedRepo = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentialRepository);
        CredentialRepository actual = mapper.readValue(serializedRepo, CredentialRepository.class);
        Assertions.assertTrue(actual.findCredential(rsaKeyCredential.getCredentialId()).isPresent());
        Assertions.assertTrue(actual.findCredential(ecKeyCredential.getCredentialId()).isPresent());
    }
}