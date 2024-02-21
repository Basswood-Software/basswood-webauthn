package io.basswood.authenticator.service;

import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import io.basswood.authenticator.dto.AuthenticatorCreateDTO;
import io.basswood.authenticator.dto.DeviceCreateDTO;
import io.basswood.authenticator.exception.AuthenticatorException;
import io.basswood.authenticator.model.Device;
import io.basswood.authenticator.model.VirtualAuthenticator;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.basswood.authenticator.exception.AuthenticatorException.ERROR_CODE_BAD_REQUEST;
import static io.basswood.authenticator.exception.AuthenticatorException.ERROR_CODE_DUPLICATE_ENTITY;
import static io.basswood.authenticator.exception.AuthenticatorException.ERROR_CODE_NOT_FOUND;

public class DeviceService {
    private final DeviceDAO deviceDAO;

    public DeviceService(DeviceDAO deviceDAO) {
        this.deviceDAO = deviceDAO;
    }

    public Optional<Device> getDevice(UUID deviceId) {
        return deviceDAO.getDevice(deviceId);
    }

    public Device create(DeviceCreateDTO deviceCreateDTO) {
        return deviceDAO.create(
                Device.builder()
                        .deviceId(deviceCreateDTO.getDeviceId())
                        .displayName(deviceCreateDTO.getDisplayName())
                        .tags(deviceCreateDTO.getTags())
                        .build()
        );
    }

    public Optional<Device> remove(UUID aaguid) {
        return deviceDAO.remove(aaguid);
    }

    public Set<Device> removeByTag(String tag) {
        return deviceDAO.removeByTag(tag);
    }

    public Set<Device> getDeviceByTag(String tag) {
        return deviceDAO.getDeviceByTag(tag);
    }

    public Set<Device> export() {
        return deviceDAO.export();
    }

    public void importDevices(Set<Device> devices, boolean overwrite) {
        if (devices == null || devices.isEmpty()) {
            throw new AuthenticatorException("No devices to add", null, ERROR_CODE_BAD_REQUEST, 400);
        }
        if (!overwrite) {
            Optional<Optional<Device>> first = devices.stream()
                    .map(t -> deviceDAO.getDevice(t.getDeviceId()))
                    .filter(o -> o.isPresent())
                    .findFirst();
            if (first.isPresent()) { // device found
                throw new AuthenticatorException("A device with the same id: " + first.get().get().getDeviceId().toString() + " already exists",
                        null, ERROR_CODE_DUPLICATE_ENTITY, 409);
            }
        }
        devices.stream().forEach(device -> deviceDAO.update(device));
    }

    public Set<Device> clear() {
        return deviceDAO.clear();
    }

    public VirtualAuthenticator createAuthenticator(UUID deviceId, AuthenticatorCreateDTO authenticatorCreateDTO) {
        Optional<Device> deviceOptional = getDevice(deviceId);
        if (deviceOptional.isEmpty()) {
            throw new AuthenticatorException("No Device found with id:" + deviceId.toString(), ERROR_CODE_NOT_FOUND, 404);
        }
        VirtualAuthenticator authenticator = VirtualAuthenticator.builder()
                .aaguid(UUID.randomUUID())
                .attachment(authenticatorCreateDTO.getAttachment())
                .authenticatorTransport(authenticatorCreateDTO.getTransport())
                .key(KeySerializationSupport.randomECKeyPair())
                .supportedAlgorithms(authenticatorCreateDTO.getSupportedAlgorithms())
                .signatureCount(authenticatorCreateDTO.getSignatureCount())
                .build();
        deviceOptional.get().addAuthenticator(authenticator);
        return authenticator;
    }

    public PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> createCredential(UUID deviceId, UUID authenticatorId, PublicKeyCredentialCreationOptions options) {
        validateIds(deviceId, authenticatorId);
        Device device = deviceDAO.getDevice(deviceId).get();
        VirtualAuthenticator authenticator = device.getVirtualAuthenticator(authenticatorId).get();
        if (!authenticator.matchForRegistration(options)) {
            throw new AuthenticatorException("The authenticator with id: " + authenticator.getAaguid().toString() + " does not support the requested option", ERROR_CODE_BAD_REQUEST);
        }
        return authenticator.create(options);
    }

    public PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> getCredential(UUID deviceId, UUID authenticatorId, PublicKeyCredentialRequestOptions options) {
        validateIds(deviceId, authenticatorId);
        Device device = deviceDAO.getDevice(deviceId).get();
        VirtualAuthenticator authenticator = device.getVirtualAuthenticator(authenticatorId).get();
        if (!authenticator.matchForAssertion(options)) {
            throw new AuthenticatorException("The authenticator with id: " + authenticator.getAaguid().toString() + " does not support the requested option",
                    ERROR_CODE_BAD_REQUEST
            );
        }
        return authenticator.get(options);
    }

    private void validateIds(UUID deviceId, UUID authenticatorId) {
        Optional<Device> deviceOptional = deviceDAO.getDevice(deviceId);
        if (deviceOptional.isEmpty()) {
            throw new AuthenticatorException("No device found with id:" + deviceId.toString());
        }
        Device device = deviceOptional.get();
        Optional<VirtualAuthenticator> authenticatorOptional = device.getVirtualAuthenticator(authenticatorId);
        if (authenticatorOptional.isEmpty()) {
            throw new AuthenticatorException("No virtual authenticator found with id:" + authenticatorId.toString(), ERROR_CODE_NOT_FOUND, 404);
        }
    }
}