package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.Optional;

public interface JWKRepository extends JpaRepository<JWKEntity, String> {
    Optional<JWKEntity> findDistinctByKid(String kid);
    Optional<JWKEntity> findFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc(KeyUseEnum keyUse, Date date);
}
