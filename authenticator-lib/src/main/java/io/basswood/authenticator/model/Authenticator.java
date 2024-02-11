package io.basswood.authenticator.model;

import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface Authenticator {
    String CLIENT_DATA_CREATE = "webauthn.create";
    String CLIENT_DATA_GET = "webauthn.get";


    PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> create(PublicKeyCredentialCreationOptions options);

    PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> get(PublicKeyCredentialRequestOptions options);

    AuthenticatorAttachment getAttachment();

    CredentialRepository getRepository();

    UUID getAaguid();

    default Set<COSEAlgorithmIdentifier> supportedAlgorithms() {
        return new LinkedHashSet<>(Arrays.asList(COSEAlgorithmIdentifier.RS256, COSEAlgorithmIdentifier.ES256));
    }

    default boolean matches(PublicKeyCredentialCreationOptions options) {
        // check attachment
        if (options.getAuthenticatorSelection().isPresent()) {
            if (options.getAuthenticatorSelection().get().getAuthenticatorAttachment().isPresent()) {
                if (getAttachment() != options.getAuthenticatorSelection().get().getAuthenticatorAttachment().get()) {
                    return false;
                }
            }
        }
        // check supported algorithms
        Optional<PublicKeyCredentialParameters> optional = options.getPubKeyCredParams()
                .stream().filter(t -> supportedAlgorithms().contains(t.getAlg())).findFirst();
        return optional.isPresent();
    }

    default Set<Credential> matchedCredentials(PublicKeyCredentialRequestOptions options) {
        if (!options.getAllowCredentials().isPresent()) {
            return Collections.emptySet();
        }
        if (options.getAllowCredentials().get().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Credential> credentials = options.getAllowCredentials().get().stream()
                .filter(t -> getRepository().findCredential(t.getId()).isPresent())
                .map(t -> getRepository().findCredential(t.getId()).get())
                .collect(Collectors.toSet());

        return credentials;
    }
}