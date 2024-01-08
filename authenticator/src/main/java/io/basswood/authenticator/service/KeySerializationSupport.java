package io.basswood.authenticator.service;

import COSE.CoseException;
import COSE.OneKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.numbers.EInteger;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.exception.RootException;
import io.basswood.authenticator.yubico.WebAuthnCodecs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.UUID;

public class KeySerializationSupport {

    public static <T extends JWK> T randomKeyPair(Class<T> type) {
        if (type == RSAKey.class) {
            return (T) randomRSAKeyPair();
        } else if (type == ECKey.class) {
            return (T) randomECKeyPair();
        } else {
            throw new RootException("Key type: " + type.getName() + " not supported.");
        }
    }

    public static RSAKey randomRSAKeyPair() {
        try {
            return new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (JOSEException e) {
            throw new RootException(e);
        }
    }

    public static CBORObject toRSAPublicKeyCOSE(RSAKey rsaKey) {
        RSAPublicKey rsaPublicKey;
        try {
            rsaPublicKey = (RSAPublicKey) rsaKey.toPublicKey();
            return toRSAPublicKeyCOSE(rsaPublicKey);
        } catch (JOSEException e) {
            throw new RootException(e);
        }
    }

    public static CBORObject toRSAPublicKeyCOSE(RSAPublicKey rsaPublicKey) {
        CBORObject cborMap = CBORObject.NewMap();
        cborMap.Add(CBORObject.FromObject(-1), EInteger.FromString(rsaPublicKey.getModulus().toString()).ToBytes(false));
        cborMap.Add(CBORObject.FromObject(-2), EInteger.FromString(rsaPublicKey.getPublicExponent().toString()).ToBytes(false));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            CBORObject.Write(cborMap, baos);
        } catch (IOException e) {
            throw new RootException(e);
        }
        CBORObject cose = CBORObject.Read(new ByteArrayInputStream(baos.toByteArray()));
        return cose;
    }

    public static RSAPublicKey fromRSACOSE(CBORObject cose) {
        RSAPublicKeySpec spec = new RSAPublicKeySpec(
                new BigInteger(1, cose.get(CBORObject.FromObject(-1)).GetByteString()),
                new BigInteger(1, cose.get(CBORObject.FromObject(-2)).GetByteString()));
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RootException(e);
        }
    }

    public static ECKey randomECKeyPair() {
        try {
            return new ECKeyGenerator(Curve.P_256)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        } catch (JOSEException e) {
            throw new RootException(e);
        }
    }

    public static CBORObject toECPublicKeyCOSE(ECKey ecKey) {
        ECPublicKey ecPublicKey;
        try {
            ecPublicKey = (ECPublicKey) ecKey.toPublicKey();
            return toECPublicKeyCOSE(ecPublicKey);
        } catch (JOSEException e) {
            throw new RootException(e);
        }
    }

    public static CBORObject toECPublicKeyCOSE(ECPublicKey ecPublicKey) {
        ByteArray byteArray = WebAuthnCodecs.ecPublicKeyToRaw(ecPublicKey);
        ByteArray cborBytes = WebAuthnCodecs.rawEcKeyToCose(byteArray);
        CBORObject cose = CBORObject.Read(new ByteArrayInputStream(cborBytes.getBytes()));
        return cose;
    }

    public static ECPublicKey fromECCOSE(CBORObject cose) {
        try {
            return (ECPublicKey) new OneKey(cose).AsPublicKey();
        } catch (CoseException e) {
            throw new RootException(e);
        }
    }

    public static <T extends JWK> CBORObject toPublicKeyCOSE(T jwk) {
        if (jwk instanceof RSAKey) {
            return toRSAPublicKeyCOSE(((RSAKey) jwk).toPublicJWK());
        } else if (jwk instanceof ECKey) {
            return toECPublicKeyCOSE(((ECKey) jwk).toPublicJWK());
        } else {
            throw new RootException("Key type:" + jwk.getClass().getName() + " not supported");
        }
    }
}
