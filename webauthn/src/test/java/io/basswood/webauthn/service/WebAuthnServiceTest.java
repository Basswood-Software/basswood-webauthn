package io.basswood.webauthn.service;

import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class WebAuthnServiceTest {
    @Test
    void testParseRegistrationResponseJson() {
        String publicKeyCredentialJson = """
                {
                  "id": "a3ddc1a7-4192-484f-b005-a39b483ee07fcmVkLmJhc3N3b29kaWQuY29tOjkwODA",
                  "response": {
                    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi24mO4a-qXlHNmAiDhS1chFBhZWEbSd4AB8yM20C-OtrtFAAAAAayjX74IzkVanRGbSwcqgZoAMmt3XXNWu_uNfdvuPOH_m9NOfmt_W-PN3ntO33JlZC5iYXNzd29vZGlkLmNvbTo5MDgwpQECAyYgASFYIJsJ0HBtcmLJSSu9jBhjtM0Tf-9iZSAwNpUJoWep1BiyIlggB7RvT3YHm4nD6xZoA0GxgYfhIydvrqrwY2BB4NJ__c4",
                    "clientDataJSON": "eyJjaGFsbGVuZ2UiOiItLUtPWVhkU2RQMXpUeXM3MDNYUnFyMXBFOXJnSG5qVUtZcXdZLXM2M3AwIiwib3JpZ2luIjoicmVkLmJhc3N3b29kaWQuY29tOjkwODAiLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0",
                    "transports": [
                      "internal"
                    ]
                  },
                  "authenticatorAttachment": null,
                  "clientExtensionResults": {
                    "appidExclude": null,
                    "credProps": null,
                    "largeBlob": null
                  },
                  "type": "public-key"
                }
                """;
        PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc = null;
        try {
            pkc = PublicKeyCredential.parseRegistrationResponseJson(publicKeyCredentialJson);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assertions.assertEquals("public-key", pkc.getType().getId());
        Assertions.assertEquals("a3ddc1a7-4192-484f-b005-a39b483ee07fcmVkLmJhc3N3b29kaWQuY29tOjkwODA", pkc.getId().getBase64Url());
    }
}