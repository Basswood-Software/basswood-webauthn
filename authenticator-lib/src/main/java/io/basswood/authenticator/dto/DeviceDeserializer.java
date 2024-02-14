package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.basswood.authenticator.exception.AuthenticatorException;
import io.basswood.authenticator.model.Device;
import io.basswood.authenticator.model.VirtualAuthenticator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.basswood.authenticator.exception.AuthenticatorException.ERROR_CODE_BAD_REQUEST;

public class DeviceDeserializer extends StdDeserializer<Device> {
    private VirtualAuthenticatorDeserializer virtualAuthenticatorDeserializer;

    public DeviceDeserializer() {
        super(Device.class);
        virtualAuthenticatorDeserializer = new VirtualAuthenticatorDeserializer();
    }

    @Override
    public Device deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        UUID deviceId = UUID.fromString(jsonNode.get("deviceId").textValue());
        JsonNode node = jsonNode.get("displayName");
        String displayName = null;
        if (node != null && node.textValue() != null && !node.textValue().trim().isEmpty()) {
            displayName = node.textValue().trim();
        }

        node = jsonNode.get("tags");
        Set<String> tags = null;
        if (node != null) {
            if (!(node instanceof ArrayNode)) {
                throw new AuthenticatorException("Device serialization failed. tags must be an array", null, ERROR_CODE_BAD_REQUEST, 400);
            }
            ArrayNode arrayNode = (ArrayNode) node;
            if (arrayNode.size() > 0) {
                tags = new LinkedHashSet<>();
                for (int i = 0; i < arrayNode.size(); i++) {
                    tags.add(arrayNode.get(i).textValue());
                }
            }
        }

        Map<UUID, VirtualAuthenticator> authenticators = new LinkedHashMap<>();
        try {
            ArrayNode arrayNode = (ArrayNode) jsonNode.get("authenticators");
            if (arrayNode != null && arrayNode.size() > 0) {
                for (int i = 0; i < arrayNode.size(); i++) {
                    ObjectNode objectNode = (ObjectNode) arrayNode.get(i);
                    VirtualAuthenticator va = virtualAuthenticatorDeserializer.deserialize(objectNode, deserializationContext);
                    authenticators.put(va.getAaguid(), va);
                }
            }
        } catch (Exception exception) {
            throw new AuthenticatorException("Device serialization failed", exception, ERROR_CODE_BAD_REQUEST, 400);
        }

        return Device.builder()
                .deviceId(deviceId)
                .displayName(displayName)
                .tags(tags)
                .authenticators(authenticators)
                .build();
    }
}
