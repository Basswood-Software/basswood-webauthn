package io.basswood.webauthn.rest;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.SecurityConfigurationProperties;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.token.Token;
import io.basswood.webauthn.service.JWKService;
import io.basswood.webauthn.service.TokenGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@RestController
public class JWTController {
    private TokenGenerator tokenGenerator;
    private JWKService jwkService;
    private SecurityConfigurationProperties securityConfigurationProperties;

    public JWTController(JWKService jwkService, SecurityConfigurationProperties securityConfigurationProperties) {
        this.tokenGenerator = new TokenGenerator();
        this.jwkService = jwkService;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @PostMapping(value = "/jwt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String createJWT(@RequestBody Token token) {
        JWKSet jwks = jwkService.jwks();
        Optional<JWK> optionalJWK = jwks.getKeys().stream()
                .filter(key -> key.getKeyUse() == KeyUse.SIGNATURE)
                .findAny();
        if (optionalJWK.isEmpty()) {
            throw new RootException("No signature keys found in repository.");
        }
        Token tk = setupTokenWithDefaults(token);
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(optionalJWK.get(), tk);
        return signedJWT.serialize();
    }

    /**
     * Setup various missing token information and returns a new Token.
     *
     * @param token
     * @return
     */
    private Token setupTokenWithDefaults(Token token) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(securityConfigurationProperties.getDefaultLifetimeSeconds());
        return new Token(
                token.subject() != null ? token.subject() : securityConfigurationProperties.getDefaultSubject(),
                token.issuer() != null ? token.issuer() : securityConfigurationProperties.getDefaultIssuer(),
                token.audience() != null ? token.audience() : securityConfigurationProperties.getDefaultAudience(),
                token.jti() != null ? token.jti() : UUID.randomUUID().toString(),
                token.issueTime() != null ? token.issueTime() : Date.from(now),
                token.notBeforeTime() != null ? token.notBeforeTime() : Date.from(now),
                token.expirationTime() != null ? token.expirationTime() : Date.from(exp),
                token.claimSet()
        );
    }
}
