package io.basswood.webauthn;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author shamualr
 * @since 1.0
 */
@Configuration
@Data
public class SecurityConfigurationProperties {
    @Value("${basswood.security.jwt.filter.disable}")
    private Boolean disableJwtFilter;
    @Value("${basswood.security.jwt.default-subject}")
    private String defaultSubject;
    @Value("${basswood.security.jwt.default-issuer}")
    private String defaultIssuer;
    @Value("${basswood.security.jwt.default-audience}")
    private String defaultAudience;
    @Value("${basswood.security.jwt.default-lifetime-seconds}")
    private Integer defaultLifetimeSeconds;
    @Value("${basswood.security.jwt.print-new-token-on-startup}")
    private Boolean printNewTokenOnStartup;
    @Value("${basswood.security.keystore.keystore-file}")
    private Resource keyStoreLocation;
    @Value("${basswood.security.keystore.keystore-config-file}")
    private Resource keyStoreConfig;
    @Value("${basswood.security.keystore.load-jwk-file-on-startup}")
    private Boolean loadJwkFileOnStartup;
    @Value("${basswood.security.keystore.test-jwk-file}")
    private Resource testJwkFile;
}
