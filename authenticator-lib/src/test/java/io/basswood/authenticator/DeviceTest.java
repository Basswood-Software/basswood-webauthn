package io.basswood.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import io.basswood.authenticator.model.Device;
import io.basswood.authenticator.model.VirtualAuthenticator;
import io.basswood.authenticator.service.KeySerializationSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static com.yubico.webauthn.data.AuthenticatorAttachment.CROSS_PLATFORM;
import static com.yubico.webauthn.data.AuthenticatorAttachment.PLATFORM;
import static com.yubico.webauthn.data.AuthenticatorTransport.INTERNAL;

class DeviceTest {

    private ObjectMapper objectMapper;
    private Device device;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        VirtualAuthenticator v1 = VirtualAuthenticator.builder()
                .aaguid(UUID.randomUUID())
                .attachment(PLATFORM)
                .authenticatorTransport(INTERNAL)
                .key(KeySerializationSupport.randomRSAKeyPair())
                .supportedAlgorithms(new LinkedHashSet<>(Arrays.asList(COSEAlgorithmIdentifier.ES256)))
                .build();
        VirtualAuthenticator v2 = VirtualAuthenticator.builder()
                .aaguid(UUID.randomUUID())
                .attachment(CROSS_PLATFORM)
                .authenticatorTransport(AuthenticatorTransport.USB)
                .key(KeySerializationSupport.randomECKeyPair())
                .supportedAlgorithms(new LinkedHashSet<>(Arrays.asList(COSEAlgorithmIdentifier.ES256)))
                .build();
        device = Device.builder()
                .deviceId(UUID.randomUUID())
                .displayName("Test Device")
                .tags(Set.of("apple", "ios"))
                .authenticators(ImmutableMap.<UUID, VirtualAuthenticator>builder()
                        .put(v1.getAaguid(), v1)
                        .put(v2.getAaguid(), v2)
                        .build())
                .build();
    }

    @Test
    void testSerialization() throws JsonProcessingException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(device);
        Device actual = objectMapper.readValue(json, Device.class);
        Assertions.assertEquals(device.getDeviceId(), actual.getDeviceId());
        Assertions.assertEquals(device.getDisplayName(), actual.getDisplayName());
        Assertions.assertArrayEquals(device.getTags().toArray(), actual.getTags().toArray());
        Assertions.assertEquals(device.authenticators().size(), actual.authenticators().size());
    }
}