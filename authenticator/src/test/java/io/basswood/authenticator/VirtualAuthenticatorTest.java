package io.basswood.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.yubico.internal.util.JacksonCodecs;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import io.basswood.authenticator.model.Credential;
import io.basswood.authenticator.model.VirtualAuthenticator;
import io.basswood.authenticator.service.KeySerializationSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.yubico.webauthn.data.AuthenticatorAttachment.PLATFORM;
import static com.yubico.webauthn.data.AuthenticatorTransport.INTERNAL;

class VirtualAuthenticatorTest {
    private VirtualAuthenticator virtualAuthenticator;
    private PublicKeyCredentialCreationOptions options;

    private ObjectMapper objectMapper;
    private Credential<RSAKey> rsaKeyCredential;
    private Credential<ECKey> ecKeyCredential;


    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        virtualAuthenticator = VirtualAuthenticator.builder()
                .aaguid(UUID.randomUUID())
                .attachment(PLATFORM)
                .authenticatorTransport(INTERNAL)
                .key(KeySerializationSupport.randomRSAKeyPair())
                .supportedAlgorithms(new LinkedHashSet<>(Arrays.asList(COSEAlgorithmIdentifier.ES256)))
                .build();

        options = PublicKeyCredentialCreationOptions.builder()
                .rp(RelyingPartyIdentity.builder()
                        .id("https://amdocs.com")
                        .name("Amdocs").build())
                .user(UserIdentity.builder()
                        .name("homer@aol.com")
                        .displayName("Homer Simpson")
                        .id(new ByteArray("123456789".getBytes(StandardCharsets.UTF_8)))
                        .build())
                .challenge(new ByteArray("oneringtorulethemall".getBytes(StandardCharsets.UTF_8)))
                .pubKeyCredParams(Arrays.asList(PublicKeyCredentialParameters.RS256, PublicKeyCredentialParameters.ES256))
                .timeout(60000L)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .authenticatorAttachment(Optional.of(PLATFORM))
                        .userVerification(UserVerificationRequirement.DISCOURAGED)
                        .build())
                .build();
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
    }

    //@Test
    void testCreate() throws IOException {
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = virtualAuthenticator.create(options);
        ObjectMapper mapper = JacksonCodecs.json();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(publicKeyCredential);
        System.out.println("---------");
        System.out.println(json);
        System.out.println("---------");
    }

    //@Test
    void testGet() throws IOException {
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = virtualAuthenticator.create(options);
        PublicKeyCredentialRequestOptions requestOptions = PublicKeyCredentialRequestOptions.builder()
                .challenge(new ByteArray("helloworld".getBytes(StandardCharsets.UTF_8)))
                .allowCredentials(
                        Arrays.asList(PublicKeyCredentialDescriptor.builder()
                                .id(publicKeyCredential.getId())
                                .type(PublicKeyCredentialType.PUBLIC_KEY)
                                .transports(new LinkedHashSet<>(Arrays.asList(INTERNAL)))
                                .build()
                        ))
                .rpId(options.getRp().getId())
                .timeout(60000)
                .userVerification(UserVerificationRequirement.REQUIRED)
                .build();

        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = virtualAuthenticator.get(requestOptions);
        ObjectMapper mapper = JacksonCodecs.json();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(authenticatorAssertionResponse);
        System.out.println("---------");
        System.out.println(json);
        System.out.println("---------");
    }

    @Test
    void testSerialization() throws JsonProcessingException {
        virtualAuthenticator.getRepository().add(rsaKeyCredential);
        virtualAuthenticator.getRepository().add(ecKeyCredential);
        String vaString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(virtualAuthenticator);
        VirtualAuthenticator actual = objectMapper.readValue(vaString, VirtualAuthenticator.class);
        Assertions.assertEquals(virtualAuthenticator.getAaguid(), actual.getAaguid());
        Assertions.assertEquals(virtualAuthenticator.getSignatureCount(), actual.getSignatureCount());
        Assertions.assertEquals(virtualAuthenticator.getAttachment(), actual.getAttachment());
        Assertions.assertEquals(virtualAuthenticator.getAuthenticatorTransport(), actual.getAuthenticatorTransport());
        Assertions.assertEquals(virtualAuthenticator.supportedAlgorithms().size(), actual.supportedAlgorithms().size());
        for (COSEAlgorithmIdentifier algorithm : virtualAuthenticator.supportedAlgorithms()) {
            Assertions.assertTrue(actual.supportedAlgorithms().contains(algorithm));
        }
        Assertions.assertTrue(virtualAuthenticator.getKey().equals(actual.getKey()));
        Assertions.assertEquals(virtualAuthenticator.getRepository().getCredentialIds().size(), actual.getRepository().getCredentialIds().size());
        Set<ByteArray> credentialIds = virtualAuthenticator.getRepository().getCredentialIds();
        for (ByteArray credentialId : credentialIds) {
            Assertions.assertTrue(actual.getRepository().findCredential(credentialId).isPresent());
            Credential expectedCredential = virtualAuthenticator.getRepository().findCredential(credentialId).get();
            Credential actualCredential = actual.getRepository().findCredential(credentialId).get();
            Assertions.assertEquals(expectedCredential.getUserId(), actualCredential.getUserId());
            Assertions.assertEquals(expectedCredential.getRpId(), actualCredential.getRpId());
            Assertions.assertEquals(expectedCredential.getCredentialId(), actualCredential.getCredentialId());
            Assertions.assertEquals(expectedCredential.getKey(), actualCredential.getKey());
        }
    }
}