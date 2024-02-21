package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.basswood.authenticator.model.Credential;

import java.io.IOException;

public class CredentialSerializer extends StdSerializer<Credential> {

    public CredentialSerializer() {
        super(Credential.class);
    }

    @Override
    public void serialize(Credential credential, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("credentialId", credential.getCredentialId().getBase64Url());
        jsonGenerator.writeStringField("userId", credential.getUserId().getBase64Url());
        jsonGenerator.writeStringField("rpId", credential.getRpId().getBase64Url());

        jsonGenerator.writeFieldName("key");
        jsonGenerator.writeRaw (": "+credential.getKey().toJSONString());

        jsonGenerator.writeEndObject();
    }
}
