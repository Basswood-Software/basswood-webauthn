package io.basswood.webauthn.rest;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JWKControllerIT extends BaseControllerIT {
    private Integer port=9080;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @Test
    void testSomething() {
        Assertions.assertEquals(2 + 2, 4);
    }
}