package io.basswood.webauthn.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;

import java.util.concurrent.TimeUnit;

public class CacheService {
    private Cache<String, PublicKeyCredentialCreationOptions> createOptions;
    private Cache<String, AssertionRequest> assertionOptions;

    public CacheService() {
        createOptions = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        assertionOptions = CacheBuilder.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public void put(String key, PublicKeyCredentialCreationOptions creationOptions) {
        createOptions.put(key, creationOptions);
    }

    public PublicKeyCredentialCreationOptions getCreateOptions(String key) {
        return createOptions.getIfPresent(key);
    }

    public void invalidateCreateOptions(String key) {
        createOptions.invalidate(key);
    }

    public void put(String key, AssertionRequest assertionRequest) {
        assertionOptions.put(key, assertionRequest);
    }

    public AssertionRequest getAssertionRequest(String key) {
        return assertionOptions.getIfPresent(key);
    }

    public void invalidateAssertionRequest(String key) {
        assertionOptions.invalidate(key);
    }
}
