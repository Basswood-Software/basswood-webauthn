package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findDistinctByUserHandle(String userHandle);
}
