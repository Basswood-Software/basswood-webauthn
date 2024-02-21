package io.basswood.webauthn.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.basswood.webauthn.exception.JWKException;
import jakarta.validation.constraints.NotNull;

import java.text.ParseException;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * @author shamualr
 * @since 1.0
 */
public class NimbusJOSEHelper {
    public RSAKey createRSAKey(@NotNull KeyUse keyUse, @NotNull int length, Duration lifeTime) {
        try {
            return new RSAKeyGenerator(length)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(keyUse)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + lifeTime.toMillis()))
                    .generate();
        } catch (JOSEException e) {
            throw new JWKException("Failed to create RSA Key", e);
        }
    }

    public ECKey createECKey(KeyUse keyUse, Curve curve, Duration lifeTime) {
        try {
            return new ECKeyGenerator(curve)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(keyUse)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + lifeTime.toMillis()))
                    .generate();
        } catch (JOSEException e) {
            throw new JWKException("Failed to create RSA Key", e);
        }
    }

    public JWK parse(String jwk){
        try {
            return JWK.parse(jwk);
        } catch (ParseException e) {
            throw new JWKException("Failed to parse JWK key", e);
        }
    }
}
