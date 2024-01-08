package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.rp.RelyingPartyOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface RelyingPartyOriginRepository extends JpaRepository<RelyingPartyOrigin, Long> {
    Optional<RelyingPartyOrigin> findDistinctByOrigin(String origin);

    @Query("select rpo from RelyingPartyOrigin rpo where rpo.origin in :origins")
    Set<RelyingPartyOrigin> findByRelyingPartyOrigins(Set<String> origins);
}