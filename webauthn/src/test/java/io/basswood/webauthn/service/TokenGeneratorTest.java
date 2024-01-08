package io.basswood.webauthn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.basswood.webauthn.dto.Token;
import io.basswood.webauthn.exception.TokenValidationError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static io.basswood.webauthn.service.TokenGenerator.ASSERTION_TOKEN_CLAIM_NAME;
import static io.basswood.webauthn.service.TokenGenerator.DEFAULT_TOKEN_LIFETIME_SECONDS;
import static io.basswood.webauthn.service.TokenGenerator.REGISTRATION_TOKEN_CLAIM_NAME;

class TokenGeneratorTest {
    private static PublicKeyCredentialCreationOptions pkco;

    private static AssertionRequest assertionRequest;
    private static JWK jwk;

    private static Token token;
    private TokenGenerator tokenGenerator;

    @BeforeAll
    static void setupClass() throws JsonProcessingException, JOSEException {
        pkco = PublicKeyCredentialCreationOptions.fromJson(
                """
                        {
                          "rp": {
                            "name": "Basswood Red Client",
                            "id": "red.basswoodid.com:9080"
                          },
                          "user": {
                            "name": "homer.simpson@aol.com",
                            "displayName": "Homer Simpson",
                            "id": "9bd16129-15fc-4115-8e70-4fd7c9b4c9f5"
                          },
                          "challenge": "CAJTrIpJUAaIr1E_sY4eiWc-YVpkEHaiFtj6S1Bkthg",
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
                          "timeout": 60000,
                          "excludeCredentials": [],
                          "authenticatorSelection": {
                            "authenticatorAttachment": "platform",
                            "requireResidentKey": false,
                            "residentKey": "discouraged",
                            "userVerification": "discouraged"
                          },
                          "attestation": "none",
                          "extensions": {
                            "appidExclude": null,
                            "credProps": true,
                            "largeBlob": null,
                            "uvm": null
                          }
                        }

                        """
        );

        assertionRequest = AssertionRequest.fromJson(
                """
                                {
                                  "username": "homer.simpson@aol.com",
                                  "userHandle": "9bd16129-15fc-4115-8e70-4fd7c9b4c9f5",
                                  "publicKeyCredentialRequestOptions": {
                                    "challenge": "bHB3Fd6tD0Hrg0gzylFAI9q74Vv8nhNT-csCTSRICy4",
                                    "timeout": 60000,
                                    "rpId": "red.basswoodid.com:9080",
                                    "allowCredentials": [
                                      {
                                        "type": "public-key",
                                        "id": "9bd16129-15fc-4115-8e70-4fd7c9b4c9f5cmVkLmJhc3N3b29kaWQuY29tOjkwODA",
                                        "transports": [
                                          "internal"
                                        ]
                                      }
                                    ],
                                    "userVerification": "discouraged",
                                    "extensions": {
                                      "appid": null,
                                      "largeBlob": null,
                                      "uvm": null
                                    }
                                  }
                                }
                        """
        );

        jwk = new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .generate();
        Instant now = Instant.now();
        token = new Token(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), new Date(now.getEpochSecond() * 1000L), new Date(now.getEpochSecond() * 1000L), new Date((now.getEpochSecond() + DEFAULT_TOKEN_LIFETIME_SECONDS) * 1000L), assertionRequest, ASSERTION_TOKEN_CLAIM_NAME);
    }

    @BeforeEach
    void setup() {
        tokenGenerator = new TokenGenerator();
    }


    @Test
    void testSignedJWTWithPublicKeyCredentialCreationOptions() throws ParseException, JsonProcessingException {
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, pkco);
        Assertions.assertEquals(1611, signedJWT.serialize().length());

        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        Assertions.assertEquals(2551, jweObject.serialize().length());

        SignedJWT decryptedJWT = tokenGenerator.decryptJWT(jwk, jweObject);
        Assertions.assertEquals(signedJWT.serialize(), decryptedJWT.serialize());
        Object publicKey = decryptedJWT.getJWTClaimsSet().getClaim(REGISTRATION_TOKEN_CLAIM_NAME);
        PublicKeyCredentialCreationOptions expected_pkco = PublicKeyCredentialCreationOptions.fromJson(publicKey.toString());
        Assertions.assertEquals(expected_pkco, pkco);
    }

    @Test
    void testSignedJWTWithAssertionRequest() throws ParseException, JsonProcessingException {
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, assertionRequest);
        Assertions.assertEquals(1092, signedJWT.serialize().length());

        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        Assertions.assertEquals(1859, jweObject.serialize().length());

        SignedJWT decryptedJWT = tokenGenerator.decryptJWT(jwk, jweObject);
        Object payload = decryptedJWT.getJWTClaimsSet().getClaim(ASSERTION_TOKEN_CLAIM_NAME);
        AssertionRequest actual = AssertionRequest.fromJson(payload.toString());
        Assertions.assertEquals(signedJWT.serialize(), decryptedJWT.serialize());
        Assertions.assertEquals(assertionRequest, actual);
    }

    @Test
    void testTokenValidation() throws JsonProcessingException {
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, assertionRequest);
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        String jsonWebEncryptedToken = jweObject.serialize();
        AssertionRequest actual = tokenGenerator.validateEncryptedToken(jwk, jsonWebEncryptedToken, AssertionRequest.class);
        Assertions.assertEquals(assertionRequest.toJson(), actual.toJson());
    }

    @Test
    void testTokenValidation_expired() {
        Instant now = Instant.ofEpochMilli(System.currentTimeMillis() - 60 * 60 * 1000L);
        Token token = new Token(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), new Date(now.getEpochSecond() * 1000L), new Date(now.getEpochSecond() * 1000L), new Date((now.getEpochSecond() + DEFAULT_TOKEN_LIFETIME_SECONDS) * 1000L), assertionRequest, ASSERTION_TOKEN_CLAIM_NAME);
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        String jsonWebEncryptedToken = jweObject.serialize();
        TokenValidationError tokenValidationError = Assertions.assertThrows(TokenValidationError.class, () -> tokenGenerator.validateEncryptedToken(jwk, jsonWebEncryptedToken, assertionRequest.getClass()));
        Assertions.assertEquals("Token expired", tokenValidationError.getMessage());
    }

    @Test
    void testTokenValidation_bad_jwe_token() {
        TokenValidationError tokenValidationError = Assertions.assertThrows(TokenValidationError.class, () -> tokenGenerator.validateEncryptedToken(jwk, "bad data", AssertionRequest.class));
        Assertions.assertEquals("Token decryption failed", tokenValidationError.getMessage());
        Assertions.assertEquals(ParseException.class, tokenValidationError.getCause().getClass());
    }

    @Test
    void testTokenValidation_wrong_key_id() throws JOSEException {
        JWK anotherKey = new ECKeyGenerator(Curve.P_256).keyID(jwk.getKeyID()) // fake the keyId
                .issueTime(new Date()).generate();
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, assertionRequest);
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        String jsonWebEncryptedToken = jweObject.serialize();

        TokenValidationError tokenValidationError = Assertions.assertThrows(TokenValidationError.class, () -> tokenGenerator.validateEncryptedToken(anotherKey, jsonWebEncryptedToken, assertionRequest.getClass()));
        Assertions.assertEquals("Token decryption failed.", tokenValidationError.getMessage());
    }

    @Test
    void testTokenValidation_bad_signature() throws JOSEException {
        JWK anotherKey = new ECKeyGenerator(Curve.P_256).keyID(jwk.getKeyID()) // fake the keyId
                .issueTime(new Date()).generate();
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, assertionRequest);
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        String jsonWebEncryptedToken = jweObject.serialize();

        TokenValidationError tokenValidationError = Assertions.assertThrows(TokenValidationError.class, () -> tokenGenerator.validateEncryptedToken(anotherKey, jsonWebEncryptedToken, AssertionRequest.class));
        Assertions.assertEquals("Token decryption failed.", tokenValidationError.getMessage());
        Assertions.assertEquals(JOSEException.class, tokenValidationError.getCause().getClass());
    }

    @Test
    void testTokenValidation_null_payload() {
        Instant now = Instant.now();
        Token token = new Token(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), new Date(now.getEpochSecond() * 1000L), new Date(now.getEpochSecond() * 1000L), new Date((now.getEpochSecond() + DEFAULT_TOKEN_LIFETIME_SECONDS) * 1000L), null, ASSERTION_TOKEN_CLAIM_NAME);
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        String jsonWebEncryptedToken = jweObject.serialize();

        TokenValidationError tokenValidationError = Assertions.assertThrows(TokenValidationError.class, () -> tokenGenerator.validateEncryptedToken(jwk, jsonWebEncryptedToken, AssertionRequest.class));
        Assertions.assertEquals("The payload is empty", tokenValidationError.getMessage());
    }
}