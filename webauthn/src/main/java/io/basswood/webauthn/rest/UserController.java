package io.basswood.webauthn.rest;

import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public User createUser(@RequestBody User user) {
        User user1 = userService.createUser(user);
        return user1;
    }

    @GetMapping("/user/{userHandle}")
    public User getUser(@PathVariable String userHandle) {
        return userService.getUser(userHandle).orElseThrow(() -> new EntityNotFound(User.class, userHandle));
    }

    @DeleteMapping("/user/{userHandle}")
    public User deleteUser(@PathVariable String userHandle) {
        return userService.deleteUser(userHandle)
                .orElseThrow(() -> new EntityNotFound(User.class, userHandle));
    }

    @GetMapping("/user")
    public User getUserByUsername(@RequestParam("username") String username) {
        return userService.findUserByUsername(username)
                .orElseThrow(() -> new EntityNotFound(User.class, username));
    }
}
