package io.basswood.webauthn.rest;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.SecurityConfigurationProperties;
import io.basswood.webauthn.model.token.Token;
import io.basswood.webauthn.service.JWKService;
import io.basswood.webauthn.service.TokenGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;
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
        Token tk = setupTokenWithDefaults(token);
        JWK jwk = jwkService.latestSignatureKey();
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(jwk, tk);
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
