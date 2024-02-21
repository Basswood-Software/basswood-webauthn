package io.basswood.webauthn.model.credential;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.exception.Base64UrlException;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.model.user.User;
import io.basswood.webauthn.repository.RegisteredCredentialEntityRepository;
import io.basswood.webauthn.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CredentialRepositoryImpl implements CredentialRepository {

    private UserService userService;

    private RegisteredCredentialEntityRepository registeredCredentialEntityRepository;

    public CredentialRepositoryImpl(UserService userService, RegisteredCredentialEntityRepository registeredCredentialEntityRepository) {
        this.userService = userService;
        this.registeredCredentialEntityRepository = registeredCredentialEntityRepository;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RootException();
        }
        Set<RegisteredCredentialEntity> credentials = registeredCredentialEntityRepository.findByUser(user);
        if (credentials == null || credentials.isEmpty()) {
            //throw new AuthenticatorException();
            return new LinkedHashSet<>();
        }
        return credentials.stream().map(credential -> map(credential)).collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        User user = findByUsername(username);
        if (user == null) {
            Optional.empty();
        }
        try {
            return Optional.of(ByteArray.fromBase64Url(user.getUserHandle()));
        } catch (Base64UrlException e) {
            throw new RootException(e);
        }
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        Optional<User> userOptional = userService.findUserById(userHandle.getBase64Url());
        return userOptional.isPresent() ? Optional.of(userOptional.get().getUsernames().stream().findFirst().get().getUsername())
                : Optional.empty();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Optional<RegisteredCredentialEntity> optional = registeredCredentialEntityRepository.findById(credentialId.getBase64Url());
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        if (!userHandle.getBase64Url().equals(optional.get().getUser().getUserHandle())) {
            return Optional.empty();
        }
        try {
            RegisteredCredential registeredCredential = RegisteredCredential.builder()
                    .credentialId(credentialId)
                    .userHandle(userHandle)
                    .publicKeyCose(ByteArray.fromBase64Url(optional.get().getPublicKeyCose()))
                    .signatureCount(optional.get().getSignatureCount())
                    .build();
            return Optional.of(registeredCredential);
        } catch (Base64UrlException e) {
            throw new RootException(e);
        }
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        Optional<RegisteredCredentialEntity> optional = registeredCredentialEntityRepository.findById(credentialId.getBase64Url());
        if (!optional.isPresent()) {
            return Collections.emptySet();
        }
        try {
            RegisteredCredential registeredCredential = RegisteredCredential.builder()
                    .credentialId(credentialId)
                    .userHandle(ByteArray.fromBase64Url(optional.get().getUser().getUserHandle()))
                    .publicKeyCose(ByteArray.fromBase64Url(optional.get().getPublicKeyCose()))
                    .signatureCount(optional.get().getSignatureCount())
                    .build();
            return new LinkedHashSet<>(Arrays.asList(registeredCredential));
        } catch (Base64UrlException e) {
            throw new RootException(e);
        }
    }

    public User findByUsername(String username) {
        return userService.findUserByUsername(username)
                .orElseThrow(() -> new EntityNotFound(User.class, username));
    }

    private PublicKeyCredentialDescriptor map(RegisteredCredentialEntity credential) {
        try {
            return PublicKeyCredentialDescriptor.builder()
                    .id(ByteArray.fromBase64Url(credential.getCredentialId()))
                    .type(PublicKeyCredentialType.PUBLIC_KEY)
                    .transports(
                            credential.getTransports() == null || credential.getTransports().isEmpty() ?
                                    null :
                                    credential.getTransports().stream().map(t -> AuthenticatorTransport.of(t.getTransport())).collect(Collectors.toSet())
                    )
                    .build();
        } catch (Base64UrlException e) {
            throw new RootException(e);
        }
    }
}
