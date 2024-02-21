package io.basswood.webauthn.service;


import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.KeyTypeEnum;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import io.basswood.webauthn.repository.JWKRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JWKServiceTest {
    private JWKRepository repository;
    private JWKService jwkService;

    @BeforeEach
    void setup(){
        repository = mock(JWKRepository.class);
        jwkService = new JWKService(repository);
    }

    @AfterEach
    void cleanup(){
        Mockito.reset(repository);
    }

    @Test
    void testCreate(){
        JWKEntity entity = JWKEntity.builder().build();
        when(repository.save(ArgumentMatchers.any(JWKEntity.class))).thenReturn(entity);
        JWK jwk = jwkService.createKey(JWKCreateDTO.builder().build());
        verify(repository, times(1)).save(ArgumentMatchers.any(JWKEntity.class));
        Assertions.assertEquals(KeyType.EC ,jwk.getKeyType());
        Assertions.assertEquals(KeyUse.SIGNATURE ,jwk.getKeyUse());
    }

    @Test
    void testCreateRSA(){
        JWKEntity entity = JWKEntity.builder().build();
        when(repository.save(ArgumentMatchers.any(JWKEntity.class))).thenReturn(entity);
        JWKCreateDTO createDTO = JWKCreateDTO.builder()
                .keyTypeEnum(KeyTypeEnum.RSA)
                .keyUseEnum(KeyUseEnum.ENCRYPTION)
                .build();
        JWK jwk = jwkService.createKey(createDTO);
        verify(repository, times(1)).save(ArgumentMatchers.any(JWKEntity.class));
        Assertions.assertEquals(KeyType.RSA ,jwk.getKeyType());
        Assertions.assertEquals(KeyUse.ENCRYPTION ,jwk.getKeyUse());
    }
}