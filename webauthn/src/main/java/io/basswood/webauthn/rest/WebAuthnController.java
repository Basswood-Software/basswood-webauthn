package io.basswood.webauthn.rest;

import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialRequestOptions;
import io.basswood.webauthn.dto.AssertionRequestDTO;
import io.basswood.webauthn.dto.RegistrationRequestDTO;
import io.basswood.webauthn.service.WebAuthnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class WebAuthnController {

    public static final String HEADER_NAME_FORWARDED_HOST = "X-Forwarded-Host";
    public static final String HEADER_NAME_REGISTRATION_ID = "registrationId";
    public static final String HEADER_NAME_LOGIN_HANDLE = "loginHandle";

    private WebAuthnService webAuthnService;

    public WebAuthnController(WebAuthnService webAuthnService) {
        this.webAuthnService = webAuthnService;
    }

    @PostMapping(value = "/webauthn/registration", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicKeyCredentialCreationOptions> startRegistration(
            @RequestHeader(HEADER_NAME_FORWARDED_HOST) String rpOrigin,
            @RequestBody RegistrationRequestDTO registrationRequestDTO
    ) {
        String registrationId = UUID.randomUUID().toString();
        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = webAuthnService.startRegistration(registrationId, rpOrigin, registrationRequestDTO);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HEADER_NAME_REGISTRATION_ID, registrationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(publicKeyCredentialCreationOptions);
    }

    @PostMapping(value = "/webauthn/registration", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "registrationId")
    public void finishRegistration(
            @RequestHeader(HEADER_NAME_FORWARDED_HOST) String rpOrigin,
            @RequestHeader(HEADER_NAME_REGISTRATION_ID) String registrationId,
            @RequestBody String publicKeyCredentialJson
    ) {
        webAuthnService.finishRegistration(rpOrigin, registrationId, publicKeyCredentialJson);
    }

    @PostMapping(value = "/webauthn/assertion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicKeyCredentialRequestOptions> startAssertion(
            @RequestHeader(HEADER_NAME_FORWARDED_HOST) String rpOrigin,
            @RequestBody AssertionRequestDTO assertionRequestDTO
    ) {
        String loginHandle = UUID.randomUUID().toString();
        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = webAuthnService.startAssertion(rpOrigin, loginHandle, assertionRequestDTO.getUsername());
        return ResponseEntity.status(HttpStatus.OK)
                .header(HEADER_NAME_LOGIN_HANDLE, loginHandle)
                .body(publicKeyCredentialRequestOptions);
    }

    @PostMapping(value = "/webauthn/assertion", consumes = MediaType.APPLICATION_JSON_VALUE, headers = "loginHandle")
    public AssertionResult finishAssertion(
            @RequestHeader(HEADER_NAME_FORWARDED_HOST) String rpOrigin,
            @RequestHeader(HEADER_NAME_LOGIN_HANDLE) String loginHandle,
            @RequestBody String publicKeyCredentialJson
    ) {
        return webAuthnService.finishAssertion(rpOrigin, loginHandle, publicKeyCredentialJson);
    }
}