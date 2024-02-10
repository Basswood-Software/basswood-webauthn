package io.basswood.webauthn.service;

import io.basswood.webauthn.dto.RelyingPartyDto;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.model.rp.RelyingPartyOrigin;
import io.basswood.webauthn.repository.BaseRepositoryIT;
import io.basswood.webauthn.repository.RelyingPartyOriginRepository;
import io.basswood.webauthn.repository.RelyingPartyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RelyingPartyServiceIT extends BaseRepositoryIT {
    @Autowired
    private RelyingPartyRepository rpRepository;
    @Autowired
    private RelyingPartyOriginRepository originRepository;

    private RelyingPartyService rpService;

    List<RelyingPartyEntity> rps;

    @BeforeEach
    void setup() {
        rpService = new RelyingPartyService(rpRepository, originRepository);
        rps = List.of(
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
    }

    @AfterEach
    void cleanup() {
        rpRepository.deleteAll();
    }

    private RelyingPartyDto toRelyingPartyDto(RelyingPartyEntity relyingPartyEntity) {
        return new RelyingPartyDto(
                relyingPartyEntity.getId(),
                relyingPartyEntity.getName(),
                relyingPartyEntity.getAttestation(),
                relyingPartyEntity.getAuthenticatorAttachment(),
                relyingPartyEntity.getResidentKey(),
                relyingPartyEntity.getUserVerification(),
                relyingPartyEntity.getAllowOriginPort(),
                relyingPartyEntity.getAllowOriginSubdomain(),
                relyingPartyEntity.getTimeout(),
                relyingPartyEntity.getOrigins().stream().map(o -> o.getOrigin()).collect(Collectors.toSet())
        );
    }

    @Test
    void testCreateRelyingParty() {
        RelyingPartyEntity expected = rps.get(0);
        RelyingPartyDto relyingPartyDto = toRelyingPartyDto(expected);
        RelyingPartyEntity actual = rpService.createNewRelyingParty(relyingPartyDto);
        Assertions.assertEquals(expected.getId(), actual.getId());
    }

    @Test
    void testCreateRelyingParty_DUPLICATE_ID() {
        RelyingPartyEntity expected = rps.get(0);
        String id = rps.get(0).getId();
        expected = rpRepository.save(expected);

        RelyingPartyDto relyingPartyDto = toRelyingPartyDto(expected);
        Assertions.assertThrows(DuplicateEntityFound.class, () -> rpService.createNewRelyingParty(relyingPartyDto));
    }

    @Test
    void testCreateRelyingParty_DUPLICATE_ORIGIN() {
        final RelyingPartyEntity expected = rps.get(0);
        String origin = rps.get(0).getOrigins().stream()
                .map(o -> o.getOrigin())
                .filter( v -> !(expected.getId().equals(v)))
                .findFirst().get();
        rpRepository.save(expected);

        RelyingPartyEntity anotherEntity = rps.get(1);
        RelyingPartyOrigin conflictingOrigin = RelyingPartyOrigin.builder()
                .origin(origin) // add conflicting origin
                .relyingPartyEntity(anotherEntity)
                .build();

        Set<RelyingPartyOrigin> allOrigins = new LinkedHashSet<>(anotherEntity.getOrigins());
        allOrigins.add(conflictingOrigin);
        anotherEntity.setOrigins(allOrigins);

        RelyingPartyDto relyingPartyDto = toRelyingPartyDto(anotherEntity);
        Assertions.assertThrows(DuplicateEntityFound.class, () -> rpService.createNewRelyingParty(relyingPartyDto));
    }

    @Test
    void testFindById(){
        RelyingPartyEntity expected = rps.get(0);
        String id = rps.get(0).getId();
        expected = rpRepository.save(expected);
        Optional<RelyingPartyEntity> optional = rpService.findById(id);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(expected.getId(), optional.get().getId());
    }
    @Test
    void testFindById_NotFound(){
        String id = "random-id";
        Optional<RelyingPartyEntity> optional = rpService.findById(id);
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    void testRemoveById(){
        RelyingPartyEntity expected = rps.get(0);
        String id = rps.get(0).getId();
        expected = rpRepository.save(expected);
        Optional<RelyingPartyEntity> optional = rpService.removeById(id);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(expected.getId(), optional.get().getId());
    }
    @Test
    void testRemoveById_NotFound(){
        String id = "random-id";
        Optional<RelyingPartyEntity> optional = rpService.removeById(id);
        Assertions.assertTrue(optional.isEmpty());
    }


    @Test
    void testFindByOrigin(){
        final RelyingPartyEntity expected = rps.get(0);
        String origin = rps.get(0).getOrigins().stream()
                .map(o -> o.getOrigin())
                .filter( v -> !(expected.getId().equals(v)))
                .findFirst().get();
        rpRepository.save(expected);
        Optional<RelyingPartyEntity> optional = rpService.findByOrigin(origin);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(expected.getId(), optional.get().getId());
    }
    @Test
    void testFindByOrigin_NotFound(){
        String origin = "random-origin";
        Optional<RelyingPartyEntity> optional = rpService.findByOrigin(origin);
        Assertions.assertTrue(optional.isEmpty());
    }
}