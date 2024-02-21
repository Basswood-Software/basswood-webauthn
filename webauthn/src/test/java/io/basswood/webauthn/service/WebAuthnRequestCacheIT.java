package io.basswood.webauthn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.request.RequestType;
import io.basswood.webauthn.model.request.WebAuthnRequestEntity;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.repository.BaseRepositoryIT;
import io.basswood.webauthn.repository.WebAuthnRequestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class WebAuthnRequestCacheIT extends BaseRepositoryIT {
    @Autowired
    private WebAuthnRequestRepository repository;

    private WebAuthnRequestCache webAuthnRequestCache;
    private String createRequestJSON;
    private String assertionRequestJSON;
    PublicKeyCredentialCreationOptions originalCreateRequest;
    AssertionRequest originalAssertionRequest;
    private String requestId;
    private String loginHandle;
    private WebAuthnRequestEntity registrationRequestEntity;
    private WebAuthnRequestEntity assertionRequestEntity;

    @BeforeEach
    void setup() throws JsonProcessingException {
        webAuthnRequestCache = new WebAuthnRequestCache(repository);
        createRequestJSON = """
                {
                  "rp": {
                    "name": "Example",
                    "id": "example.com"
                  },
                  "user": {
                    "name": "homer.simpson@gmail.com",
                    "displayName": "Homer Simpson",
                    "id": "a951d907-aa0b-461e-8cdf-96559b358a9b"
                  },
                  "challenge": "Tw5GFfndG3k2XPky9dx3iFytYJo0oSzA-Z-F2FEx5NQ",
                  "pubKeyCredParams": [
                    {
                      "alg": -7,
                      "type": "public-key"
                    },
                    {
                      "alg": -8,
                      "type": "public-key"
                    },
                    {
                      "alg": -35,
                      "type": "public-key"
                    },
                    {
                      "alg": -36,
                      "type": "public-key"
                    },
                    {
                      "alg": -257,
                      "type": "public-key"
                    },
                    {
                      "alg": -258,
                      "type": "public-key"
                    },
                    {
                      "alg": -259,
                      "type": "public-key"
                    }
                  ],
                  "timeout": 300,
                  "excludeCredentials": [],
                  "authenticatorSelection": {
                    "authenticatorAttachment": "cross-platform",
                    "requireResidentKey": null,
                    "residentKey": null,
                    "userVerification": null
                  },
                  "attestation": "none",
                  "extensions": {
                    "appidExclude": null,
                    "credProps": true,
                    "largeBlob": null,
                    "uvm": null
                  }
                }                    
                                """;
        assertionRequestJSON = """
                {
                  "publicKeyCredentialRequestOptions": {
                    "challenge": "xzBTOExJL-JGQovaqpKn4e66bMYLhrJ1004-zM3qaxk",
                    "timeout": 300,
                    "rpId": "example.com",
                    "allowCredentials": [
                      {
                        "type": "public-key",
                        "id": "a951d907-aa0b-461e-8cdf-96559b358a9bZXhhbXBsZS5jb20",
                        "transports": [
                          "internal"
                        ]
                      }
                    ],
                    "userVerification": null,
                    "extensions": {
                      "appid": null,
                      "largeBlob": null,
                      "uvm": null
                    }
                  },
                  "username": "homer.simpson@gmail.com",
                  "userHandle": null
                }
                """;

        requestId = UUID.randomUUID().toString();
        loginHandle = UUID.randomUUID().toString();
        originalCreateRequest = PublicKeyCredentialCreationOptions.fromJson(createRequestJSON);
        originalAssertionRequest = AssertionRequest.fromJson(assertionRequestJSON);
        registrationRequestEntity = repository.save(WebAuthnRequestEntity.builder()
                .requestId(requestId)
                .requestType(RequestType.REGISTRATION)
                .request(createRequestJSON)
                .build());
        assertionRequestEntity = repository.save(WebAuthnRequestEntity.builder()
                .requestId(loginHandle)
                .requestType(RequestType.ASSERTION)
                .request(assertionRequestJSON)
                .build());
        registrationRequestEntity = repository.save(registrationRequestEntity);
        assertionRequestEntity = repository.save(assertionRequestEntity);
    }

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void testRequestType() {
        Assertions.assertEquals(RequestType.REGISTRATION, webAuthnRequestCache.requestType(originalCreateRequest));
        Assertions.assertEquals(RequestType.ASSERTION, webAuthnRequestCache.requestType(originalAssertionRequest));
        Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.requestType("Hello"));
    }

    @Test
    void testRequestType_CLASS() {
        Assertions.assertEquals(RequestType.REGISTRATION, webAuthnRequestCache.requestType(PublicKeyCredentialCreationOptions.class));
        Assertions.assertEquals(RequestType.ASSERTION, webAuthnRequestCache.requestType(AssertionRequest.class));
        Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.requestType(String.class));
    }

    @Test
    void testFromJson() {
        Assertions.assertNotNull(webAuthnRequestCache.fromJson(createRequestJSON, PublicKeyCredentialCreationOptions.class));
        Assertions.assertNotNull(webAuthnRequestCache.fromJson(assertionRequestJSON, AssertionRequest.class));
        Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.fromJson(assertionRequestJSON, User.class));
        RootException rootException = Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.fromJson("bad json data", PublicKeyCredentialCreationOptions.class));
        Assertions.assertTrue(rootException.getCause() instanceof JsonProcessingException);
    }

    @Test
    void testToJson() {
        Assertions.assertNotNull(webAuthnRequestCache.toJson(originalCreateRequest));
        Assertions.assertNotNull(webAuthnRequestCache.toJson(originalAssertionRequest));
        Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.toJson(User.builder().build()));
    }

    @Test
    void testExpiryTime() {
        Assertions.assertNotNull(webAuthnRequestCache.expiryTime(originalCreateRequest));
        Assertions.assertNotNull(webAuthnRequestCache.expiryTime(originalAssertionRequest));
        Assertions.assertThrows(RootException.class, () -> webAuthnRequestCache.expiryTime(User.builder().build()));
    }

    @Test
    void saveAndLoadRequest() {
        String id = UUID.randomUUID().toString();
        webAuthnRequestCache.saveRequest(id, originalCreateRequest);
        PublicKeyCredentialCreationOptions creationOptions = webAuthnRequestCache.loadRequest(id, PublicKeyCredentialCreationOptions.class);
        Assertions.assertEquals(originalCreateRequest.getChallenge(), creationOptions.getChallenge());
    }

    @Test
    void saveRequest() {
        Assertions.assertThrows(DuplicateEntityFound.class, () -> webAuthnRequestCache.saveRequest(requestId, originalCreateRequest));
    }

    @Test
    void loadRequest_Simulate_Cache_Load() {
        Assertions.assertNotNull(webAuthnRequestCache.loadRequest(loginHandle, AssertionRequest.class));
    }
    @Test
    void loadRequest() {
        Assertions.assertThrows(EntityNotFound.class, () -> webAuthnRequestCache.loadRequest("random_id", AssertionRequest.class));
    }
}