package io.basswood.webauthn.service;

import io.basswood.webauthn.dto.RelyingPartyDto;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.model.rp.RelyingPartyOrigin;
import io.basswood.webauthn.repository.RelyingPartyOriginRepository;
import io.basswood.webauthn.repository.RelyingPartyRepository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RelyingPartyService {
    private RelyingPartyRepository relyingPartyRepository;

    private RelyingPartyOriginRepository relyingPartyOriginRepository;

    public RelyingPartyService(RelyingPartyRepository relyingPartyRepository, RelyingPartyOriginRepository relyingPartyOriginRepository) {
        this.relyingPartyRepository = relyingPartyRepository;
        this.relyingPartyOriginRepository = relyingPartyOriginRepository;
    }

    public RelyingPartyEntity createNewRelyingParty(RelyingPartyDto dto) {
        Optional<RelyingPartyEntity> optional = relyingPartyRepository.findById(dto.id());
        if (optional.isPresent()) {
            throw new DuplicateEntityFound(RelyingPartyEntity.class, dto.id());
        }

        Set<RelyingPartyOrigin> rpOrigins = relyingPartyOriginRepository.findByRelyingPartyOrigins(dto.origins());
        if (rpOrigins != null && !rpOrigins.isEmpty()) {
            throw new DuplicateEntityFound(RelyingPartyOrigin.class,
                    dto.origins().stream().collect(Collectors.joining(",")));
        }

        RelyingPartyEntity relyingPartyEntity = RelyingPartyEntity.builder()
                .id(dto.id())
                .name(dto.name())
                .attestation(dto.attestation())
                .authenticatorAttachment(dto.authenticatorAttachment())
                .residentKey(dto.residentKey())
                .userVerification(dto.userVerification())
                .allowOriginPort(dto.allowOriginPort())
                .allowOriginSubdomain(dto.allowOriginSubdomain())
                .origins(toRelyingPartyOrigins(dto))
                .timeout(dto.timeout())
                .build();
        relyingPartyEntity.getOrigins().forEach(originEntity -> originEntity.setRelyingPartyEntity(relyingPartyEntity));

        RelyingPartyEntity entity = relyingPartyRepository.save(relyingPartyEntity);
        return entity;
    }

    public Optional<RelyingPartyEntity> findById(String id) {
        return relyingPartyRepository.findById(id);
    }

    public Optional<RelyingPartyEntity> removeById(String id) {
        Optional<RelyingPartyEntity> optional = relyingPartyRepository.findById(id);
        if(optional.isPresent()){
            relyingPartyRepository.deleteById(id);
        }
        return optional;
    }

    public Optional<RelyingPartyEntity> findByOrigin(String origin) {
        Optional<RelyingPartyOrigin> originOptional = relyingPartyOriginRepository.findDistinctByOrigin(origin);
        return originOptional.isEmpty() ? Optional.empty() : Optional.of(originOptional.get().getRelyingPartyEntity());
    }

    private Set<RelyingPartyOrigin> toRelyingPartyOrigins(RelyingPartyDto dto) {
        Set<String> origins = new LinkedHashSet<>();
        origins.add(dto.id());
        origins.addAll(dto.origins());
        return origins.stream().map(origin -> RelyingPartyOrigin.builder()
                .origin(origin)
                .build()).collect(Collectors.toSet());
    }
}
