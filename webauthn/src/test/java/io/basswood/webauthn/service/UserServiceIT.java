package io.basswood.webauthn.service;

import io.basswood.webauthn.exception.BadRequest;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import io.basswood.webauthn.repository.BaseRepositoryIT;
import io.basswood.webauthn.repository.UserRepository;
import io.basswood.webauthn.repository.UsernameRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class UserServiceIT extends BaseRepositoryIT {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UsernameRepository usernameRepository;

    private UserService userService;

    List<User> users;

    @BeforeEach
    void setup() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        userService = new UserService(userRepository, usernameRepository, secureRandom);
        users = List.of(
                User.builder()
                        .userHandle(UUID.randomUUID().toString())
                        .displayName("Homer Simpson")
                        .usernames(Set.of(
                                Username.builder()
                                        .username("homer.simpson@gmail.com")
                                        .build()
                        ))
                        .build(),
                User.builder()
                        .userHandle(UUID.randomUUID().toString())
                        .displayName("Merge Simpson")
                        .usernames(Set.of(
                                Username.builder()
                                        .username("merge.simpson@gmail.com")
                                        .build()
                        ))
                        .build()
        );
        users.forEach(user -> user.getUsernames().forEach(username -> username.setUser(user)));
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    void testCreateUser() {
        User expected = users.get(0);
        User actual = userService.createUser(expected);
        Assertions.assertEquals(expected.getUserHandle(), actual.getUserHandle());
        Assertions.assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    @Test
    void testCreateUser_NO_USER_HANDLE() {
        User expected = users.get(0);
        String oldId = expected.getUserHandle();
        expected.setUserHandle(null);
        User actual = userService.createUser(expected);
        Assertions.assertNotNull(actual.getUserHandle());
        Assertions.assertNotEquals(oldId, actual.getUserHandle());
        Assertions.assertEquals(expected.getDisplayName(), actual.getDisplayName());
    }

    @Test
    void testCreateUser_NO_USERNAME() {
        User expected = users.get(0);
        expected.setUsernames(null);
        Assertions.assertThrows(BadRequest.class, () -> userService.createUser(expected));
    }

    @Test
    void testCreateRelyingParty_DUPLICATE_ID() {
        User expected = users.get(0);
        String id = expected.getUserHandle();
        expected = userRepository.save(expected);
        User anotherUser = users.get(1);
        anotherUser.setUserHandle(id); // set same id as first user
        Assertions.assertThrows(DuplicateEntityFound.class, () -> userService.createUser(anotherUser));
    }

    @Test
    void testCreateRelyingParty_DUPLICATE_USERNAME() {
        final User expected = users.get(0);
        String username = expected.getUsernames().stream().findFirst().get().getUsername();
        userRepository.save(expected);
        User anotherUser = users.get(1);
        anotherUser.getUsernames().stream().findFirst().get().setUsername(username);
        Assertions.assertThrows(DuplicateEntityFound.class, () -> userService.createUser(anotherUser));
    }

    @Test
    void testFindById() {
        User expected = users.get(0);
        String id = expected.getUserHandle();
        expected = userRepository.save(expected);
        Optional<User> optional = userService.findUserById(id);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(expected.getUserHandle(), optional.get().getUserHandle());
        Assertions.assertEquals(expected.getDisplayName(), optional.get().getDisplayName());
    }


    @Test
    void testFindById_NotFound() {
        String id = "random-id";
        Optional<User> optional = userService.findUserById(id);
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    void testFindUserByUsername() {
        User expected = users.get(0);
        String username = expected.getUsernames().stream().findFirst().get().getUsername();
        expected = userRepository.save(expected);
        Optional<User> optional = userService.findUserByUsername(username);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(expected.getUserHandle(), optional.get().getUserHandle());
        Assertions.assertEquals(expected.getDisplayName(), optional.get().getDisplayName());
    }


    @Test
    void testFindUserByUsername_NotFound() {
        String username = "random-id";
        Optional<User> optional = userService.findUserByUsername(username);
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    void testDeleteUser() {
        User expected = users.get(0);
        String id = users.get(0).getUserHandle();
        expected = userRepository.save(expected);
        Optional<User> optional = userService.deleteUser(id);
        Assertions.assertTrue(optional.isPresent());
    }


    @Test
    void testDeleteUser_NotFound() {
        String id = "random-id";
        Optional<User> optional = userService.deleteUser(id);
        Assertions.assertTrue(optional.isEmpty());
    }
}