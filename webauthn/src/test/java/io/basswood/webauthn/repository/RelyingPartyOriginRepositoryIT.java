package io.basswood.webauthn.repository;

import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.model.rp.RelyingPartyOrigin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RelyingPartyOriginRepositoryIT extends BaseRepositoryIT {
    @Autowired
    private RelyingPartyOriginRepository relyingPartyOriginRepository;
    @Autowired
    private RelyingPartyRepository relyingPartyRepository;

    @BeforeEach
    void setup() {
        List<RelyingPartyEntity> rps = List.of(
                RelyingPartyEntity.builder()
                        .id("example.com")
                        .name("Example")
                        .allowOriginPort(true)
                        .allowOriginSubdomain(true)
                        .origins(
                                Set.of(
                                        RelyingPartyOrigin.builder()
                                                .origin("example.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("a.example.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("b.example.com")
                                                .build()
                                )
                        )
                        .build(),
                RelyingPartyEntity.builder()
                        .id("hello.com")
                        .name("Hello")
                        .allowOriginPort(true)
                        .allowOriginSubdomain(true)
                        .origins(
                                Set.of(
                                        RelyingPartyOrigin.builder()
                                                .origin("hello.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("a.hello.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("b.hello.com")
                                                .build()
                                )
                        )
                        .build(),
                RelyingPartyEntity.builder()
                        .id("beyond.com")
                        .name("Beyond")
                        .allowOriginPort(true)
                        .allowOriginSubdomain(true)
                        .origins(
                                Set.of(
                                        RelyingPartyOrigin.builder()
                                                .origin("beyond.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("a.beyond.com")
                                                .build(),
                                        RelyingPartyOrigin.builder()
                                                .origin("b.beyond.com")
                                                .build()
                                )
                        )
                        .build()
        );
        rps.forEach(rp -> {
            rp.getOrigins().forEach(origin -> origin.setRelyingPartyEntity(rp));
        });
        relyingPartyRepository.saveAll(rps);
    }

    @Test
    void testFindDistinctByOrigin() {
        Optional<RelyingPartyOrigin> distinctByOrigin = relyingPartyOriginRepository.findDistinctByOrigin("b.beyond.com");
        Assertions.assertTrue(distinctByOrigin.isPresent());
        Assertions.assertEquals("b.beyond.com", distinctByOrigin.get().getOrigin());
    }

    @Test
    void testFindByRelyingPartyOrigins() {
        Set<String> origins = Set.of("example.com", "a.hello.com");
        Set<RelyingPartyOrigin> byRelyingPartyOrigins = relyingPartyOriginRepository.findByRelyingPartyOrigins(origins);
        Assertions.assertEquals(2, byRelyingPartyOrigins.size());
        Optional<RelyingPartyEntity> optionalRp = byRelyingPartyOrigins.stream().map(o -> o.getRelyingPartyEntity())
                .filter(rp -> "example.com".equals(rp.getId()))
                .findFirst();
        Assertions.assertTrue(optionalRp.isPresent());
        Assertions.assertEquals("example.com", optionalRp.get().getId());

        optionalRp = byRelyingPartyOrigins.stream().map(o -> o.getRelyingPartyEntity())
                .filter(rp -> "hello.com".equals(rp.getId()))
                .findFirst();
        Assertions.assertTrue(optionalRp.isPresent());
        Assertions.assertEquals("hello.com", optionalRp.get().getId());

    }
}