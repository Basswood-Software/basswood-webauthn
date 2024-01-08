package io.basswood.webauthn.dto;

import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.rp.AuthenticatorPreference;
import lombok.Builder;

import java.util.Set;

@Builder
public record RelyingPartyDto(
        String id,
        String name,
        AuthenticatorPreference.Attestation attestation,
        AuthenticatorPreference.Attachment authenticatorAttachment,
        AuthenticatorPreference.ResidentKey residentKey,
        AuthenticatorPreference.UserVerification userVerification,
        Boolean allowOriginPort,
        Boolean allowOriginSubdomain,
        Long timeout,
        Set<String> origins
) {
    public RelyingPartyDto {
        authenticatorAttachment = authenticatorAttachment == null ? AuthenticatorPreference.Attachment.PLATFORM : authenticatorAttachment;
        residentKey = residentKey == null ? AuthenticatorPreference.ResidentKey.DISCOURAGED : residentKey;
        userVerification = userVerification == null ? AuthenticatorPreference.UserVerification.DISCOURAGED : userVerification;
        allowOriginPort = allowOriginPort == null ? true : allowOriginPort;
        allowOriginSubdomain = allowOriginSubdomain == null ? true : allowOriginSubdomain;
        timeout = timeout == null ? 60000L : timeout;
        if (origins == null || origins.isEmpty()) {
            throw new RootException("RelyingParty origins cannot be null or empty");
        }
    }
}
