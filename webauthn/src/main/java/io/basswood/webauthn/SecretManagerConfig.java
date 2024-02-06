package io.basswood.webauthn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.secret.EncryptionService;
import io.basswood.webauthn.secret.KeystoreConfig;
import io.basswood.webauthn.secret.SecretManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * @author shamualr
 * @since 1.0
 */
@Configuration
public class SecretManagerConfig {
    @Autowired
    private SecurityConfigurationProperties securityConfigurationProperties;
//    @Autowired
//    private Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder;

    @Bean
    public KeystoreConfig keystoreConfig() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(securityConfigurationProperties.getKeyStoreConfig().getInputStream(), KeystoreConfig.class);
        } catch (IOException e) {
            throw new RootException("Error creating KeystoreConfig", e);
        }
    }

    @Bean
    public SecretManager secretManager() {
        try {
            return SecretManager.build(securityConfigurationProperties.getKeyStoreLocation().getInputStream(), keystoreConfig());
        } catch (IOException e) {
            throw new RootException("Error creating KeystoreService", e);
        }
    }

    @Bean
    public EncryptionService encryptionService() {
        SecretManager secretManager = secretManager();
        SecretKey aesKey = secretManager.getAesKey();
        return EncryptionService.build(aesKey);
    }
}
