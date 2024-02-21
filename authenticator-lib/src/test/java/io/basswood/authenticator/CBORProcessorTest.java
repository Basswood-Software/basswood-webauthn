package io.basswood.authenticator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.numbers.EInteger;
import com.yubico.webauthn.data.AttestationObject;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.CollectedClientData;
import com.yubico.webauthn.data.exception.Base64UrlException;
import io.basswood.authenticator.service.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
/*
{
  "type": "public-key",
  "id": "ASD1j9Ku97OxjjhPWX7ex5I-jsFOiZ0ND9De9pgKI9H77YvATRI4dIwqWw2fV4brsJKfcCz6qqdQ9KF7h7bszxLyA5yhtvpAkU4O5igpDt76aa6GSz4JYhuoLnThtYFGWCAaYEDmnoRwIxDhbN9FVQvWndo_gHCEaPHYEzN1GUcZ2KUg6g",
  "rawId": "ASD1j9Ku97OxjjhPWX7ex5I-jsFOiZ0ND9De9pgKI9H77YvATRI4dIwqWw2fV4brsJKfcCz6qqdQ9KF7h7bszxLyA5yhtvpAkU4O5igpDt76aa6GSz4JYhuoLnThtYFGWCAaYEDmnoRwIxDhbN9FVQvWndo_gHCEaPHYEzN1GUcZ2KUg6g",
  "authenticatorAttachment": "platform",
  "response": {
    "clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiZnp0aTJKUHFScFZDaFd1LXhGSTU4M2FuUHdIU0xJdG4zUDZCWDBNXzJybyIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCIsImNyb3NzT3JpZ2luIjpmYWxzZX0",
    "attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVkBCUmWDeWIDoxodDQXD2R2YFuP5K65ooYyx5lc87qDHZdjRWJgJ1KtzgACNbzGCmSLCyXx8FUDAIUBIPWP0q73s7GOOE9Zft7Hkj6OwU6JnQ0P0N72mAoj0fvti8BNEjh0jCpbDZ9Xhuuwkp9wLPqqp1D0oXuHtuzPEvIDnKG2-kCRTg7mKCkO3vpproZLPgliG6gudOG1gUZYIBpgQOaehHAjEOFs30VVC9ad2j-AcIRo8dgTM3UZRxnYpSDqpQECAyYgASFYINwpbIz79Knm9fUA3DDt5WZZsz618GcjkXtRrf5bgoYRIlggNA-v-slAPR2VoURVNZWKcjwYkAxABOS1dKcPLC_cwJo",
    "transports": [
      "internal"
    ]
  },
  "clientExtensionResults": {
    "credProps": {
      "rk": false
    }
  }
}

 */

class CBORProcessorTest {
    public static final String attestationObjectString = "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVkBCUmWDeWIDoxodDQXD2R2YFuP5K65ooYyx5lc87qDHZdjRWJgJ1KtzgACNbzGCmSLCyXx8FUDAIUBIPWP0q73s7GOOE9Zft7Hkj6OwU6JnQ0P0N72mAoj0fvti8BNEjh0jCpbDZ9Xhuuwkp9wLPqqp1D0oXuHtuzPEvIDnKG2-kCRTg7mKCkO3vpproZLPgliG6gudOG1gUZYIBpgQOaehHAjEOFs30VVC9ad2j-AcIRo8dgTM3UZRxnYpSDqpQECAyYgASFYINwpbIz79Knm9fUA3DDt5WZZsz618GcjkXtRrf5bgoYRIlggNA-v-slAPR2VoURVNZWKcjwYkAxABOS1dKcPLC_cwJo";
    public static final String clientDataJSON = "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiZnp0aTJKUHFScFZDaFd1LXhGSTU4M2FuUHdIU0xJdG4zUDZCWDBNXzJybyIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6MzAwMCIsImNyb3NzT3JpZ2luIjpmYWxzZX0";

    @Test
    void testSerialization() throws Exception {
        byte[] decode = Base64.getUrlDecoder().decode(attestationObjectString);
        CBORObject attestationObject = CBORObject.Read(new ByteArrayInputStream(decode));
        CBORObject authData = attestationObject.get("authData");
        //byte[] encodeToBytes = authData.EncodeToBytes();
        byte[] authDataBytes = authData.GetByteString();
        int authDataBytesLength = authDataBytes.length;
        int offset = 0;
        byte[] rpIdHash = new byte[32];
        System.arraycopy(authDataBytes, offset, rpIdHash, 0, 32);
        offset += 32;

        byte flags = authDataBytes[32];
        ++offset;

        byte[] signCount = new byte[4];
        System.arraycopy(authDataBytes, offset, signCount, 0, 4);
        offset += 4;
        int aaaa = Ints.fromByteArray(signCount);
        int bbbb = new BigInteger(1, signCount).intValue();
        byte[] cccc = new BigInteger(""+ ++bbbb).toByteArray();

        byte[] aaguid = new byte[16];
        System.arraycopy(authDataBytes, offset, aaguid, 0, 16);
        offset += 16;

        byte[] credentialIdLengthBytes = new byte[2];
        System.arraycopy(authDataBytes, offset, credentialIdLengthBytes, 0, 2);
        offset += 2;

        int a = credentialIdLengthBytes[0];
        int credentialIdLength = Byte.toUnsignedInt(credentialIdLengthBytes[1]);

        byte[] credentialIdBytes = new byte[credentialIdLength];
        System.arraycopy(authDataBytes, offset, credentialIdBytes, 0, credentialIdLength);
        offset += credentialIdLength;

        byte[] credentialPublicKeyBytes = new byte[authDataBytesLength - offset];
        System.arraycopy(authDataBytes, offset, credentialPublicKeyBytes, 0, authDataBytesLength - offset);
        offset += (authDataBytesLength - offset);

    }

    @Test
    void tryLibrary() throws Base64UrlException, IOException {
        AttestationObject attestationObject = new AttestationObject(ByteArray.fromBase64Url(attestationObjectString));
        CollectedClientData collectedClientData = new CollectedClientData(ByteArray.fromBase64Url(clientDataJSON));
        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString = "{\"challenge\":\"onequickbrownfoxjumpoverthelazydog\",\"origin\":\"https://amdocs.com\",\"type\":\"webauthn.create\"}";
        JsonNode jsonNode = objectMapper.reader().readTree(jsonString);
        byte[] bytes = objectMapper.writeValueAsBytes(jsonNode);
        ByteArray clientDataJson = ByteArray.fromBase64Url(Base64.getUrlEncoder().encodeToString(bytes));


        AuthenticatorAttestationResponse build = AuthenticatorAttestationResponse.builder()
                .attestationObject(ByteArray.fromBase64Url(attestationObjectString))
                .clientDataJSON(clientDataJson)
                .transports(new LinkedHashSet<>(Arrays.asList(AuthenticatorTransport.INTERNAL)))
                .build();
        System.out.println();
    }

    @Test
    void testCose() throws Exception {
        String numString = "123456789012345678901234567890";
        BigInteger bigInteger = new BigInteger(numString);
        EInteger eInteger = EInteger.FromString(numString);
        String eIntegerString = eInteger.toString();
        String bigIntegerString = bigInteger.toString();
        assertEquals(eIntegerString, bigIntegerString);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        BigInteger modulus = publicKey.getModulus();
        BigInteger exponent = publicKey.getPublicExponent();

        //CBORObject.FromObject(-1);
        CBORObject map = CBORObject.NewMap();
        map.Add(CBORObject.FromObject(-1), EInteger.FromString(modulus.toString()).ToBytes(false));
        map.Add(CBORObject.FromObject(-2), EInteger.FromString(exponent.toString()).ToBytes(false));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CBORObject.Write(map, baos);
        CBORObject cose = CBORObject.Read(new ByteArrayInputStream(baos.toByteArray()));


        RSAPublicKey rsaPublicKey = importCoseRsaPublicKey(cose);
        assertEquals(publicKey.getPublicExponent(), rsaPublicKey.getPublicExponent());
        assertEquals(publicKey.getModulus(), rsaPublicKey.getModulus());
        System.out.println();
    }


    private static RSAPublicKey importCoseRsaPublicKey(CBORObject cose)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec spec =
                new RSAPublicKeySpec(
                        new BigInteger(1, cose.get(CBORObject.FromObject(-1)).GetByteString()),
                        new BigInteger(1, cose.get(CBORObject.FromObject(-2)).GetByteString()));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    @Test
    void testJsonNode() throws IOException {
        String jsonString = "{\"challenge\":\"onequickbrownfoxjumpoverthelazydog\",\"origin\":\"https://amdocs.com\",\"type\":\"webauthn.create\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("challenge", "onequickbrownfoxjumpoverthelazydog");
        objectNode.put("origin", "https://amdocs.com");
        objectNode.put("type", "webauthn.create");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        objectMapper.writeValue(baos, objectNode);
        assertEquals(jsonString, new String(baos.toByteArray()));
    }

    @Test
    void testBuildAuthData() {
        ByteArray byteArray = CryptoUtil.sha256("https://amdocs.com");
        assertEquals(32, byteArray.getBytes().length);
        byte flag = 0b01000101;
        int signCount = 3;
        byte[] signCountBytes = Ints.toByteArray(signCount);

        Short aaguid = Short.valueOf((short) 123456);
        byte[] bytes = new BigInteger(aaguid.toString()).toByteArray();
        byte[] bytes2 = new BigInteger(""+Short.MAX_VALUE).toByteArray();


        System.out.println(flag);
    }
}