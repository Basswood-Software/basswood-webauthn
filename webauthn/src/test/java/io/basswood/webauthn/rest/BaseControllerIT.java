package io.basswood.webauthn.rest;

import io.basswood.webauthn.JPAConfiguration;
import io.basswood.webauthn.SecretManagerConfig;
import io.basswood.webauthn.SecurityConfigurationProperties;
import jakarta.validation.constraints.NotNull;
import org.junit.BeforeClass;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@Transactional
@AutoConfigureCache
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseControllerIT.DataSourceInitializer.class,
        classes = {JPAConfiguration.class, SecretManagerConfig.class, SecurityConfigurationProperties.class})
public abstract class BaseControllerIT {
    //@Container /** Using @Container annotation will spin new container for each test  **/
    private static JdbcDatabaseContainer database = new MySQLContainer("mysql:8.2")
            .withDatabaseName("webauthn_basswood")
            .withUsername("basswood")
            .withPassword("basswood");

    public static class DataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                    "spring.test.database.replace=none", // Tells Spring Boot not to start in-memory db for tests.
                    "basswood.datasource.driver-class-name=" + "org.testcontainers.jdbc.ContainerDatabaseDriver",
                    "basswood.datasource.url=" + "jdbc:tc:mysql:8.2://localhost:3306/webauthn_basswood",
                    "basswood.datasource.username=" + "basswood",
                    "basswood.datasource.password=" + "basswood",
                    "spring.sql.init.mode=" + "always");
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("basswood.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("basswood.datasource.username", () -> "basswood");
        registry.add("basswood.datasource.password", () -> "basswood");
        registry.add("basswood.datasource.url", () -> "jdbc:tc:mysql:8.2://localhost:3306/webauthn_basswood");
    }

    @BeforeClass
    public static void setupClass() {
        database.start();
    }
}