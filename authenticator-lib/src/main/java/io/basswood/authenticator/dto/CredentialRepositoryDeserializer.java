package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.exception.AuthenticatorException;
import io.basswood.authenticator.model.Credential;
import io.basswood.authenticator.model.CredentialRepository;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CredentialRepositoryDeserializer extends StdDeserializer<CredentialRepository> {
    private CredentialDeserializer credentialDeserializer;
    public CredentialRepositoryDeserializer() {
        super(CredentialRepository.class);
        credentialDeserializer = new CredentialDeserializer();
    }

    @Override
    public CredentialRepository deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ArrayNode node = jsonParser.getCodec().readTree(jsonParser);
        return deserialize(node, deserializationContext);
    }
    public CredentialRepository deserialize(ArrayNode arrayNode, DeserializationContext deserializationContext) throws IOException, JacksonException {
        Map<ByteArray, Credential> repository = new LinkedHashMap<>();
        try {
            for (int i = 0; i < arrayNode.size(); i++) {
                ObjectNode jsonNode = (ObjectNode)arrayNode.get(i);
                Credential credential = credentialDeserializer.deserialize(jsonNode);
                repository.put(credential.getCredentialId(), credential);
            }
            return new CredentialRepository(repository);
        }catch (Exception ex){
            throw new AuthenticatorException(ex);
        }
    }
}
