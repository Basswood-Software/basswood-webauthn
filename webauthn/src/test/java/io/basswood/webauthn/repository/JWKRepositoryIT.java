package io.basswood.webauthn.repository;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.JWKException;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.JWKEntityConverter;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import io.basswood.webauthn.service.JWKService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

public class JWKRepositoryIT extends BaseRepositoryIT {
    @Autowired
    private JWKRepository jwkRepository;
    private JWKService jwkService;

    @BeforeEach
    void setup() {
        jwkService = new JWKService(jwkRepository);
    }

    @Test
    void findDistinctByKid() {
        // Create a new Key
        JWKCreateDTO createDTO = JWKCreateDTO.builder().build();
        JWK key = jwkService.createKey(createDTO);
        JWKEntity jwkEntity = new JWKEntityConverter().toEntity(key);

        Optional<JWKEntity> optional = jwkRepository.findDistinctByKid(jwkEntity.getKid());
        Assertions.assertTrue(optional.isPresent());
        JWKEntity actual = optional.get();
        Assertions.assertEquals(jwkEntity.getKid(), actual.getKid());
        Assertions.assertEquals(jwkEntity.getKeyUse(), actual.getKeyUse());
        Assertions.assertEquals(jwkEntity.getKty(), actual.getKty());
        Assertions.assertEquals(jwkEntity.getExpiryTime(), actual.getExpiryTime());
        Assertions.assertEquals(jwkEntity.getCreatedTime(), actual.getCreatedTime());
    }

    @Test
    void testFindFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc() {
        Instant today = Instant.now();
        Instant issuesAtMonthAgo = today.minus(30, ChronoUnit.DAYS);
        Instant yesterday = today.minus(1, ChronoUnit.DAYS);
        Instant nextYear = today.plus(365, ChronoUnit.DAYS);
        JWKEntity pastKey = jwkService.saveKey(createECKey("expires-yesterday", KeyUse.SIGNATURE, issuesAtMonthAgo, yesterday));
        JWKEntity presentKey = jwkService.saveKey(createECKey("expires-today", KeyUse.SIGNATURE, issuesAtMonthAgo, today));
        JWKEntity futureKey = jwkService.saveKey(createECKey("expires-next-year", KeyUse.SIGNATURE, issuesAtMonthAgo, nextYear));

        Optional<JWKEntity> actualOptional = jwkRepository.findFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc(KeyUseEnum.SIGNATURE, Date.from(today));
        Assertions.assertTrue(actualOptional.isPresent());
        Assertions.assertEquals(futureKey.getKid(), actualOptional.get().getKid());

        // Delete latest key
        jwkRepository.deleteById(futureKey.getKid());
        actualOptional = jwkRepository.findFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc(KeyUseEnum.SIGNATURE, Date.from(yesterday));
        Assertions.assertTrue(actualOptional.isPresent());
        Assertions.assertEquals(presentKey.getKid(), actualOptional.get().getKid());
    }

    private ECKey createECKey(String kid, KeyUse keyUse, Instant issueTime, Instant expiryTime) {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID(kid).keyUse(keyUse).issueTime(Date.from(issueTime)).expirationTime(Date.from(expiryTime)).generate();
        } catch (JOSEException e) {
            throw new JWKException("Failed to create RSA Key", e);
        }
    }
}