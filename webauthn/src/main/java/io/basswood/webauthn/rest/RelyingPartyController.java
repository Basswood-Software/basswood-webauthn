package io.basswood.webauthn.rest;

import io.basswood.webauthn.dto.RelyingPartyDto;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.service.RelyingPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class RelyingPartyController {

    private RelyingPartyService relyingPartyService;

    public RelyingPartyController(@Autowired RelyingPartyService relyingPartyService) {
        this.relyingPartyService = relyingPartyService;
    }

    @PostMapping("/relying-party")
    public RelyingPartyDto register(@RequestBody RelyingPartyDto dto) {
        RelyingPartyEntity entity = relyingPartyService.createNewRelyingParty(dto);
        return toRelyingPartyRecordDto(entity);
    }

    @GetMapping("/relying-party/{id}")
    public RelyingPartyDto findById(@PathVariable String id) {
        RelyingPartyEntity entity = relyingPartyService.findById(id)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, id));
        return toRelyingPartyRecordDto(entity);
    }

    @DeleteMapping("/relying-party/{id}")
    public RelyingPartyDto removeById(@PathVariable String id) {
        RelyingPartyEntity relyingPartyEntity = relyingPartyService.removeById(id)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, id));
        return toRelyingPartyRecordDto(relyingPartyEntity);
    }

    private RelyingPartyDto toRelyingPartyRecordDto(RelyingPartyEntity entity) {
        return new RelyingPartyDto(
                entity.getId(),
                entity.getName(),
                entity.getAttestation(),
                entity.getAuthenticatorAttachment(),
                entity.getResidentKey(),
                entity.getUserVerification(),
                entity.getAllowOriginPort(),
                entity.getAllowOriginSubdomain(),
                entity.getTimeout(),
                entity.getOrigins().stream().map(origin -> origin.getOrigin()).collect(Collectors.toSet())
        );
    }
}