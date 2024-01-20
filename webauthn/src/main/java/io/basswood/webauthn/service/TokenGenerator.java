package io.basswood.webauthn.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.exception.TokenValidationError;
import io.basswood.webauthn.model.token.Token;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TokenGenerator {
    public static final String CLAIM_NAME_ROLES = "roles";
    private final AlgorithmSelectionStrategy algorithmSelectionStrategy;

    public TokenGenerator() {
        algorithmSelectionStrategy = new AlgorithmSelectionStrategy() {
        };
    }

    public SignedJWT createSignedJWT(JWK jwk, Token token) {
        SignedJWT signedJWT = new SignedJWT(header(jwk), jwtClaimsSet(token));
        try {
            signedJWT.sign(algorithmSelectionStrategy.signer(jwk));
        } catch (JOSEException e) {
            throw new RootException("Failed to sign jwt token", e);
        }
        return signedJWT;
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

    private JWSHeader header(JWK jwk) {
        return new JWSHeader.Builder(algorithmSelectionStrategy.signatureAlgorithm(jwk))
                .keyID(jwk.getKeyID())
                .type(JOSEObjectType.JWT)
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

    public boolean validateSignedJWT(JWK jwk, String jwtString) {
        return validateSignedJWT(jwk, parseSignedJWT(jwtString));
    }

    public boolean validateSignedJWT(JWK jwk, SignedJWT signedJWT) {
        JWTClaimsSet claimsSet;
        try {
            claimsSet = signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new TokenValidationError("Failed to parse claim set", e);
        }
        Object exp = claimsSet.getClaim(JWTClaimNames.EXPIRATION_TIME);
        if (exp != null && exp instanceof Date expiryDate) {
            if (expiryDate.before(new Date())) {
                throw new TokenValidationError("Token expired");
            }
        }
        return verifySignature(jwk, signedJWT);
    }

    public void validateEncryptedToken(JWK jwk, String jsonWebEncryptedToken) {
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
        } catch (ParseException e) {
            throw new TokenValidationError("Failed to extract token claims", e);
        }
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
        if (token.claimSet() != null && !token.claimSet().isEmpty()) {
            token.claimSet().forEach((k, v) -> builder.claim(k, v));
        }
        return builder.build();
    }

    public Token createToken(String subject, String issuer, String audience, Duration tokenLifeTimeDuration, List<String> roles) {
        Instant now = Instant.now();
        Instant expiredAt = now.plusMillis(tokenLifeTimeDuration.toMillis());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(CLAIM_NAME_ROLES, roles);
        return new Token(
                subject, issuer, audience, UUID.randomUUID().toString(),
                Date.from(now), Date.from(now), Date.from(expiredAt), claims
        );
    }

    public SignedJWT parseSignedJWT(String jwtToken){
        try {
            return SignedJWT.parse(jwtToken);
        } catch (ParseException e) {
            throw new TokenValidationError("Failed to parse JWT", e);
        }
    }

    public boolean hasClaim(SignedJWT jwt, String claimName, String claimValue){
        Object claim;
        try {
            claim = jwt.getJWTClaimsSet().getClaim(claimName);
        } catch (ParseException e) {
            throw new TokenValidationError("Failed to parse token claim", e);
        }
        if(claim instanceof List claims){
            return claims.contains(claimValue);
        }else if(claim instanceof String value){
            return value.equals(claimValue);
        }
        return false;
    }
}
