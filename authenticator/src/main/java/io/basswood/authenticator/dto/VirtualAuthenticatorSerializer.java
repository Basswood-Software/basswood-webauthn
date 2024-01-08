package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.basswood.authenticator.model.VirtualAuthenticator;

import java.io.IOException;
import java.util.stream.Collectors;

public class VirtualAuthenticatorSerializer extends StdSerializer<VirtualAuthenticator> {
    private CredentialRepositorySerializer credentialRepositorySerializer;

    public VirtualAuthenticatorSerializer() {
        super(VirtualAuthenticator.class);
        this.credentialRepositorySerializer = new CredentialRepositorySerializer();
    }

    @Override
    public void serialize(VirtualAuthenticator va, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String[] algorithms = va.supportedAlgorithms().stream()
                .map(t -> t.name())
                .collect(Collectors.toSet())
                .toArray(new String[]{});
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("aaguid", va.getAaguid().toString());
        jsonGenerator.writeNumberField("signatureCount", va.getSignatureCount());
        jsonGenerator.writeStringField("attachment", va.getAttachment().getValue());
        jsonGenerator.writeStringField("transport", va.getAuthenticatorTransport().getId());

        jsonGenerator.writeFieldName("algorithms");
        jsonGenerator.writeStartArray();
        for (String algorithm : algorithms) {
            jsonGenerator.writeString (algorithm);
        }
        jsonGenerator.writeEndArray();

        jsonGenerator.writeRaw(String.format(",%s:%s", "\"key\"", va.getKey().toJSONString()));
        jsonGenerator.writeFieldName("repository");
        credentialRepositorySerializer.serialize(va.getRepository(), jsonGenerator, serializerProvider);
        jsonGenerator.writeEndObject();
    }
}
