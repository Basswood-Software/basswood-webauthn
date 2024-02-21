package io.basswood.webauthn.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.basswood.webauthn.model.request.RequestType;
import io.basswood.webauthn.model.request.WebAuthnRequestEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

public class WebAuthnRequestRepositoryIT extends BaseRepositoryIT {
    @Autowired
    private WebAuthnRequestRepository repository;
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
    void testLoad() {
        Optional<WebAuthnRequestEntity> byId = repository.findById(this.requestId);
        Assertions.assertTrue(byId.isPresent());
        WebAuthnRequestEntity webAuthnRequestEntity = byId.get();
        Assertions.assertEquals(RequestType.REGISTRATION, webAuthnRequestEntity.getRequestType());
        Assertions.assertEquals(registrationRequestEntity.getExpiryTime(), webAuthnRequestEntity.getExpiryTime());
        Assertions.assertEquals(registrationRequestEntity.getCreatedTime(), webAuthnRequestEntity.getCreatedTime());
        Assertions.assertEquals(registrationRequestEntity.getRequestId(), webAuthnRequestEntity.getRequestId());
        try {
            PublicKeyCredentialCreationOptions actual = PublicKeyCredentialCreationOptions.fromJson(webAuthnRequestEntity.getRequest());
            Assertions.assertEquals(originalCreateRequest.getChallenge(), actual.getChallenge());
        } catch (JsonProcessingException e) {
            Assertions.fail("Failed to parse JSON payload in entity");
        }
    }
}