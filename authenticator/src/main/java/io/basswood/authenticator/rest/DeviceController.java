package io.basswood.authenticator.rest;

import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import io.basswood.authenticator.dto.AuthenticatorCreateDTO;
import io.basswood.authenticator.dto.DeviceCreateDTO;
import io.basswood.authenticator.exception.EntityNotFound;
import io.basswood.authenticator.model.Device;
import io.basswood.authenticator.model.VirtualAuthenticator;
import io.basswood.authenticator.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
public class DeviceController {

    private DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping(value = "/device")
    public Device createDevice(@RequestBody(required = false) DeviceCreateDTO deviceCreateDTO) {
        return deviceService.create(deviceCreateDTO != null ? deviceCreateDTO : DeviceCreateDTO.builder().build());
    }

    @GetMapping(value = "/device/{deviceId}")
    public Device getDevice(@PathVariable UUID deviceId) {
        Optional<Device> device = deviceService.getDevice(deviceId);
        if (!device.isPresent()) {
            throw new EntityNotFound(Device.class, deviceId.toString());
        }
        return device.get();
    }

    @GetMapping(value = "/device")
    public ResponseEntity<?> getDeviceByTag(@RequestParam String tag) {
        Set<Device> devices = deviceService.getDeviceByTag(tag);
        return (devices == null || devices.isEmpty()) ?
                ResponseEntity.status(HttpStatus.NOT_FOUND).build() :
                ResponseEntity.status(HttpStatus.OK).body(devices);
    }

    @DeleteMapping(value = "/device")
    public ResponseEntity<?> removeDeviceByTags(@RequestParam String tag) {
        Set<Device> devices = deviceService.removeByTag(tag);
        return (devices == null || devices.isEmpty()) ?
                ResponseEntity.status(HttpStatus.NO_CONTENT).build() :
                ResponseEntity.status(HttpStatus.OK).body(devices);
    }

    @DeleteMapping(value = "/device/{deviceId}")
    public ResponseEntity<?> removeDevice(@PathVariable UUID deviceId) {
        Optional<Device> remove = deviceService.remove(deviceId);
        if (remove.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(remove.get());
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
    }

    @PostMapping(value = "/device/export")
    public Set<Device> export() {
        return deviceService.export();
    }

    @PostMapping(value = "/device/import")
    public ResponseEntity<Object> importDevices(@RequestBody Set<Device> devices, @RequestParam(defaultValue = "false", required = false) boolean overwrite) {
        deviceService.importDevices(devices, overwrite);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @DeleteMapping(value = "/device/clear")
    public Set<Device> clear() {
        return deviceService.clear();
    }

    @PostMapping(value = "/device/{deviceId}/authenticator")
    public VirtualAuthenticator createAuthenticator(@PathVariable UUID deviceId, @RequestBody(required = false) AuthenticatorCreateDTO authenticatorCreateDTO) {
        AuthenticatorCreateDTO dto = (authenticatorCreateDTO != null) ?
                authenticatorCreateDTO :
                AuthenticatorCreateDTO.builder().build();
        return deviceService.createAuthenticator(deviceId, dto);
    }

    @PostMapping(value = "/device/credential/create")
    public PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> createCredential(@RequestParam("deviceId") UUID deviceId, @RequestParam("aaguid") UUID authenticatorId, @RequestBody PublicKeyCredentialCreationOptions options) {
        return deviceService.createCredential(deviceId, authenticatorId, options);
    }

    @PostMapping(value = "/device/credential/get")
    public PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> getCredential(@RequestParam("deviceId") UUID deviceId, @RequestParam("aaguid") UUID authenticatorId, @RequestBody PublicKeyCredentialRequestOptions options) {
        return deviceService.getCredential(deviceId, authenticatorId, options);
    }
}