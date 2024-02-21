package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class AuthenticatorCreateDTODeserializer extends StdDeserializer<AuthenticatorCreateDTO> {

    public AuthenticatorCreateDTODeserializer() {
        super(AuthenticatorCreateDTO.class);
    }

    @Override
    public AuthenticatorCreateDTO deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return deserialize((ObjectNode) node);
    }

    public AuthenticatorCreateDTO deserialize(ObjectNode node) {
        int signatureCount = (node.has("signatureCount")) ? node.get("signatureCount").intValue() : 0;
        AuthenticatorAttachment attachment = node.has("attachment") ?
                AuthenticatorAttachment.valueOf(node.get("attachment").textValue().toUpperCase()) :
                AuthenticatorAttachment.PLATFORM;

        AuthenticatorTransport transport = node.has("transport") ?
                AuthenticatorTransport.valueOf(node.get("transport").textValue().toUpperCase()) :
                AuthenticatorTransport.INTERNAL;

        Set<COSEAlgorithmIdentifier> supportedAlgorithms;

        if (node.has("supportedAlgorithms")) {
            JsonNode algoNode = node.get("supportedAlgorithms");
            if (!(algoNode instanceof ArrayNode arrayNode)) {
                throw new IllegalArgumentException("supportedAlgorithms must be an array node");
            }
            supportedAlgorithms = new LinkedHashSet<>();
            for (int i = 0; i < arrayNode.size(); i++) {
                supportedAlgorithms.add(COSEAlgorithmIdentifier.valueOf(arrayNode.get(i).textValue()));
            }
        } else {
            supportedAlgorithms = Set.of(COSEAlgorithmIdentifier.RS256, COSEAlgorithmIdentifier.ES256);
        }

        return new AuthenticatorCreateDTO.AuthenticatorCreateDTOBuilder()
                .signatureCount(signatureCount)
                .attachment(attachment)
                .transport(transport)
                .supportedAlgorithms(supportedAlgorithms)
                .build();
    }
}
