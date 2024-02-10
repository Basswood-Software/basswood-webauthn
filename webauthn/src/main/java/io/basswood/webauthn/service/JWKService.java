package io.basswood.webauthn.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import io.basswood.webauthn.dto.JWKCreateDTO;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.jwk.JWKEntityConverter;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import io.basswood.webauthn.repository.JWKRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.basswood.webauthn.model.jwk.JWKEntity.ONE_MONTH_DURATION;

/**
 * @author shamualr
 * @since 1.0
 */
public class JWKService {
    private JWKRepository jwkRepository;
    private JWKEntityConverter converter;

    private NimbusJOSEHelper nimbusJOSEHelper;
    private LoadingCache<String, JWKEntity> keyCache;
    private JWK latestSignatureKey;
    private JWK latestEncryptionKey;

    public JWKService(JWKRepository jwkRepository) {
        this.jwkRepository = jwkRepository;
        this.converter = new JWKEntityConverter();
        this.nimbusJOSEHelper = new NimbusJOSEHelper();
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
        KeyUse keyUse = converter.toKeyUse(dto.getKeyUseEnum());
        JWK jwk = switch (dto.getKeyTypeEnum()) {
            case EC -> nimbusJOSEHelper.createECKey(keyUse, converter.toCurve(dto.getCurveEnum()), ONE_MONTH_DURATION);
            case RSA -> nimbusJOSEHelper.createRSAKey(keyUse, dto.getKeyLengthEnum().length(), ONE_MONTH_DURATION);
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
        return optional;
    }

    public JWKSet jwks() {
        List<JWKEntity> keys = jwkRepository.findAll();
        List<JWK> jwks = keys.stream()
                .map(jwkEntity -> converter.toJWK(jwkEntity))
                .collect(Collectors.toList());
        return new JWKSet(jwks);
    }

    /**
     * Returns the latest key for signature. If none found or expired then creates a new one and returns. It updates the
     * keyCache accordingly if needed.
     * @return
     */
    public JWK latestSignatureKey() {
        if(latestSignatureKey != null && latestSignatureKey.getExpirationTime().after(new Date())){
            return latestSignatureKey;
        }
        latestSignatureKey = null; // no key or the key is expired.
        Optional<JWKEntity> optionalJWK = jwkRepository.findFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc(KeyUseEnum.SIGNATURE, new Date());
        if(optionalJWK.isPresent()){
            latestSignatureKey = converter.toJWK(optionalJWK.get());
            keyCache.put(optionalJWK.get().getKid(), optionalJWK.get());
            return latestSignatureKey;
        }
        // none found. So create a new one
        return latestSignatureKey = createKey(JWKCreateDTO.builder().build());
    }

    /**
     * Returns the latest key for encryption. If none found or expired then creates a new one and returns. It updates the
     * keyCache accordingly if needed
     * @return
     */
    public JWK latestEncryptionKey() {
        if(latestEncryptionKey != null && latestEncryptionKey.getExpirationTime().after(new Date())){
            return latestEncryptionKey;
        }
        latestEncryptionKey = null; // no key or the key is expired.
        Optional<JWKEntity> optionalJWK = jwkRepository.findFirstByKeyUseAndExpiryTimeAfterOrderByExpiryTimeDesc(KeyUseEnum.ENCRYPTION, new Date());
        if(optionalJWK.isPresent()){
            latestEncryptionKey = converter.toJWK(optionalJWK.get());
            keyCache.put(optionalJWK.get().getKid(), optionalJWK.get());
            return latestEncryptionKey;
        }
        // none found. So create a new one
        return latestEncryptionKey = createKey(JWKCreateDTO.builder()
                .keyUseEnum(KeyUseEnum.ENCRYPTION)
                .build());
    }

    public void removeKey(String kid){
        Optional<JWKEntity> optional = jwkRepository.findDistinctByKid(kid);
        if(optional.isEmpty()){
            throw new EntityNotFound(JWKEntity.class, kid);
        }
        jwkRepository.delete(optional.get());
        keyCache.invalidate(kid);
        if(latestEncryptionKey!=null && kid.equals(latestEncryptionKey.getKeyID())){
            latestEncryptionKey = null;
        }
        if(latestSignatureKey!=null && kid.equals(latestSignatureKey.getKeyID())){
            latestSignatureKey = null;
        }
    }

    public JWK parse(String jwk){
        return nimbusJOSEHelper.parse(jwk);
    }
}
