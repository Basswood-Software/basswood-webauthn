package io.basswood.webauthn.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.model.token.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.basswood.webauthn.service.TokenGenerator.CLAIM_NAME_ROLES;

class TokenGeneratorTest {
    private static final Duration TOKEN_LIFE_TIME = Duration.ofSeconds(300);
    private JWK jwk;
    private String sub;
    private String issuer;
    private String audience;
    private String role;
    private List<String> roles;
    private TokenGenerator tokenGenerator;
    private Token token;

    @BeforeEach
    void setup() throws JOSEException {
        Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
        sub = UUID.randomUUID().toString();
        issuer = UUID.randomUUID().toString();
        audience = UUID.randomUUID().toString();
        role = "webauthn_manager";
        roles = Arrays.asList(role);

        tokenGenerator = new TokenGenerator();
        jwk = new ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .generate();
        token = tokenGenerator.createToken(sub, issuer, audience, TOKEN_LIFE_TIME, roles);
    }


    @Test
    void testCreateSignedJWT() throws ParseException {
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        String expected = signedJWT.serialize();
        Assertions.assertTrue(tokenGenerator.verifySignature(jwk, signedJWT));
        // Now create encrypted token and decrypt.
        JWEObject jweObject = tokenGenerator.encryptJWT(jwk, signedJWT);
        SignedJWT decryptedJWT = tokenGenerator.decryptJWT(jwk, jweObject);
        String actual = decryptedJWT.serialize();
        Assertions.assertEquals(expected, actual);
        // Let's verify the claims in the decrypted token
        JWTClaimsSet jwtClaimsSet = decryptedJWT.getJWTClaimsSet();
        Assertions.assertEquals(token.jti(), jwtClaimsSet.getJWTID());
        Assertions.assertEquals(token.subject(), jwtClaimsSet.getSubject());
        Assertions.assertEquals(token.issuer(), jwtClaimsSet.getIssuer());
        Assertions.assertEquals(token.audience(), jwtClaimsSet.getAudience().get(0));
        Object claim = jwtClaimsSet.getClaim(CLAIM_NAME_ROLES);
        Assertions.assertTrue(claim instanceof List);
        if (claim instanceof List actualRoles) {
            Assertions.assertArrayEquals(roles.toArray(), actualRoles.toArray());
        } else {
            Assertions.fail("The roles claim not List as expected");
        }
        // validate the SignedJWT
        Assertions.assertEquals(true, tokenGenerator.validateSignedJWT(jwk, expected));
    }

    @Test
    void testHasClaim(){
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        Assertions.assertTrue(tokenGenerator.hasClaim(signedJWT, CLAIM_NAME_ROLES, role));
    }

    @Test
    void testHasClaim_NONE(){
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        Assertions.assertFalse(tokenGenerator.hasClaim(signedJWT, CLAIM_NAME_ROLES, "admin"));
    }

    @Test
    void testHasClaim_STRING_CLAIM(){
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        Assertions.assertTrue(tokenGenerator.hasClaim(signedJWT, JWTClaimNames.SUBJECT, sub));
    }
    @Test
    void testHasClaim_MISSING(){
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, token);
        Assertions.assertFalse(tokenGenerator.hasClaim(signedJWT, "randomclaim", sub));
    }
}