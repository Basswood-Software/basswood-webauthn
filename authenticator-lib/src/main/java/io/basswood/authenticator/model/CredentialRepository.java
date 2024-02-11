package io.basswood.authenticator.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.dto.CredentialRepositoryDeserializer;
import io.basswood.authenticator.dto.CredentialRepositorySerializer;
import io.basswood.authenticator.exception.RootException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@JsonSerialize(using = CredentialRepositorySerializer.class)
@JsonDeserialize(using = CredentialRepositoryDeserializer.class)
public class CredentialRepository {
    private Map<ByteArray, Credential> repository;

    public CredentialRepository() {
        this(new LinkedHashMap<>());
    }

    public CredentialRepository(Map<ByteArray, Credential> repository) {
        this.repository = new LinkedHashMap<>();
        this.repository.putAll(repository);
    }

    public Set<ByteArray> getCredentialIds() {
        if (repository == null || repository.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ByteArray> ids = new LinkedHashSet<>();
        for (ByteArray id : repository.keySet()) {
            ids.add(id);
        }
        return ids;
    }

    public void add(Credential credential) {
        if (repository.containsKey(credential.getCredentialId())) {
            throw new RootException("credential already exists");
        }
        repository.put(credential.getCredentialId(), credential);
    }

    public Optional<Credential> findCredential(ByteArray credentialId) {
        Credential credential = repository.get(credentialId);
        Optional<Credential> optional = Optional.ofNullable(credential);
        return optional;
    }

    public Optional<Credential> findCredential(ByteArray userId, ByteArray rpId) {
        Credential credential = repository.get(Credential.credentialId(userId, rpId));
        Optional<Credential> optional = Optional.ofNullable(credential);
        return optional;
    }

    public Optional<Credential> removeCredential(ByteArray userId, ByteArray rpId) {
        Credential credential = repository.remove(Credential.credentialId(userId, rpId));
        Optional<Credential> optional = Optional.ofNullable(credential);
        return optional;
    }
}
