package io.basswood.webauthn;

import io.basswood.webauthn.secret.SecretManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

/**
 * @author shamualr
 * @since 1.0
 */
@Configuration
@EnableJpaRepositories(basePackages = {"io.basswood.webauthn"})
@EntityScan(basePackages = {"io.basswood.webauthn.model.*"})
public class JPAConfiguration {
    @Value("${basswood.datasource.url}")
    private String url;
    @Value("${basswood.datasource.username}")
    private String username;
    @Value("${basswood.datasource.driver-class-name}")
    private String driverClassName;
    @Autowired
    private SecretManager secretManager;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(secretManager.getDatabasePassword())
                .driverClassName(driverClassName)
                .build();
    }
}
