package io.basswood.webauthn.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.JWKException;
import io.basswood.webauthn.model.jwk.CurveEnum;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.JWKEntityConverter;
import io.basswood.webauthn.model.jwk.KeyLengthEnum;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import io.basswood.webauthn.repository.JWKRepository;
import jakarta.validation.constraints.NotNull;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.basswood.webauthn.model.jwk.JWKEntity.ONE_WEEK_IN_MILLIS;

/**
 * @author shamualr
 * @since 1.0
 */
public class JWKService {
    private JWKRepository jwkRepository;
    private JWKEntityConverter converter;
    private LoadingCache<String, JWKEntity> keyCache;

    public JWKService(JWKRepository jwkRepository) {
        this.jwkRepository = jwkRepository;
        this.converter = new JWKEntityConverter();
        keyCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(1000)
                .build(new CacheLoader<>() {
                    @Override
                    public JWKEntity load(String kid) {
                        Optional<JWKEntity> entity = jwkRepository.findDistinctByKid(kid);
                        if(entity.isPresent()){
                            return entity.get();
                        }else{
                            throw new EntityNotFound(JWKEntity.class, kid);
                        }
                    }
                });
    }


    public JWK createKey(JWKCreateDTO dto) {
        JWK jwk = switch (dto.getKeyTypeEnum()) {
            case EC -> createECKey(dto.getKeyUseEnum(), dto.getCurveEnum());
            case RSA -> createRSAKey(dto.getKeyUseEnum(), dto.getKeyLengthEnum());
        };
        JWKEntity entity = converter.toEntity(jwk);
        jwkRepository.save(entity);
        keyCache.put(entity.getKid(), entity);
        return jwk;
    }

    public JWKEntity saveKey(JWK jwk){
        JWKEntity entity = converter.toEntity(jwk);
        Optional<JWKEntity> optional = jwkRepository.findDistinctByKid(entity.getKid());
        if(optional.isPresent()){
            throw new DuplicateEntityFound(JWKEntity.class, entity.getKid());
        }
        JWKEntity saved = jwkRepository.save(entity);
        keyCache.put(saved.getKid(), saved);
        return saved;
    }

    private RSAKey createRSAKey(@NotNull KeyUseEnum keyUseEnum, @NotNull KeyLengthEnum keyLengthEnum) {
        try {
            return new RSAKeyGenerator(keyLengthEnum.length())
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(converter.toKeyUse(keyUseEnum))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLIS))
                    .generate();
        } catch (JOSEException e) {
            throw new JWKException("Failed to create RSA Key", e);
        }
    }

    private ECKey createECKey(KeyUseEnum keyUseEnum, CurveEnum curveEnum) {
        try {
            return new ECKeyGenerator(converter.toCurve(curveEnum))
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(converter.toKeyUse(keyUseEnum))
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + ONE_WEEK_IN_MILLIS))
                    .generate();
        } catch (JOSEException e) {
            throw new JWKException("Failed to create RSA Key", e);
        }
    }

    public Optional<JWKEntity> getJWKEntity(String kid) {
        // check cache first
        JWKEntity jwkEntity = keyCache.getIfPresent(kid);
        if(jwkEntity != null){
            return Optional.of(jwkEntity);
        }
        // Check database now.
        Optional<JWKEntity> optional = jwkRepository.findDistinctByKid(kid);
        if(optional.isPresent()){ // update cache
            keyCache.put(kid, optional.get());
            return optional;
        }
        return optional;    }

    public JWKSet jwks() {
        List<JWKEntity> keys = jwkRepository.findAll();
        List<JWK> jwks = keys.stream()
                .map(jwkEntity -> converter.toJWK(jwkEntity))
                .collect(Collectors.toList());
        return new JWKSet(jwks);
    }

    public void removeKey(String kid){
        Optional<JWKEntity> optional = jwkRepository.findDistinctByKid(kid);
        if(optional.isEmpty()){
            throw new EntityNotFound(JWKEntity.class, kid);
        }
        jwkRepository.delete(optional.get());
        keyCache.invalidate(kid);
    }

    public JWK parse(String jwk){
        try {
            return JWK.parse(jwk);
        } catch (ParseException e) {
            throw new JWKException("Failed to parse JWK key", e);
        }
    }
}
