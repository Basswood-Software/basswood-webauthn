package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.credential.RegisteredCredentialEntity;
import io.basswood.webauthn.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface RegisteredCredentialEntityRepository extends JpaRepository<RegisteredCredentialEntity, String> {
    Set<RegisteredCredentialEntity> findByUser(User user);
    void deleteByUser(User user);
}
