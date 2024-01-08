package io.basswood.webauthn;

import org.springframework.beans.factory.annotation.Value;
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
public class JPAConfiguration {
    @Value("${basswood.datasource.url}")
    private String url;
    @Value("${basswood.datasource.username}")
    private String username;
    @Value("${basswood.datasource.password}")
    private String password;
    @Value("${basswood.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }
}
