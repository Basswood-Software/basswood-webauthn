package io.basswood.webauthn.rest;

import com.nimbusds.jose.jwk.JWK;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.JWKEntityConverter;
import io.basswood.webauthn.service.JWKService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class JWKController {
    private static final JWKCreateDTO DEFAULT_JWK_CREATE_DTO = JWKCreateDTO.builder().build();
    private JWKService jwkService;
    private JWKEntityConverter converter;

    public JWKController(JWKService jwkService) {
        this.jwkService = jwkService;
        this.converter = new JWKEntityConverter();
    }

    @PostMapping(value = "/jwk", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createJWK(
            @RequestBody(required = false) JWKCreateDTO createDTO,
            @RequestParam(name = "returnPublicKeyOnly", required = false, defaultValue = "true") Boolean returnPublicKeyOnly) {
        JWK jwk = jwkService.createKey(createDTO != null ? createDTO : DEFAULT_JWK_CREATE_DTO);
        return returnPublicKeyOnly ? jwk.toPublicJWK().toJSONString() : jwk.toJSONString();
    }

    @GetMapping(value = "/jwk/{kid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getJWKById(@PathVariable("kid") String kid,
                          @RequestParam(name = "returnPublicKeyOnly", required = false, defaultValue = "true") Boolean returnPublicKeyOnly) {
        Optional<JWKEntity> optional = jwkService.getJWKEntity(kid);
        if (optional.isEmpty()) {
            throw new EntityNotFound(JWKEntity.class, kid);
        }
        JWK jwk = converter.toJWK(optional.get());
        return returnPublicKeyOnly ? jwk.toPublicJWK().toJSONString() : jwk.toJSONString();
    }

    @DeleteMapping(value = "/jwk/{kid}")
    public ResponseEntity removeKey(@PathVariable String kid){
        jwkService.removeKey(kid);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
