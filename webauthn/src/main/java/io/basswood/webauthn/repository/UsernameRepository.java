package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.user.Username;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsernameRepository extends JpaRepository<Username, Long> {
    Optional<Username> findDistinctByUsername(String username);
}
