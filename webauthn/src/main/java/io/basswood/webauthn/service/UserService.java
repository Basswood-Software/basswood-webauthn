package io.basswood.webauthn.service;

import io.basswood.webauthn.exception.BadRequest;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import io.basswood.webauthn.repository.UserRepository;
import io.basswood.webauthn.repository.UsernameRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * @author shamualr
 * @since 1.0
 */
public class UserService {
    private static final int USER_HANDLE_LENGTH_IN_BYTES = 16;
    private UserRepository userRepository;
    private UsernameRepository usernameRepository;
    private SecureRandom secureRandom;


    public UserService(UserRepository userRepository, UsernameRepository usernameRepository, SecureRandom secureRandom) {
        this.userRepository = userRepository;
        this.usernameRepository = usernameRepository;
        this.secureRandom = secureRandom;
    }

    public Optional<User> findUserById(String userHandle) {
        return userRepository.findById(userHandle);
    }

    public Optional<User> findUserByUsername(String username) {
        Optional<Username> optionalUsername = usernameRepository.findDistinctByUsername(username);
        return optionalUsername.isEmpty() ? Optional.empty() : Optional.of(optionalUsername.get().getUser());
    }

    public User createUser(User user) {
        if (user.getUserHandle() == null) {
            user.setUserHandle(generateRandomUserHandle());
        }
        if (user.getUsernames() == null || user.getUsernames().isEmpty()) {
            throw new BadRequest("At least one username must be provided.");
        }

        user.getUsernames().forEach(username -> username.setUser(user));

        User saveduser = userRepository.save(user);
        return saveduser;
    }

    public Optional<User> getUser(String userHandle) {
        return userRepository.findDistinctByUserHandle(userHandle);
    }

    public Optional<User> deleteUser(String userHandle){
        Optional<User> userOptional = userRepository.findDistinctByUserHandle(userHandle);
        if(userOptional.isPresent()){
            userRepository.delete(userOptional.get());
        }
        return userOptional;
    }

    private String generateRandomUserHandle() {
        byte[] randomBytes = new byte[USER_HANDLE_LENGTH_IN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getEncoder().encodeToString(randomBytes);
    }
}
