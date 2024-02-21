package io.basswood.authenticator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ComponentScan(excludeFilters = {@ComponentScan.Filter(RestController.class),
        @ComponentScan.Filter(ControllerAdvice.class)})
// Exclude RestControllers as they will be created explicitly via the @Bean methods
public class AuthenticatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticatorApplication.class, args);
    }
}
