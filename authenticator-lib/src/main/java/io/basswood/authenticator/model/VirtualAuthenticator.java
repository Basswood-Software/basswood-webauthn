package io.basswood.authenticator.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.upokecenter.cbor.CBORObject;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialParameters;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import io.basswood.authenticator.dto.VirtualAuthenticatorDeserializer;
import io.basswood.authenticator.dto.VirtualAuthenticatorSerializer;
import io.basswood.authenticator.exception.AlgorithmMismatch;
import io.basswood.authenticator.exception.BadRequest;
import io.basswood.authenticator.exception.ConflictException;
import io.basswood.authenticator.exception.CredentialMismatch;
import io.basswood.authenticator.service.ByteArrayConverter;
import io.basswood.authenticator.service.CryptoUtil;
import io.basswood.authenticator.service.KeySerializationSupport;
import lombok.Builder;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.yubico.webauthn.data.COSEAlgorithmIdentifier.ES256;
import static com.yubico.webauthn.data.COSEAlgorithmIdentifier.RS256;
import static io.basswood.authenticator.service.CryptoUtil.sha256;

@JsonSerialize(using = VirtualAuthenticatorSerializer.class)
@JsonDeserialize(using = VirtualAuthenticatorDeserializer.class)
public class VirtualAuthenticator implements Authenticator {
    @Getter
    private UUID aaguid;

    @Getter
    private int signatureCount;

    @Getter
    private AuthenticatorAttachment attachment;

    @Getter
    private AuthenticatorTransport authenticatorTransport;

    private Set<COSEAlgorithmIdentifier> supportedAlgorithms;

    @Getter
    private JWK key;
    @Getter
    private CredentialRepository repository;

    private ByteArrayConverter byteArrayConverter;

    @Builder
    public VirtualAuthenticator(UUID aaguid, AuthenticatorAttachment attachment, AuthenticatorTransport authenticatorTransport, JWK key, Set<COSEAlgorithmIdentifier> supportedAlgorithms, Integer signatureCount) {
        this.aaguid = aaguid;
        this.attachment = attachment;
        this.authenticatorTransport = authenticatorTransport;
        this.key = key;
        this.supportedAlgorithms = (supportedAlgorithms != null) ?
                supportedAlgorithms :
                new LinkedHashSet<>(Arrays.asList(RS256, ES256));
        this.repository = new CredentialRepository();
        this.signatureCount = signatureCount != null ? signatureCount.intValue() : 0;
        this.byteArrayConverter = new ByteArrayConverter() {
        };
    }

    @Override
    public Set<COSEAlgorithmIdentifier> supportedAlgorithms() {
        return supportedAlgorithms == null || supportedAlgorithms.isEmpty()
                ? Authenticator.super.supportedAlgorithms()
                : supportedAlgorithms;
    }

    public PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> create(PublicKeyCredentialCreationOptions options) {
        ByteArray userId = options.getUser().getId();
        ByteArray rpId = new ByteArray(options.getRp().getId().getBytes(StandardCharsets.UTF_8));
        Optional<Credential> credentialOptional = repository.findCredential(userId, rpId);
        if (credentialOptional.isPresent()) {
            throw new ConflictException("Credential already present for rpId: " + rpId + " and userId: " + userId);
        }
        Set<PublicKeyCredentialParameters> matchedAlgorithms = options.getPubKeyCredParams().stream()
                .filter(t -> supportedAlgorithms.contains(t.getAlg()))
                .collect(Collectors.toSet());
        if (matchedAlgorithms.isEmpty()) {
            throw new AlgorithmMismatch(getAaguid());
        }
        Credential credential = new Credential(userId, rpId, ECKey.class);
        repository.add(credential);

        return PublicKeyCredential.<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>builder()
                .id(credential.getCredentialId())
                .response(authenticatorAttestationResponse(options, credential))
                .clientExtensionResults(ClientRegistrationExtensionOutputs.builder().build())
                .build();
    }

    public PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> get(PublicKeyCredentialRequestOptions options) {
        Set<Credential> credentials = matchedCredentials(options);
        if (credentials.isEmpty()) {
            throw new CredentialMismatch(getAaguid());
        }
        Credential credential = credentials.iterator().next();
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> publicKeyCredential = PublicKeyCredential
                .<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>builder()
                .id(credential.getCredentialId())
                .response(authenticatorAssertionResponse(options, credential))
                .clientExtensionResults(ClientAssertionExtensionOutputs.builder().build())
                .build();
        ++signatureCount;
        return publicKeyCredential;
    }

    public boolean matchForRegistration(PublicKeyCredentialCreationOptions options) {
        if (options.getAuthenticatorSelection().isPresent()) {
            AuthenticatorSelectionCriteria selectionCriteria = options.getAuthenticatorSelection().get();
            if (selectionCriteria.getAuthenticatorAttachment().isPresent()) {
                AuthenticatorAttachment attachment = selectionCriteria.getAuthenticatorAttachment().get();
                if (getAttachment() != attachment) {
                    return false;
                }
            }
        }
        List<PublicKeyCredentialParameters> credParams = options.getPubKeyCredParams();
        if (credParams != null && !credParams.isEmpty()) {
            for (PublicKeyCredentialParameters credParam : credParams) {
                if (supportedAlgorithms.contains(credParam.getAlg())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matchForAssertion(PublicKeyCredentialRequestOptions options) {
        if (!options.getAllowCredentials().isPresent()) {
            return false;
        }
        if (options.getAllowCredentials().get().isEmpty()) {
            return false;
        }
        return options.getAllowCredentials().get().stream()
                .map(credentialDescriptor -> credentialDescriptor.getId())
                .filter(credentialId -> repository.findCredential(credentialId).isPresent())
                .findFirst()
                .isPresent();
    }

    /**
     * Used in get()
     *
     * @param options
     * @return
     */
    private AuthenticatorAssertionResponse authenticatorAssertionResponse(PublicKeyCredentialRequestOptions options, Credential credential) {
        try {
            ByteArray authenticatorData = authenticatorData(options.getRpId(), credential, 1 + signatureCount);
            ByteArray clientDataJSON = clientDataJSON(options.getRpId(), options.getChallenge(), CLIENT_DATA_GET);
            ByteArray clientDataJsonHash = clientDataJsonHash(clientDataJSON);
            ByteArray dataToSign = authenticatorData.concat(clientDataJsonHash);
            return AuthenticatorAssertionResponse.builder()
                    .authenticatorData(authenticatorData)
                    .clientDataJSON(clientDataJSON)
                    .signature(CryptoUtil.signWithKey(dataToSign, credential.getKey()))
                    .userHandle(credential.getUserId())
                    .build();
        } catch (Exception e) {
            throw new BadRequest("Failed to crate AuthenticatorAssertionResponse", e);
        }
    }

    private AuthenticatorAttestationResponse authenticatorAttestationResponse(PublicKeyCredentialCreationOptions options,
                                                                              Credential credential) {
        try {
            return AuthenticatorAttestationResponse.builder()
                    .attestationObject(attestationObject(options, credential))
                    .clientDataJSON(clientDataJSON(options.getRp().getId(), options.getChallenge(), CLIENT_DATA_CREATE))
                    .transports(new LinkedHashSet<>(Arrays.asList(authenticatorTransport)))
                    .build();
        } catch (Exception e) {
            throw new BadRequest("Failed to create AuthenticatorAttestationResponse", e);
        }
    }

    private ByteArray clientDataJSON(String rpId, ByteArray challenge, String type) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("challenge", challenge.getBase64Url());
        objectNode.put("origin", rpId);
        objectNode.put("type", type);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            objectMapper.writeValue(baos, objectNode);
            return ByteArray.fromBase64Url(Base64.getUrlEncoder().encodeToString(baos.toByteArray()));
        } catch (Exception e) {
            throw new BadRequest("Failed to create clientDataJSON", e);
        }
    }

    private ByteArray clientDataJsonHash(ByteArray clientDataJSON) {
        return sha256(clientDataJSON);
    }

    private ByteArray attestationObject(PublicKeyCredentialCreationOptions options, Credential credential) {
        //prepare authData: https://developer.mozilla.org/en-US/docs/Web/API/AuthenticatorAssertionResponse/authenticatorData
        ByteArray authenticatorData = authenticatorData(options.getRp().getId(), credential, signatureCount);
        //prepare fmt
        String fmt = "none";
        //prepare attStmt
        ByteArray attStmt = new ByteArray("{}".getBytes(StandardCharsets.UTF_8));

        CBORObject cborMap = CBORObject.NewMap();
        cborMap.Add("authData", CBORObject.FromObject(authenticatorData.getBytes()));
        cborMap.Add("fmt", CBORObject.FromObject(fmt));
        cborMap.Add("attStmt", CBORObject.NewMap());
        return byteArrayConverter.toByteArray(cborMap);
    }

    private ByteArray authenticatorData(String rpId, Credential credential, int signatureCount) {
        //rpIdHash (32 bytes)
        ByteArray rpIdHashBytes = sha256(rpId);
        assert 32 == rpIdHashBytes.getBytes().length;
        //flags (1 bytes)
        ByteArray flagsByte = new ByteArray(new byte[]{0b01000101});
        assert 1 == flagsByte.getBytes().length;
        //signCount (4 bytes)
        ByteArray signCountBytes = byteArrayConverter.toByteArray(signatureCount);
        assert 4 == signCountBytes.getBytes().length;
        //attestedCredentialData (variable length)
        //AAGUID (16 bytes)
        ByteArray aaguidBytes = byteArrayConverter.toByteArray(getAaguid());
        assert 16 == aaguidBytes.getBytes().length;
        //credentialId (variable length)
        ByteArray credentialIdBytes = credential.getCredentialId();
        //credentialIdLength (2 bytes)
        short length = (short) credentialIdBytes.getBytes().length;
        ByteArray credentialIdLengthBytes = byteArrayConverter.toByteArray(length);
        assert 2 == credentialIdLengthBytes.getBytes().length;
        //credentialPublicKey (variable length)
        CBORObject credentialPublicKey = KeySerializationSupport.toPublicKeyCOSE(credential.getKey());
        ByteArray credentialPublicKeyBytes = byteArrayConverter.toByteArray(credentialPublicKey);
        return rpIdHashBytes
                .concat(flagsByte)
                .concat(signCountBytes)
                .concat(aaguidBytes)
                .concat(credentialIdLengthBytes)
                .concat(credentialIdBytes)
                .concat(credentialPublicKeyBytes);
    }
}