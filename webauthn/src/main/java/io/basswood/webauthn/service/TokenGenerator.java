package io.basswood.webauthn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.basswood.webauthn.dto.Token;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.exception.TokenValidationError;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class TokenGenerator {
    public static final String REGISTRATION_TOKEN_CLAIM_NAME = "publicKey";
    public static final String ASSERTION_TOKEN_CLAIM_NAME = "assertionRequest";

    public static final long DEFAULT_TOKEN_LIFETIME_SECONDS = 300;

    private final AlgorithmSelectionStrategy algorithmSelectionStrategy;

    public TokenGenerator() {
        algorithmSelectionStrategy = new AlgorithmSelectionStrategy() {
        };
    }

    public SignedJWT createSignedJWT(JWK jwk, Token token) {
        SignedJWT signedJWT = new SignedJWT(header(jwk, token.payloadClaimName()), jwtClaimsSet(token));
        try {
            signedJWT.sign(algorithmSelectionStrategy.signer(jwk));
        } catch (JOSEException e) {
            throw new RootException("Failed to sign jwt token", e);
        }
        return signedJWT;
    }

    public SignedJWT createSignedJWT(JWK jwk, PublicKeyCredentialCreationOptions pkco) {
        Instant now = Instant.now();
        Token token = new Token(
                pkco.getUser().getId().getBase64Url(),
                pkco.getRp().getId(),
                pkco.getRp().getId(),
                UUID.randomUUID().toString(),
                new Date(now.getEpochSecond() * 1000L),
                new Date(now.getEpochSecond() * 1000L),
                new Date((now.getEpochSecond() + DEFAULT_TOKEN_LIFETIME_SECONDS) * 1000L), pkco, REGISTRATION_TOKEN_CLAIM_NAME
        );
        return createSignedJWT(jwk, token);
    }

    public SignedJWT createSignedJWT(JWK jwk, AssertionRequest assertionRequest) {
        Instant now = Instant.now();
        Token token = new Token(
                assertionRequest.getUserHandle().get().getBase64Url(),
                assertionRequest.getPublicKeyCredentialRequestOptions().getRpId(),
                assertionRequest.getPublicKeyCredentialRequestOptions().getRpId(),
                UUID.randomUUID().toString(),
                new Date(now.getEpochSecond() * 1000L),
                new Date(now.getEpochSecond() * 1000L),
                new Date((now.getEpochSecond() + DEFAULT_TOKEN_LIFETIME_SECONDS) * 1000L), assertionRequest, ASSERTION_TOKEN_CLAIM_NAME
        );
        return createSignedJWT(jwk, token);
    }

    public JWEObject encryptJWT(JWK jwk, SignedJWT signedJWT) {
        JWEObject jweObject = new JWEObject(new JWEHeader.Builder(algorithmSelectionStrategy.encryptionAlgorithm(jwk),
                algorithmSelectionStrategy.encryptionMethod())
                .contentType("JWT") // required to indicate nested JWT
                .keyID(jwk.getKeyID()).build(),
                new Payload(signedJWT));

        try {
            jweObject.encrypt(algorithmSelectionStrategy.encrypter(jwk));
        } catch (JOSEException e) {
            throw new RootException("Encryption error", e);
        }
        return jweObject;
    }

    public SignedJWT decryptJWT(JWK jwk, JWEObject jweObject) {
        String keyID = jweObject.getHeader().getKeyID();
        if (!jwk.getKeyID().equals(keyID)) {
            throw new TokenValidationError("Invalid encryption key - keyId do not match");
        }
        try {
            jweObject.decrypt(algorithmSelectionStrategy.decrypter(jwk));
        } catch (JOSEException e) {
            throw new TokenValidationError("Token decryption failed.", e);
        }
        SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();
        return signedJWT;
    }

    private JWSHeader header(JWK jwk, String contentType) {
        return new JWSHeader.Builder(algorithmSelectionStrategy.signatureAlgorithm(jwk))
                .keyID(jwk.getKeyID())
                .type(JOSEObjectType.JWT)
                .contentType(contentType)
                .build();
    }

    public boolean verifySignature(JWK jwk, SignedJWT jwt) {
        String keyID = jwt.getHeader().getKeyID();
        if (!jwk.getKeyID().equals(keyID)) {
            throw new TokenValidationError("Invalid signature key - keyId do not match");
        }
        try {
            return jwt.verify(algorithmSelectionStrategy.verifier(jwk));
        } catch (JOSEException e) {
            throw new TokenValidationError("Signature validation failed", e);
        }
    }

    public <T> T validateEncryptedToken(JWK jwk, String jsonWebEncryptedToken, Class<T> payloadType) {
        JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(jsonWebEncryptedToken);
        } catch (ParseException e) {
            throw new TokenValidationError("Token decryption failed", e);
        }
        SignedJWT signedJWT = decryptJWT(jwk, jweObject);
        if (!verifySignature(jwk, signedJWT)) {
            throw new TokenValidationError("Signature validation false");
        }
        try {
            Date now = new Date();
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime.before(now)) {
                throw new TokenValidationError("Token expired");
            }
            return extractTokenPayload(signedJWT, payloadType);
        } catch (ParseException e) {
            throw new TokenValidationError("Failed to extract token claims", e);
        } catch (JsonProcessingException e) {
            throw new TokenValidationError("Failed to extract token payload", e);
        }
    }

    private <T> T extractTokenPayload(SignedJWT signedJWT, Class<T> payloadType) throws ParseException, JsonProcessingException {
        T payload = null;
        if (payloadType == PublicKeyCredentialCreationOptions.class) {
            String payloadAsString = signedJWT.getJWTClaimsSet().getStringClaim(REGISTRATION_TOKEN_CLAIM_NAME);
            if (payloadAsString != null) {
                payload = (T) PublicKeyCredentialCreationOptions.fromJson(payloadAsString);
            }
        } else if (payloadType == AssertionRequest.class) {
            String payloadAsString = signedJWT.getJWTClaimsSet().getStringClaim(ASSERTION_TOKEN_CLAIM_NAME);
            if (payloadAsString != null) {
                payload = (T) AssertionRequest.fromJson(payloadAsString);
            }
        } else {
            throw new TokenValidationError("Unsupported payload type:" + payloadType.getName());
        }
        if (payload == null) {
            throw new TokenValidationError("The payload is empty");
        }
        return payload;
    }

    private JWTClaimsSet jwtClaimsSet(Token token) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(token.subject())
                .issuer(token.issuer())
                .audience(token.audience())
                .jwtID(token.jti())
                .issueTime(token.issueTime())
                .notBeforeTime(token.notBeforeTime())
                .expirationTime(token.expirationTime());
        if (token.payload() != null) {
            builder.claim(token.payloadClaimName(), payloadAsString(token));
        }
        return builder.build();
    }

    private String payloadAsString(Token token) {
        try {
            return switch (token.payload()) {
                case null -> throw new TokenValidationError("Payload is null");
                case PublicKeyCredentialCreationOptions pkco -> pkco.toJson();
                case AssertionRequest assertionRequest -> assertionRequest.toJson();
                default -> throw new TokenValidationError("Unsupported payload type: " + token.payload().getClass());
            };
        } catch (JsonProcessingException ex) {
            throw new TokenValidationError("Failed to serialize token payload", ex);
        }
    }
}
