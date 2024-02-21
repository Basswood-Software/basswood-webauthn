package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.model.Credential;
import io.basswood.authenticator.model.CredentialRepository;

import java.io.IOException;

public class CredentialRepositorySerializer extends StdSerializer<CredentialRepository> {
    private CredentialSerializer credentialSerializer;
    public CredentialRepositorySerializer() {
        super(CredentialRepository.class);
        credentialSerializer = new CredentialSerializer();
    }

    @Override
    public void serialize(CredentialRepository repository, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray();
        for (ByteArray credentialId : repository.getCredentialIds()){
            Credential credential = repository.findCredential(credentialId).get();
            credentialSerializer.serialize(credential, jsonGenerator, serializerProvider);
        }
        jsonGenerator.writeEndArray();
    }
}