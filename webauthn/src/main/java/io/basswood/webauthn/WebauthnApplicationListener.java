package io.basswood.webauthn;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.token.Role;
import io.basswood.webauthn.model.token.Token;
import io.basswood.webauthn.service.JWKService;
import io.basswood.webauthn.service.TokenGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author shamualr
 * @since 1.0
 */
@Slf4j
public class WebauthnApplicationListener implements ApplicationListener<ApplicationStartedEvent> {
    private JWKService jwkService;
    private SecurityConfigurationProperties securityConfigurationProperties;

    private TokenGenerator tokenGenerator;

    public WebauthnApplicationListener(JWKService jwkService, SecurityConfigurationProperties securityConfigurationProperties) {
        this.jwkService = jwkService;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.tokenGenerator = new TokenGenerator();
    }

    @EventListener
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        JWK jwk = null;
        boolean needNewKey = false;
        JWKSet jwks = jwkService.jwks();
        if (jwks == null || jwks.isEmpty()) {
            needNewKey = true;
        } else {
            Optional<JWK> any = jwks.getKeys().stream().filter(k -> k.getKeyUse() == KeyUse.SIGNATURE).findAny();
            if (any.isEmpty()) {
                needNewKey = true;
            } else {
                jwk = any.get();
            }
        }
        if (needNewKey) {
            jwk = (Boolean.TRUE.equals(securityConfigurationProperties.getLoadJwkFileOnStartup()))
                    ? loadAndSaveDefaultJWK()
                    : jwkService.createKey(JWKCreateDTO.builder().build());
        }

        if (securityConfigurationProperties.getPrintNewTokenOnStartup()) {
            printNewToken(jwk);
        }
    }

    private JWK loadAndSaveDefaultJWK() {
        JWK jwk;
        try {
            String jwkJson = securityConfigurationProperties.getTestJwkFile().getContentAsString(StandardCharsets.UTF_8);
            jwk = jwkService.parse(jwkJson);
        } catch (IOException e) {
            throw new RootException("Failed to load default JWK key", e);
        }
        // check key already in place
        Optional<JWKEntity> optional = jwkService.getJWKEntity(jwk.getKeyID());
        if (optional.isEmpty()) {
            jwkService.saveKey(jwk);
        }
        return jwk;
    }

    private void printNewToken(JWK key) {
        Token token = createToken();
        SignedJWT signedJWT = tokenGenerator.createSignedJWT(key, token);
        System.out.println("--------------------------JWT--------------------------");
        System.out.printf("\n%s\n", signedJWT.serialize());
        System.out.println("--------------------------JWT--------------------------");
    }

    private Token createToken() {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(5 * 365 * 24 * 3600L);
        return new Token(
                securityConfigurationProperties.getDefaultSubject(),
                securityConfigurationProperties.getDefaultIssuer(),
                securityConfigurationProperties.getDefaultAudience(),
                UUID.randomUUID().toString(),
                Date.from(now),
                Date.from(now),
                Date.from(exp),
                Map.of(TokenGenerator.CLAIM_NAME_ROLES,
                        Arrays.asList(Role.jwk_manager, Role.token_manager, Role.rp_manager, Role.user_manager))

        );
    }
}
