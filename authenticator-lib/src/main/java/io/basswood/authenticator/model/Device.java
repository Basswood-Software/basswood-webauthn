package io.basswood.authenticator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import io.basswood.authenticator.dto.DeviceDeserializer;
import io.basswood.authenticator.dto.DeviceSerializer;
import io.basswood.authenticator.exception.AuthenticatorException;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.basswood.authenticator.exception.AuthenticatorException.ERROR_CODE_DUPLICATE_ENTITY;

@JsonSerialize(using = DeviceSerializer.class)
@JsonDeserialize(using = DeviceDeserializer.class)
public class Device {

    @Getter
    private UUID deviceId;

    @Getter
    private String displayName;

    @Getter
    private Set<String> tags;

    @Getter
    private Map<UUID, VirtualAuthenticator> authenticators;


    @Builder
    @JsonCreator
    public Device(UUID deviceId, String displayName, Set<String> tags, Map<UUID, VirtualAuthenticator> authenticators) {
        this.deviceId = deviceId;
        this.displayName = displayName;
        this.tags = new LinkedHashSet<>();
        if (tags != null && !tags.isEmpty()) {
            this.tags = tags.stream().collect(Collectors.toSet());
        }
        this.authenticators = new LinkedHashMap<>();
        if (authenticators != null && !authenticators.isEmpty()) {
            authenticators.entrySet().stream().forEach(entry -> this.authenticators.put(entry.getKey(), entry.getValue()));
        }
    }

    public Optional<VirtualAuthenticator> getVirtualAuthenticator(UUID key) {
        return Optional.ofNullable(authenticators.get(key));
    }

    public VirtualAuthenticator addAuthenticator(VirtualAuthenticator authenticator) {
        if (authenticators.containsKey(authenticator.getAaguid())) {
            throw new AuthenticatorException("A virtual authenticator with the same id: " + authenticator.getAaguid().toString() + " already exists",
                    null, ERROR_CODE_DUPLICATE_ENTITY, 409);
        }
        authenticators.put(authenticator.getAaguid(), authenticator);
        return authenticator;
    }

    public Optional<VirtualAuthenticator> remove(UUID aaguid) {
        if (!authenticators.containsKey(aaguid)) {
            return Optional.empty();
        }
        VirtualAuthenticator authenticator = authenticators.get(aaguid);
        authenticators.remove(aaguid);
        return Optional.of(authenticator);
    }

    public void update(VirtualAuthenticator authenticator) {
        authenticators.put(authenticator.getAaguid(), authenticator);
    }

    public Set<VirtualAuthenticator> authenticators() {
        return (authenticators == null || authenticators.isEmpty()) ? Collections.EMPTY_SET : this.authenticators.entrySet().stream().map(t -> t.getValue()).collect(Collectors.toSet());
    }

    public void importAuthenticators(Set<VirtualAuthenticator> authenticators) {
        if (authenticators == null || authenticators.isEmpty()) {
            return;
        }
        this.authenticators.clear();
        for (VirtualAuthenticator authenticator : authenticators) {
            this.authenticators.put(authenticator.getAaguid(), authenticator);
        }
    }

    public Map<UUID, VirtualAuthenticator> clear() {
        Map<UUID, VirtualAuthenticator> removedItems = new LinkedHashMap<>();
        for (UUID uuid : authenticators.keySet()) {
            removedItems.put(uuid, authenticators.get(uuid));
        }
        authenticators.clear();
        return removedItems;
    }

    public Set<VirtualAuthenticator> matchForRegistration(PublicKeyCredentialCreationOptions options) {
        Set<VirtualAuthenticator> result = authenticators();
        Iterator<VirtualAuthenticator> iterator = result.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().matchForRegistration(options)) {
                iterator.remove();
            }
        }
        return result;
    }

    public Set<VirtualAuthenticator> matchForAssertion(PublicKeyCredentialRequestOptions options) {
        Set<VirtualAuthenticator> result = authenticators();
        Iterator<VirtualAuthenticator> iterator = result.iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().matchForAssertion(options)) {
                iterator.remove();
            }
        }
        return result;
    }
}
