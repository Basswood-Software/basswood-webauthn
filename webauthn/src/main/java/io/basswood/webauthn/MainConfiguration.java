package io.basswood.webauthn;

import brave.context.log4j2.ThreadContextScopeDecorator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.basswood.webauthn.exception.GlobalErrorHandler;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.credential.CredentialRepositoryImpl;
import io.basswood.webauthn.repository.JWKRepository;
import io.basswood.webauthn.repository.RegisteredCredentialEntityRepository;
import io.basswood.webauthn.repository.RelyingPartyOriginRepository;
import io.basswood.webauthn.repository.RelyingPartyRepository;
import io.basswood.webauthn.repository.UserRepository;
import io.basswood.webauthn.repository.UsernameRepository;
import io.basswood.webauthn.repository.WebAuthnRequestRepository;
import io.basswood.webauthn.rest.JWKController;
import io.basswood.webauthn.rest.JWTController;
import io.basswood.webauthn.rest.RelyingPartyController;
import io.basswood.webauthn.rest.UserController;
import io.basswood.webauthn.rest.WebAuthnController;
import io.basswood.webauthn.security.JWTFilter;
import io.basswood.webauthn.service.JWKService;
import io.basswood.webauthn.service.RelyingPartyService;
import io.basswood.webauthn.service.UserService;
import io.basswood.webauthn.service.WebAuthnRequestCache;
import io.basswood.webauthn.service.WebAuthnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author shamualr
 * @since 1.0
 */
@Configuration
@ComponentScan(excludeFilters = {@ComponentScan.Filter(RestController.class),
        @ComponentScan.Filter(ControllerAdvice.class)})
// Exclude RestControllers as they will be created explicitly via the @Bean methods
public class MainConfiguration {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UsernameRepository usernameRepository;
    @Autowired
    private RelyingPartyRepository relyingPartyRepository;
    @Autowired
    private RelyingPartyOriginRepository relyingPartyOriginRepository;
    @Autowired
    private RegisteredCredentialEntityRepository registeredCredentialEntityRepository;
    @Autowired
    private JWKRepository jwkRepository;
    @Autowired
    private WebAuthnRequestRepository webAuthnRequestRepository;
    @Autowired
    private SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    private Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;

    // Services
    @Bean
    public SecureRandom secureRandom() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RootException("Failed to create SecureRandom instance");
        }
    }

    @Bean
    public ObjectMapper objectMapper() {
        return jackson2ObjectMapperBuilder.build();
    }

    @Bean
    public WebauthnApplicationListener webauthnApplicationListener(){
        return new WebauthnApplicationListener(jwkService(), securityConfigurationProperties);
    }
    @Bean
    public UserService userService() {
        return new UserService(userRepository, usernameRepository, secureRandom());
    }

    @Bean
    public RelyingPartyService relyingPartyService() {
        return new RelyingPartyService(relyingPartyRepository, relyingPartyOriginRepository);
    }

    @Bean
    public CredentialRepositoryImpl credentialRepository() {
        return new CredentialRepositoryImpl(userService(), registeredCredentialEntityRepository);
    }

    @Bean
    public WebAuthnService webAuthnService() {
        return new WebAuthnService(
                relyingPartyService(),
                userService(),
                credentialRepository(),
                registeredCredentialEntityRepository,
                webAuthnRequestCache()
        );
    }

    @Bean
    public JWKService jwkService(){
        return new JWKService(jwkRepository);
    }

    @Bean
    public WebAuthnRequestCache webAuthnRequestCache() {
        return new WebAuthnRequestCache(webAuthnRequestRepository);
    }

    // Filter
    @Bean
    public JWTFilter jwtFilter(){
        return new JWTFilter(jwkService(), objectMapper(), securityConfigurationProperties.getDisableJwtFilter());
    }

    //Controllers
    @Bean
    public GlobalErrorHandler globalErrorHandler() {
        return new GlobalErrorHandler();
    }

    @Bean
    public ThreadContextScopeDecorator threadContextScopeDecorator() {
        return new ThreadContextScopeDecorator();
    }

    @Bean
    public UserController userController() {
        return new UserController(userService());
    }

    @Bean
    public RelyingPartyController relyingPartyController() {
        return new RelyingPartyController(relyingPartyService());
    }

    @Bean
    public JWKController jwkController(){
        return new JWKController(jwkService());
    }
    @Bean
    public JWTController jwtController(){
        return new JWTController(jwkService(), securityConfigurationProperties);
    }

    @Bean
    public WebAuthnController webAuthnController() {
        return new WebAuthnController(webAuthnService());
    }
}
