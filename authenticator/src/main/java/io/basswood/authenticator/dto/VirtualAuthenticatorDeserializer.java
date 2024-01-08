package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.nimbusds.jose.jwk.JWK;
import com.yubico.webauthn.data.AuthenticatorAttachment;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;
import io.basswood.authenticator.exception.RootException;
import io.basswood.authenticator.model.CredentialRepository;
import io.basswood.authenticator.model.VirtualAuthenticator;

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class VirtualAuthenticatorDeserializer extends StdDeserializer<VirtualAuthenticator> {
    private CredentialRepositoryDeserializer credentialRepositoryDeserializer;

    public VirtualAuthenticatorDeserializer() {
        super(VirtualAuthenticator.class);
        credentialRepositoryDeserializer = new CredentialRepositoryDeserializer();
    }

    @Override
    public VirtualAuthenticator deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        return deserialize(jsonNode, deserializationContext);
    }

    public VirtualAuthenticator deserialize(JsonNode jsonNode, DeserializationContext deserializationContext) throws IOException, JacksonException {
        UUID aaguid = UUID.fromString(jsonNode.get("aaguid").textValue());
        int signatureCount = jsonNode.get("signatureCount").asInt();
        String attachment = jsonNode.get("attachment").textValue();
        AuthenticatorAttachment authenticatorAttachment = null;
        for (AuthenticatorAttachment value : AuthenticatorAttachment.values()) {
            if (value.getValue().equals(attachment)) {
                authenticatorAttachment = value;
            }
        }
        if (authenticatorAttachment == null) {
            throw new RootException("Invalid value for authenticator attachment:" + attachment);
        }

        String transport = jsonNode.get("transport").textValue();
        AuthenticatorTransport authenticatorTransport = AuthenticatorTransport.of(transport);

        ArrayNode supportedAlgorithmNodes = (ArrayNode) jsonNode.get("algorithms");
        Set<COSEAlgorithmIdentifier> supportedAlgorithms = new LinkedHashSet<>();
        for (int i = 0; i < supportedAlgorithmNodes.size(); i++) {
            supportedAlgorithms.add(COSEAlgorithmIdentifier.valueOf(supportedAlgorithmNodes.get(i).textValue()));
        }
        JWK key;
        try {
            key = JWK.parse(jsonNode.get("key").toString());
        } catch (ParseException e) {
            throw new RootException(e);
        }
        if (key == null) {
            throw new RootException("Failed to serialize key");
        }

        JsonNode repoNode = jsonNode.get("repository");
        CredentialRepository credentialRepository = credentialRepositoryDeserializer.deserialize((ArrayNode) repoNode, deserializationContext);
        VirtualAuthenticator authenticator = new VirtualAuthenticator(aaguid, authenticatorAttachment,
                authenticatorTransport, key, supportedAlgorithms, signatureCount);

        if(credentialRepository != null && !credentialRepository.getCredentialIds().isEmpty()){
            for (ByteArray credentialId : credentialRepository.getCredentialIds()) {
                authenticator.getRepository().add(credentialRepository.findCredential(credentialId).get());
            }
        }
        return authenticator;
    }
}
