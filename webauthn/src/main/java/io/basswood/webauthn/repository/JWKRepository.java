package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.jwk.JWKEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JWKRepository extends JpaRepository<JWKEntity, Long> {
    Optional<JWKEntity> findDistinctByKid(String kid);
}
