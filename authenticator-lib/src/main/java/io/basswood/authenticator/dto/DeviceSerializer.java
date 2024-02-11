package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.basswood.authenticator.model.Device;
import io.basswood.authenticator.model.VirtualAuthenticator;

import java.io.IOException;
import java.util.Set;

public class DeviceSerializer extends StdSerializer<Device> {
    private VirtualAuthenticatorSerializer virtualAuthenticatorSerializer;

    public DeviceSerializer() {
        super(Device.class);
        this.virtualAuthenticatorSerializer = new VirtualAuthenticatorSerializer();
    }

    @Override
    public void serialize(Device device, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("deviceId", device.getDeviceId().toString());
        jsonGenerator.writeStringField("displayName", device.getDisplayName());

        Set<String> tags = device.getTags();
        if (!tags.isEmpty()) {
            jsonGenerator.writeFieldName("tags");
            jsonGenerator.writeStartArray();
            for (String tag : tags) {
                jsonGenerator.writeString(tag);
            }
            jsonGenerator.writeEndArray();
        }
        Set<VirtualAuthenticator> authenticators = device.authenticators();
        if (!authenticators.isEmpty()) {
            jsonGenerator.writeFieldName("authenticators");
            jsonGenerator.writeStartArray();
            for (VirtualAuthenticator authenticator : authenticators) {
                virtualAuthenticatorSerializer.serialize(authenticator, jsonGenerator, serializerProvider);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndObject();
    }
}