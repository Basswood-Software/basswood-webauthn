package io.basswood.authenticator.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.yubico.webauthn.data.AuthenticatorAttachment.PLATFORM;
import static com.yubico.webauthn.data.AuthenticatorTransport.INTERNAL;
import static com.yubico.webauthn.data.COSEAlgorithmIdentifier.ES256;
import static com.yubico.webauthn.data.COSEAlgorithmIdentifier.RS256;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = AuthenticatorCreateDTODeserializer.class)
public class AuthenticatorCreateDTO {
    @Builder.Default
    private int signatureCount = 0;
    @Builder.Default
    private AuthenticatorAttachment attachment = PLATFORM;
    @Builder.Default
    private AuthenticatorTransport transport = INTERNAL;
    @Builder.Default
    private Set<COSEAlgorithmIdentifier> supportedAlgorithms = new LinkedHashSet<>(Arrays.asList(RS256, ES256));
}
