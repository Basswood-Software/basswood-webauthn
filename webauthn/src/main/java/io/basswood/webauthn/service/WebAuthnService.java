package io.basswood.webauthn.service;

import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import com.yubico.webauthn.data.RegistrationExtensionInputs;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.basswood.webauthn.dto.RegistrationRequestDTO;
import io.basswood.webauthn.exception.BadRequest;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.credential.AuthenticatorTransportEntity;
import io.basswood.webauthn.model.credential.CredentialRepositoryImpl;
import io.basswood.webauthn.model.credential.RegisteredCredentialEntity;
import io.basswood.webauthn.model.rp.AuthenticatorPreference;
import io.basswood.webauthn.model.rp.RelyingPartyEntity;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.model.user.Username;
import io.basswood.webauthn.repository.RegisteredCredentialEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

@Slf4j
public class WebAuthnService {
    private RelyingPartyService relyingPartyService;
    private UserService userService;
    private CredentialRepositoryImpl credentialRepository;
    private RegisteredCredentialEntityRepository registeredCredentialEntityRepository;
    private CacheService cacheService;

    public WebAuthnService(RelyingPartyService relyingPartyService, UserService userService, CredentialRepositoryImpl credentialRepository, RegisteredCredentialEntityRepository registeredCredentialEntityRepository, CacheService cacheService) {
        this.relyingPartyService = relyingPartyService;
        this.userService = userService;
        this.credentialRepository = credentialRepository;
        this.registeredCredentialEntityRepository = registeredCredentialEntityRepository;
        this.cacheService = cacheService;
    }

    public PublicKeyCredentialCreationOptions startRegistration(String registrationId, String rpOrigin, RegistrationRequestDTO request) {
        RelyingPartyEntity relyingPartyEntity = relyingPartyService.findByOrigin(rpOrigin)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, rpOrigin));
        RelyingParty relyingParty = relyingParty(relyingPartyEntity);
        StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder()
                .user(userIdentity(request))
                .authenticatorSelection(authenticatorSelectionCriteria(relyingPartyEntity))
                .extensions(RegistrationExtensionInputs.builder().build())
                .timeout(relyingPartyEntity.getTimeout())
                .build();

        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(startRegistrationOptions);
        cacheService.put(registrationId, creationOptions);
        return creationOptions;
    }

    @Transactional
    public void finishRegistration(String rpOrigin, String registrationId, String publicKeyCredentialJson) {
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(publicKeyCredentialJson);
        } catch (IOException e) {
            String message = "Failed to parse publicKeyCredentialJson";
            log.debug(message);
            throw new BadRequest(message, e);
        }
        RelyingPartyEntity relyingPartyEntity = relyingPartyService.findByOrigin(rpOrigin)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, rpOrigin));
        RelyingParty rp = relyingParty(relyingPartyEntity);
        PublicKeyCredentialCreationOptions request = cacheService.getCreateOptions(registrationId);
        if(request == null){
            String message = "No registration request with id:"+registrationId+" found";
            log.debug(message);
            throw new EntityNotFound(PublicKeyCredentialCreationOptions.class, registrationId);
        }
        RegistrationResult result;
        try {
            result = rp.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());
        } catch (RegistrationFailedException e) {
            throw new RootException("Registration failed", e);
        }

        User user = userService.findUserByUsername(request.getUser().getName())
                .orElseThrow(() -> new EntityNotFound(User.class, request.getUser().getName()));

        SortedSet<AuthenticatorTransport> transports = pkc.getResponse().getTransports();
        Set<AuthenticatorTransportEntity> transportsData = (transports == null || transports.isEmpty())
                ? Collections.emptySet()
                : transports.stream().map(t -> AuthenticatorTransportEntity.builder().transport(t.getId()).build()).collect(Collectors.toSet());


        RegisteredCredentialEntity cre = RegisteredCredentialEntity.builder()
                .credentialId(result.getKeyId().getId().getBase64Url())
                .publicKeyCose(result.getPublicKeyCose().getBase64Url())
                .discoverable(false)
                .user(user)
                .signatureCount(result.getSignatureCount())
                .transports(transportsData)
                .clientDataJSON(pkc.getResponse().getClientDataJSON().getBase64Url())
                .attestationObject(pkc.getResponse().getAttestationObject().getBase64Url())
                .type(pkc.getType().getId())
                .build();
        cre.getTransports().forEach(t -> t.setRegisteredCredentialEntity(cre));
        RegisteredCredentialEntity registeredCredentialEntity = registeredCredentialEntityRepository.save(cre);
        cacheService.invalidateCreateOptions(registrationId);
    }

    public PublicKeyCredentialRequestOptions startAssertion(String rpOrigin, String loginHandle, String username) {
        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new EntityNotFound(User.class, username));
        RelyingPartyEntity rpEntity = relyingPartyService.findByOrigin(rpOrigin)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, rpOrigin));
        RelyingParty relyingParty = relyingParty(rpEntity);
        AssertionRequest assertionRequest = relyingParty.startAssertion(StartAssertionOptions.builder()
                .username(username)
                .timeout(rpEntity.getTimeout())
                .userVerification(rpEntity.getUserVerification() != null ? UserVerificationRequirement.valueOf(rpEntity.getUserVerification().name()) : null)
                .build());
        cacheService.put(loginHandle, assertionRequest);
        return assertionRequest.getPublicKeyCredentialRequestOptions();
    }

    public AssertionResult finishAssertion(String rpOrigin, String loginHandle, String publicKeyCredentialJson) {
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc;
        try {
            pkc = PublicKeyCredential.parseAssertionResponseJson(publicKeyCredentialJson);
        } catch (IOException e) {
            String message ="Failed to parse publicKeyCredentialJson";
            log.debug(message, e);
            throw new BadRequest(message, e);
        }
        RelyingPartyEntity relyingPartyEntity = relyingPartyService.findByOrigin(rpOrigin)
                .orElseThrow(() -> new EntityNotFound(RelyingPartyEntity.class, rpOrigin));

        RelyingParty rp = relyingParty(relyingPartyEntity);
        AssertionRequest request = cacheService.getAssertionRequest(loginHandle);
        if (request == null) {
            String message = "No assertion request with id:"+loginHandle+" found";
            log.debug(message);
            throw new EntityNotFound(AssertionRequest.class, loginHandle);
        }
        //
        List<RegisteredCredentialEntity> all = registeredCredentialEntityRepository.findAll();
        //
        AssertionResult assertionResult;
        try {
            assertionResult = rp.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());
        } catch (AssertionFailedException e) {
            throw new RootException("Assertion failed", e);
        }
        if (!assertionResult.isSuccess()) {
            throw new RootException("Authentication failed");
        }
        // Now update the credential signature count in the database.
        Optional<RegisteredCredentialEntity> optional = registeredCredentialEntityRepository.findById(assertionResult.getCredential().getCredentialId().getBase64Url());
        if (!optional.isPresent()) {
            throw new RootException("No credential found");
        }
        RegisteredCredentialEntity registeredCredential = optional.get();
        registeredCredential.setSignatureCount(assertionResult.getSignatureCount());
        registeredCredentialEntityRepository.save(registeredCredential);
        return assertionResult;
    }

    private RelyingParty relyingParty(RelyingPartyEntity relyingPartyEntity) {
        RelyingParty rp = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(relyingPartyEntity.getId())
                        .name(relyingPartyEntity.getName())
                        .build())
                .credentialRepository(credentialRepository)
                .origins(relyingPartyEntity.getOrigins().stream().map(t -> t.getOrigin()).collect(Collectors.toSet()))
                .allowOriginPort(relyingPartyEntity.getAllowOriginPort())
                .allowOriginSubdomain(relyingPartyEntity.getAllowOriginSubdomain())
                .build();
        return rp;
    }

    private UserIdentity userIdentity(RegistrationRequestDTO request) {
        User user = userService.findUserByUsername(request.getUsername())
                .orElseGet(() ->
                        userService.createUser(
                                User.builder()
                                        .displayName(request.getDisplayName())
                                        .usernames(Set.of(Username.builder().username(request.getUsername()).build()))
                                        .build()
                        )
                );
        try {
            return UserIdentity.builder()
                    .name(request.getUsername())
                    .displayName(user.getDisplayName())
                    .id(ByteArray.fromBase64Url(user.getUserHandle()))
                    .build();
        } catch (Base64UrlException e) {
            throw new RootException(e);
        }
    }

    private AuthenticatorSelectionCriteria authenticatorSelectionCriteria(RelyingPartyEntity relyingPartyEntity) {
        ResidentKeyRequirement keyRequirement = relyingPartyEntity.getResidentKey() == null ? null : ResidentKeyRequirement.valueOf(relyingPartyEntity.getResidentKey().toString());
        UserVerificationRequirement userVerificationRequirement = relyingPartyEntity.getUserVerification() == null ? null : UserVerificationRequirement.valueOf(relyingPartyEntity.getUserVerification().toString());
        AuthenticatorPreference.Attachment attachment = relyingPartyEntity.getAuthenticatorAttachment();
        AuthenticatorAttachment authenticatorAttachment = (attachment == null) ? AuthenticatorAttachment.CROSS_PLATFORM : AuthenticatorAttachment.valueOf(attachment.toString());
        return AuthenticatorSelectionCriteria.builder()
                .authenticatorAttachment(authenticatorAttachment)
                .residentKey(keyRequirement)
                .userVerification(userVerificationRequirement)
                .authenticatorAttachment(Optional.of(authenticatorAttachment))
                .build();
    }
}
