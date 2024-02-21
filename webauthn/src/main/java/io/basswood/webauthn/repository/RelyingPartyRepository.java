package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelyingPartyRepository extends JpaRepository<RelyingPartyEntity, String> {
}