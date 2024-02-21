package io.basswood.webauthn.service;


import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.ErrorCode;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.JWKEntityConverter;
import io.basswood.webauthn.model.jwk.KeyTypeEnum;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import io.basswood.webauthn.repository.BaseRepositoryIT;
import io.basswood.webauthn.repository.JWKRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static io.basswood.webauthn.model.jwk.JWKEntity.ONE_MONTH_DURATION;

public class JWKServiceIT extends BaseRepositoryIT {
    @Autowired
    private JWKRepository repository;
    private JWKService jwkService;
    private NimbusJOSEHelper nimbusJOSEHelper;

    @BeforeEach
    void setup() {
        jwkService = new JWKService(repository);
        nimbusJOSEHelper = new NimbusJOSEHelper();
    }

    @Test
    void testCreate() {
        //setup
        JWKCreateDTO createDTO = JWKCreateDTO.builder().build();
        // test
        JWK jwk = jwkService.createKey(createDTO);
        //assertions
        Assertions.assertEquals(KeyType.EC, jwk.getKeyType());
        Assertions.assertEquals(KeyUse.SIGNATURE, jwk.getKeyUse());
        Assertions.assertTrue(jwk instanceof ECKey);
        //cleanup
        repository.deleteById(jwk.getKeyID());
    }

    @Test
    void testCreateRSA() {
        //setup
        JWKCreateDTO createDTO = JWKCreateDTO.builder()
                .keyTypeEnum(KeyTypeEnum.RSA)
                .keyUseEnum(KeyUseEnum.ENCRYPTION)
                .build();
        // test
        JWK jwk = jwkService.createKey(createDTO);
        //assertions
        Assertions.assertEquals(KeyType.RSA, jwk.getKeyType());
        Assertions.assertEquals(KeyUse.ENCRYPTION, jwk.getKeyUse());
        Assertions.assertTrue(jwk instanceof RSAKey);
        //cleanup
        repository.deleteById(jwk.getKeyID());
    }

    @Test
    void testSaveKey(){
        ECKey ecKey = nimbusJOSEHelper.createECKey(KeyUse.SIGNATURE, Curve.P_256, ONE_MONTH_DURATION);
        JWKEntity entity = jwkService.saveKey(ecKey);
        Assertions.assertEquals(KeyTypeEnum.EC, entity.getKty());
        Assertions.assertEquals(KeyUseEnum.SIGNATURE, entity.getKeyUse());
        Assertions.assertEquals(ecKey.getKeyID(), entity.getKid());
        Assertions.assertEquals(ecKey.toJSONString(), entity.getJwkData());
        repository.deleteById(ecKey.getKeyID());
    }
    @Test
    void testSaveKey_DUPLICATE(){
        JWK ecKey = jwkService.createKey(JWKCreateDTO.builder().build());
        DuplicateEntityFound duplicateEntityFound = Assertions.assertThrows(DuplicateEntityFound.class, () -> jwkService.saveKey(ecKey));
        Assertions.assertEquals(ErrorCode.duplicate_entity, duplicateEntityFound.getErrorCode());
        Assertions.assertEquals(HttpStatus.CONFLICT.value(), duplicateEntityFound.getHttpStatus());
    }

    @Test
    void testGetJWKEntity_CACHE_HIT() {
        // create and save a key with jwkService so that the key is in cache.
        JWK ecKey = jwkService.createKey(JWKCreateDTO.builder().build());
        JWKEntity entity = new JWKEntityConverter().toEntity(ecKey);
        JWKEntity saved = repository.save(entity);

        // Now get the entity
        Optional<JWKEntity> optional = jwkService.getJWKEntity(ecKey.getKeyID());
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(ecKey.getKeyID(), optional.get().getKid());

        //cleanup
        repository.deleteById(ecKey.getKeyID());
    }

    @Test
    void testGetJWKEntity_CACHE_MISS() {
        // create and save a key in db with repository, without hitting JWKService cache.
        ECKey ecKey = createKeyInDb(KeyUse.SIGNATURE);

        // Now get the entity
        Optional<JWKEntity> optional = jwkService.getJWKEntity(ecKey.getKeyID());
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(ecKey.getKeyID(), optional.get().getKid());

        //cleanup
        repository.deleteById(ecKey.getKeyID());
    }

    @Test
    void testGetJWKEntity_NO_KEY_FOUND() {
        String kid = UUID.randomUUID().toString();
        Optional<JWKEntity> optional = jwkService.getJWKEntity(kid);
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    void testLatestSignatureKey() {
        // create and save a key in db with repository, without hitting JWKService cache.
        ECKey ecKey = createKeyInDb(KeyUse.SIGNATURE);
        // test
        JWK jwk = jwkService.latestSignatureKey();
        Assertions.assertEquals(ecKey.getKeyID(),jwk.getKeyID());

        // load from cache
        jwk = jwkService.latestSignatureKey();
        Assertions.assertEquals(ecKey.getKeyID(),jwk.getKeyID());

        //cleanup
        repository.deleteById(ecKey.getKeyID());
    }
    @Test
    void testLatestSignatureKey_NONE_EXITS() {
        // cleanup db
        repository.deleteAll();
        // test
        JWK jwk = jwkService.latestSignatureKey();
        Assertions.assertTrue(jwk != null);
        Assertions.assertEquals(KeyType.EC, jwk.getKeyType());
        //cleanup
        repository.deleteAll();
    }

    @Test
    void testLatestEncryptionKey() {
        // create and save a key in db with repository, without hitting JWKService cache.
        ECKey ecKey = createKeyInDb(KeyUse.ENCRYPTION);

        // test
        JWK jwk = jwkService.latestEncryptionKey();
        Assertions.assertEquals(ecKey.getKeyID(),jwk.getKeyID());

        // load from cache
        jwk = jwkService.latestEncryptionKey();
        Assertions.assertEquals(ecKey.getKeyID(),jwk.getKeyID());

        //cleanup
        repository.deleteById(ecKey.getKeyID());
    }
    @Test
    void testLatestEncryptionKey_NONE_EXITS() {
        // cleanup db
        repository.deleteAll();
        // test
        JWK jwk = jwkService.latestEncryptionKey();
        Assertions.assertTrue(jwk != null);
        Assertions.assertEquals(KeyType.EC, jwk.getKeyType());
        //cleanup
        repository.deleteAll();
    }

    @Test
    void testRemoveKey(){
        ECKey ecKey = createKeyInDb(KeyUse.SIGNATURE);
        jwkService.removeKey(ecKey.getKeyID());
        Optional<JWKEntity> optional = jwkService.getJWKEntity(ecKey.getKeyID());
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    void testRemoveKey_NOT_FOUND(){
        String kid = UUID.randomUUID().toString();
        EntityNotFound entityNotFound = Assertions.assertThrows(EntityNotFound.class, () -> jwkService.removeKey(kid));
        Assertions.assertEquals(ErrorCode.not_found, entityNotFound.getErrorCode());
    }

    private ECKey createKeyInDb(KeyUse keyUse){
        ECKey ecKey = nimbusJOSEHelper.createECKey(keyUse, Curve.P_256, ONE_MONTH_DURATION);
        JWKEntity entity = new JWKEntityConverter().toEntity(ecKey);
        JWKEntity saved = repository.save(entity);
        return ecKey;
    }
}