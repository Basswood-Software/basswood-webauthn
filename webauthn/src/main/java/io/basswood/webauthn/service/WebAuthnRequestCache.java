package io.basswood.webauthn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.basswood.webauthn.exception.DuplicateEntityFound;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.request.RequestType;
import io.basswood.webauthn.model.request.WebAuthnRequestEntity;
import io.basswood.webauthn.repository.WebAuthnRequestRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author shamualr
 * @since 1.0
 */
@Slf4j
public class WebAuthnRequestCache {
    private WebAuthnRequestRepository repository;
    private LoadingCache<String, WebAuthnRequestEntity> requestCache;

    public WebAuthnRequestCache(WebAuthnRequestRepository repository) {
        this.repository = repository;
        this.requestCache = CacheBuilder.newBuilder().expireAfterWrite(WebAuthnRequestEntity.DEFAULT_REQUEST_TIMEOUT.toMinutes(), TimeUnit.MINUTES).maximumSize(1000).build(new CacheLoader<>() {
            @Override
            public WebAuthnRequestEntity load(String requestId) {
                Optional<WebAuthnRequestEntity> entity = repository.findById(requestId);
                if (entity.isPresent()) {
                    return entity.get();
                } else {
                    throw new EntityNotFound(WebAuthnRequestEntity.class, requestId);
                }
            }
        });
    }

    public <R> void saveRequest(String requestId, R request) {
        Optional<WebAuthnRequestEntity> byId = repository.findById(requestId);
        if (byId.isPresent()) {
            throw new DuplicateEntityFound(request.getClass(), requestId);
        }
        WebAuthnRequestEntity requestEntity = buildEntity(requestId, request);
        repository.save(requestEntity);
        requestCache.put(requestId, requestEntity);
    }

    public <R> R loadRequest(String requestId, Class<R> requestClass) {
        WebAuthnRequestEntity webAuthnRequestEntity;
        try {
            webAuthnRequestEntity = requestCache.get(requestId);
        } catch (ExecutionException | UncheckedExecutionException e) {
            switch (e.getCause()) {
                case EntityNotFound entityNotFound -> throw entityNotFound;
                case null -> throw new RootException(e);
                default -> throw new RootException(e.getCause());
            }
        }
        RequestType expectedRequestType = requestType(requestClass);
        if (webAuthnRequestEntity.getRequestType() != expectedRequestType) {
            throw new EntityNotFound(WebAuthnRequestEntity.class, requestId);
        }
        return fromJson(webAuthnRequestEntity.getRequest(), requestClass);
    }

    private <R> WebAuthnRequestEntity buildEntity(String requestId, R payLoad) {
        return WebAuthnRequestEntity.builder()
                .requestId(requestId)
                .requestType(requestType(payLoad))
                .expiryTime(expiryTime(payLoad))
                .request(toJson(payLoad))
                .build();
    }

    public <R> RequestType requestType(R payLoad) {
        return switch (payLoad) {
            case PublicKeyCredentialCreationOptions ignored -> RequestType.REGISTRATION;
            case AssertionRequest ignored -> RequestType.ASSERTION;
            default -> throw new RootException("Request type:" + payLoad.getClass().getName() + " not supported.");
        };
    }

    public <R> RequestType requestType(Class<R> requestClass) {
        if (requestClass == PublicKeyCredentialCreationOptions.class) {
            return RequestType.REGISTRATION;
        } else if (requestClass == AssertionRequest.class) {
            return RequestType.ASSERTION;
        } else {
            throw new RootException("Request type:" + requestClass.getName() + " not supported.");
        }
    }

    public <R> R fromJson(String json, Class<R> payloadClass) {
        try {
            if (PublicKeyCredentialCreationOptions.class == payloadClass) {
                return (R) PublicKeyCredentialCreationOptions.fromJson(json);
            } else if (AssertionRequest.class == payloadClass) {
                return (R) AssertionRequest.fromJson(json);
            } else {
                throw new RootException("Request type: " + payloadClass.getName() + " not supported.");
            }
        } catch (JsonProcessingException e) {
            String message = "Failed to parse request payload";
            log.debug(message, e);
            throw new RootException(message, e);
        }
    }

    public <R> String toJson(R request) {
        try {
            return switch (request) {
                case PublicKeyCredentialCreationOptions creationOptions -> creationOptions.toJson();
                case AssertionRequest assertionRequest -> assertionRequest.toJson();
                default -> throw new RootException("Request type: " + request.getClass().getName() + " not supported.");
            };
        } catch (JsonProcessingException e) {
            String message = "Failed to serialize request payload";
            log.debug(message, e);
            throw new RootException(message, e);
        }
    }

    public <R> Date expiryTime(R request) {
        Optional<Long> optionalTimeout = switch (request) {
            case PublicKeyCredentialCreationOptions creationOptions -> creationOptions.getTimeout();
            case AssertionRequest assertionRequest ->
                    assertionRequest.getPublicKeyCredentialRequestOptions().getTimeout();
            default -> throw new RootException("Request type:" + request.getClass().getName() + " not supported.");
        };
        return optionalTimeout.isPresent() ? Date.from(Instant.now().plusMillis(optionalTimeout.get())) : Date.from(Instant.now().plusMillis(WebAuthnRequestEntity.DEFAULT_REQUEST_TIMEOUT.toMillis()));
    }
}
