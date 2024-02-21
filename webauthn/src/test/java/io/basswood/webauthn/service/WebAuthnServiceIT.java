package io.basswood.webauthn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.basswood.authenticator.model.VirtualAuthenticator;
import io.basswood.webauthn.dto.RegistrationRequestDTO;
import io.basswood.webauthn.exception.BadRequest;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.credential.CredentialRepositoryImpl;
import io.basswood.webauthn.model.credential.RegisteredCredentialEntity;
import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.model.rp.RelyingPartyOrigin;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import io.basswood.webauthn.repository.BaseRepositoryIT;
import io.basswood.webauthn.repository.RegisteredCredentialEntityRepository;
import io.basswood.webauthn.repository.RelyingPartyOriginRepository;
import io.basswood.webauthn.repository.RelyingPartyRepository;
import io.basswood.webauthn.repository.UserRepository;
import io.basswood.webauthn.repository.UsernameRepository;
import io.basswood.webauthn.repository.WebAuthnRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public class WebAuthnServiceIT extends BaseRepositoryIT {
    private WebAuthnService webAuthnService;
    private VirtualAuthenticator authenticator;
    private RelyingPartyEntity relyingParty;
    private User user;
    @Autowired
    private RegisteredCredentialEntityRepository registeredCredentialEntityRepository;
    @Autowired
    private RelyingPartyRepository relyingPartyRepository;
    @Autowired
    private RelyingPartyOriginRepository relyingPartyOriginRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UsernameRepository usernameRepository;
    @Autowired
    private WebAuthnRequestRepository webAuthnRequestRepository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        RelyingPartyService relyingPartyService = new RelyingPartyService(relyingPartyRepository, relyingPartyOriginRepository);
        UserService userService = new UserService(userRepository, usernameRepository, secureRandom);
        CredentialRepositoryImpl credentialRepository = new CredentialRepositoryImpl(userService, registeredCredentialEntityRepository);
        WebAuthnRequestCache webAuthnRequestCache = new WebAuthnRequestCache(webAuthnRequestRepository);
        objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        webAuthnService = new WebAuthnService(relyingPartyService, userService, credentialRepository,
                registeredCredentialEntityRepository, webAuthnRequestCache);
        authenticator = VirtualAuthenticator.builder()
                .aaguid(UUID.randomUUID())
                .attachment(AuthenticatorAttachment.PLATFORM)
                .authenticatorTransport(AuthenticatorTransport.INTERNAL)
                .key(new NimbusJOSEHelper().createECKey(KeyUse.SIGNATURE, Curve.P_256, Duration.ofDays(1)))
                .build();

        relyingParty = RelyingPartyEntity.builder()
                .id("example.com")
                .name("Example")
                .allowOriginPort(true)
                .allowOriginSubdomain(true)
                .timeout(300L)
                .origins(
                        Set.of(
                                RelyingPartyOrigin.builder()
                                        .origin("example.com")
                                        .build(),
                                RelyingPartyOrigin.builder()
                                        .origin("a.example.com")
                                        .build(),
                                RelyingPartyOrigin.builder()
                                        .origin("b.example.com")
                                        .build()
                        )
                )
                .build();
        relyingParty.getOrigins().forEach(origin -> origin.setRelyingPartyEntity(relyingParty));
        user = User.builder()
                .userHandle(UUID.randomUUID().toString())
                .displayName("Homer Simpson")
                .usernames(Set.of(
                        Username.builder()
                                .username("homer.simpson@gmail.com")
                                .build()
                ))
                .build();
        user.getUsernames().forEach(username -> username.setUser(user));

        relyingParty = relyingPartyRepository.save(relyingParty);
        user = userRepository.save(user);
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        relyingPartyRepository.deleteAll();
        registeredCredentialEntityRepository.deleteAll();
        objectMapper = null;
        webAuthnService = null;
        authenticator = null;
        relyingParty = null;
        user = null;
    }

    @Test
    void testRegistrationAndAssertionFlow() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        // sever receives finish registration request
        webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse);

        // server receives assertion request
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(origin, loginHandle, username);
        // client receives assertion challenge
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = authenticator.get(publicKeyCredentialRequestOptions);
        String assertionFinishResponse = null;
        try {
            assertionFinishResponse = objectMapper.writeValueAsString(authenticatorAssertionResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        //server receives signed challenge response
        AssertionResult assertionResult = webAuthnService.finishAssertion(origin, loginHandle, assertionFinishResponse);
        Assertions.assertTrue(assertionResult.isSuccess());

        // Assert credential in db
        Set<RegisteredCredentialEntity> credentialEntities = registeredCredentialEntityRepository.findByUser(user);
        Assertions.assertFalse(credentialEntities.isEmpty());
        Assertions.assertEquals(1, credentialEntities.size());
        RegisteredCredentialEntity credentialEntity = credentialEntities.stream().findFirst().get();
        Assertions.assertEquals(1, credentialEntity.getSignatureCount());
        Assertions.assertEquals(1, credentialEntity.getTransports().size());
        Assertions.assertEquals("internal", credentialEntity.getTransports().stream().findFirst().get().getTransport());
    }

    @Test
    void testRegistrationAndAssertionFlow_FOR_NON_EXISTENT_USER() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = "merge.simpson@aol.com";
        String displayName = "Merge Simpson";
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(username)
                .displayName(displayName)
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        String userHandle = publicKeyCredentialCreationOptions.getUser().getId().getBase64Url();
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        // sever receives finish registration request
        webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse);

        // server receives assertion request
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(origin, loginHandle, username);
        // client receives assertion challenge
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = authenticator.get(publicKeyCredentialRequestOptions);
        String assertionFinishResponse = null;
        try {
            assertionFinishResponse = objectMapper.writeValueAsString(authenticatorAssertionResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        //server receives signed challenge response
        AssertionResult assertionResult = webAuthnService.finishAssertion(origin, loginHandle, assertionFinishResponse);
        Assertions.assertTrue(assertionResult.isSuccess());

        // Assert credential in db
        Set<RegisteredCredentialEntity> credentialEntities = registeredCredentialEntityRepository.findByUser(User.builder()
                .userHandle(userHandle)
                .build());
        Assertions.assertFalse(credentialEntities.isEmpty());
        Assertions.assertEquals(1, credentialEntities.size());
        RegisteredCredentialEntity credentialEntity = credentialEntities.stream().findFirst().get();
        Assertions.assertEquals(1, credentialEntity.getSignatureCount());
        Assertions.assertEquals(1, credentialEntity.getTransports().size());
        Assertions.assertEquals("internal", credentialEntity.getTransports().stream().findFirst().get().getTransport());
    }

    @Test
    void testFinishRegistration_BAD_publicKeyCredentialResponse() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        String badData = publicKeyCredentialResponse.replace("{", "(");
        // sever receives finish registration request with bad Payload.
        BadRequest badRequest = Assertions.assertThrows(BadRequest.class, () -> webAuthnService.finishRegistration(origin, registrationId, badData));
        Assertions.assertTrue(badRequest.getCause() instanceof IOException);
    }

    @Test
    void testFinishRegistration_BAD_registratinId() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        try {
            final String publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
            String wrongRegistrationId = UUID.randomUUID().toString();
            EntityNotFound entityNotFound = Assertions.assertThrows(EntityNotFound.class, () ->
                    webAuthnService.finishRegistration(origin, wrongRegistrationId, publicKeyCredentialResponse));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator create response");
        }
    }

    @Test
    void testFinishRegistration_RegistrationFailedException() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);

        String badAttestationData="o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViqo3mm9u6vuaVeN4wRgDTidR5oL6ufLTCrE9ISVYbOGUdFAAAAAIUCRwHXZ0dhlBEQw_YYeqkAJnNmndHd3_nPHd_uO9PfveHNvnm9ntd39Hn-umV4YW1wbGUuY29tpQECAyYgASFYIDY8Xl98ZKO-ZTB9P3YPqWQCzqimo3EQAlOQ5EVMQwABIlggVogsSUUSZWkfng_X2uj9YvFoLXoUt7MkUyPZEm7L3oc";
        String publicKeyCredentialResponse = badRegistrationData(publicKeyCredential.getId().getBase64Url(), badAttestationData);
        // sever receives finish registration request
        RootException rootException = Assertions.assertThrows(RootException.class, () -> webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse));
        Assertions.assertTrue(rootException.getCause() instanceof RegistrationFailedException);
    }

    private String badRegistrationData(String id, String attestationObject){
        String pattern = """
                {
                  "id": "%s",
                  "response": {
                    "attestationObject": "%s",
                    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiJVR2x2UTdGVWlCeWVZZEpPUURWeDMzZVo1ZFhpWTRSSjJYLVMtWUo1cmh3Iiwib3JpZ2luIjoiZXhhbXBsZS5jb20iLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0",
                    "transports": [
                      "internal"
                    ]
                  },
                  "authenticatorAttachment": null,
                  "clientExtensionResults": {
                    "appidExclude": null,
                    "credProps": null,
                    "largeBlob": null
                  },
                  "type": "public-key"
                }
                """;
        return String.format(pattern, id, attestationObject);
    }

    @Test
    void testFinishAssertion_Bad_publicKeyCredential() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        // sever receives finish registration request
        webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse);

        // server receives assertion request
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(origin, loginHandle, username);
        // client receives assertion challenge
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = authenticator.get(publicKeyCredentialRequestOptions);
        String assertionFinishResponse = null;
        try {
            assertionFinishResponse = objectMapper.writeValueAsString(authenticatorAssertionResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        String badFinishResponse = assertionFinishResponse.replace("{", "}");
        //server receives signed challenge response
        BadRequest badRequest = Assertions.assertThrows(BadRequest.class, () -> webAuthnService.finishAssertion(origin, loginHandle, badFinishResponse));
        Assertions.assertTrue(badRequest.getCause() instanceof IOException);
    }

    @Test
    void testFinishAssertion_Bad_loginHandle() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        // sever receives finish registration request
        webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse);

        // server receives assertion request
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(origin, loginHandle, username);
        // client receives assertion challenge
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = authenticator.get(publicKeyCredentialRequestOptions);
        final String assertionFinishResponse;
        try {
            assertionFinishResponse = objectMapper.writeValueAsString(authenticatorAssertionResponse);
            //server receives signed challenge response
            String badLoginHandle = UUID.randomUUID().toString();
            EntityNotFound entityNotFound = Assertions.assertThrows(EntityNotFound.class, () -> webAuthnService.finishAssertion(origin, badLoginHandle, assertionFinishResponse));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
    }

    @Test
    void testFinishAssertion_AssertionFailed() {
        String registrationId = UUID.randomUUID().toString();
        String origin = relyingParty.getOrigins().stream().findFirst().get().getOrigin();
        String username = user.getUsernames().stream().findFirst().get().getUsername();
        // Sever receives registration Request
        RegistrationRequestDTO registrationRequestDTO = RegistrationRequestDTO.builder()
                .username(user.getUsernames().stream().findFirst().get().getUsername())
                .build();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, origin, registrationRequestDTO);
        // client receives response from server and creates keys
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential = authenticator.create(publicKeyCredentialCreationOptions);
        String publicKeyCredentialResponse = null;
        try {
            publicKeyCredentialResponse = objectMapper.writeValueAsString(publicKeyCredential);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assertions.fail("Failed to serialize authenticator crate response");
        }
        // sever receives finish registration request
        webAuthnService.finishRegistration(origin, registrationId, publicKeyCredentialResponse);

        // server receives assertion request
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(origin, loginHandle, username);
        // client receives assertion challenge
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> authenticatorAssertionResponse = authenticator.get(publicKeyCredentialRequestOptions);
        String badAuthenticatorData = "o3mm9u6vuaVeN4wRgDTidR5oL6ufLTCrE9ISVYbOGUdFAAAAAd6vwNh170wBlHKwyjwkyMMAJnPHOPXO2vm33GvuOeXvvOfdft-OOtd_dHXeNGV4YW1wbGUuY29tpQECAyYgASFYIG7KHJUHpNyy3lKcYzMmQ7VuSEjJMhWfEDgQ95AThFqcIlggqyLt1PexoGzM7faDEE9HSxkvRYcBDcWrDWUX_nPAhsM";
        final String assertionFinishResponse = badAssertionData(authenticatorAssertionResponse.getId().getBase64Url(), badAuthenticatorData);
        RootException rootException = Assertions.assertThrows(RootException.class, () -> webAuthnService.finishAssertion(origin, loginHandle, assertionFinishResponse));
        Assertions.assertTrue(rootException.getCause() instanceof AssertionFailedException);
    }

    private String badAssertionData(String id, String authenticatorData) {
        String pattern = """
                {
                   "id": "%s",
                   "response": {
                     "authenticatorData": "%s",
                     "clientDataJSON": "eyJjaGFsbGVuZ2UiOiI5VTdVd1ZfbGVPRERIMVVlQXJrZVJjeWF6dHRHUkIydGh0S3lUNmFjbVN3Iiwib3JpZ2luIjoiZXhhbXBsZS5jb20iLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0",
                     "signature": "MEQCIDK3amn3Ox4sCBGln7KEEbeSQ_7y43GWo0zEuy5rbd3kAiBstSfto0IEcphX7YLmQJ8hmxDIwWzpHfnLSmwGf__M3Q",
                     "userHandle": "c8c49c7a-bfca-455e-8591-34461390dd40"
                   },
                   "authenticatorAttachment": null,
                   "clientExtensionResults": {
                     "appid": null,
                     "largeBlob": null
                   },
                   "type": "public-key"
                 }
                """;
        return String.format(pattern, id, authenticatorData);
    }
}