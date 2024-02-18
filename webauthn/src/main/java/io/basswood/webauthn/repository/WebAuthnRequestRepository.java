package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.request.WebAuthnRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebAuthnRequestRepository extends JpaRepository<WebAuthnRequestEntity, String> {

}
