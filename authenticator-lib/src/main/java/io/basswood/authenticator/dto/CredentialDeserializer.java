package io.basswood.authenticator.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.jwk.JWK;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.Base64UrlException;
import io.basswood.authenticator.exception.AuthenticatorException;
import io.basswood.authenticator.model.Credential;

import java.io.IOException;
import java.text.ParseException;

public class CredentialDeserializer extends StdDeserializer<Credential> {

    public CredentialDeserializer() {
        super(Credential.class);
    }

    @Override
    public Credential deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return deserialize((ObjectNode) node);
    }

    public Credential deserialize(ObjectNode node) {
        try {
            ByteArray credentialId = ByteArray.fromBase64Url(node.get("credentialId").textValue());
            ByteArray userId = ByteArray.fromBase64Url(node.get("userId").textValue());
            ByteArray rpId = ByteArray.fromBase64Url(node.get("rpId").textValue());
            String keyString = ((ObjectNode) node.get("key")).toString();
            JWK jwk = JWK.parse(keyString);
            return new Credential(credentialId, userId, rpId, jwk);
        } catch (Base64UrlException | ParseException e) {
            throw new AuthenticatorException(e);
        }
    }
}
