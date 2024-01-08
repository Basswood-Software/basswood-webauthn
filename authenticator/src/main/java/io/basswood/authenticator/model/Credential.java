package io.basswood.authenticator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nimbusds.jose.jwk.JWK;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.dto.CredentialDeserializer;
import io.basswood.authenticator.dto.CredentialSerializer;
import io.basswood.authenticator.service.KeySerializationSupport;

import java.util.Objects;

@JsonSerialize(using = CredentialSerializer.class)
@JsonDeserialize(using = CredentialDeserializer.class)
public class Credential<T extends JWK> implements Comparable<Credential> {
    private ByteArray credentialId;
    private ByteArray userId;
    private ByteArray rpId;
    private T key;

    @JsonCreator
    public Credential(@JsonProperty ByteArray credentialId, @JsonProperty ByteArray userId, @JsonProperty ByteArray rpId, @JsonProperty T key) {
        Objects.requireNonNull(credentialId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(rpId);
        Objects.requireNonNull(key);
        this.credentialId = credentialId;
        this.userId = userId;
        this.rpId = rpId;
        this.key = key;
    }

    public Credential(ByteArray userId, ByteArray rpId, Class<T> type) {
        this(credentialId(userId, rpId), userId, rpId, KeySerializationSupport.randomKeyPair(type));
    }

    public ByteArray getCredentialId() {
        return credentialId;
    }

    public ByteArray getUserId() {
        return userId;
    }

    public ByteArray getRpId() {
        return rpId;
    }

    public JWK getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credential that = (Credential) o;
        return credentialId.equals(that.credentialId);
    }

    @Override
    public int hashCode() {
        return credentialId.hashCode();
    }

    public static ByteArray credentialId(ByteArray userId, ByteArray rpId) {
        return userId.concat(rpId);
    }

    @Override
    public int compareTo(Credential o) {
        return getCredentialId().compareTo(o.getCredentialId());
    }
}