// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


package io.basswood.authenticator.yubico;

import COSE.CoseException;
import COSE.OneKey;
import com.google.common.primitives.Bytes;
import com.upokecenter.cbor.CBORObject;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.COSEAlgorithmIdentifier;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is copied from Youbico Library class <a href="https://github.com/Yubico/java-webauthn-server/blob/main/webauthn-server-core/src/main/java/com/yubico/webauthn/WebAuthnCodecs.java">com.yubico.webauthn.WebAuthnCodecs</a>
 *
 */
public final class WebAuthnCodecs {

    private static final ByteArray ED25519_CURVE_OID =
            new ByteArray(new byte[] {0x30, 0x05, 0x06, 0x03, 0x2B, 0x65, 0x70});

    public static ByteArray ecPublicKeyToRaw(ECPublicKey key) {
        byte[] x = key.getW().getAffineX().toByteArray();
        byte[] y = key.getW().getAffineY().toByteArray();
        byte[] xPadding = new byte[Math.max(0, 32 - x.length)];
        byte[] yPadding = new byte[Math.max(0, 32 - y.length)];

        Arrays.fill(xPadding, (byte) 0);
        Arrays.fill(yPadding, (byte) 0);

        return new ByteArray(
                Bytes.concat(
                        new byte[] {0x04},
                        Bytes.concat(xPadding, Arrays.copyOfRange(x, Math.max(0, x.length - 32), x.length)),
                        Bytes.concat(yPadding, Arrays.copyOfRange(y, Math.max(0, y.length - 32), y.length))));
    }

    public static ByteArray rawEcKeyToCose(ByteArray key) {
        final byte[] keyBytes = key.getBytes();
        if (!(keyBytes.length == 64 || (keyBytes.length == 65 && keyBytes[0] == 0x04))) {
            throw new IllegalArgumentException(
                    String.format(
                            "Raw key must be 64 bytes long or be 65 bytes long and start with 0x04, was %d bytes starting with %02x",
                            keyBytes.length, keyBytes[0]));
        }
        final int   start = (keyBytes.length == 64) ? 0 : 1;

        final Map<Long, Object> coseKey = new HashMap<>();
        coseKey.put(1L, 2L); // Key type: EC

        coseKey.put(3L, COSEAlgorithmIdentifier.ES256.getId());
        coseKey.put(-1L, 1L); // Curve: P-256

        coseKey.put(-2L, Arrays.copyOfRange(keyBytes, start, start + 32)); // x
        coseKey.put(-3L, Arrays.copyOfRange(keyBytes, start + 32, start + 64)); // y

        return new ByteArray(CBORObject.FromObject(coseKey).EncodeToBytes());
    }

    public static PublicKey importCosePublicKey(ByteArray key)
            throws CoseException, IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        CBORObject cose = CBORObject.DecodeFromBytes(key.getBytes());
        final int kty = cose.get(CBORObject.FromObject(1)).AsInt32();
        switch (kty) {
            case 1:
                return importCoseEdDsaPublicKey(cose);
            case 2:
                return importCoseP256PublicKey(cose);
            case 3:
                return importCoseRsaPublicKey(cose);
            default:
                throw new IllegalArgumentException("Unsupported key type: " + kty);
        }
    }

    private static PublicKey importCoseRsaPublicKey(CBORObject cose)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPublicKeySpec spec =
                new RSAPublicKeySpec(
                        new BigInteger(1, cose.get(CBORObject.FromObject(-1)).GetByteString()),
                        new BigInteger(1, cose.get(CBORObject.FromObject(-2)).GetByteString()));
        return Crypto.getKeyFactory("RSA").generatePublic(spec);
    }

    private static ECPublicKey importCoseP256PublicKey(CBORObject cose) throws CoseException {
        return (ECPublicKey) new OneKey(cose).AsPublicKey();
    }

    private static PublicKey importCoseEdDsaPublicKey(CBORObject cose)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        final int curveId = cose.get(CBORObject.FromObject(-1)).AsInt32();
        switch (curveId) {
            case 6:
                return importCoseEd25519PublicKey(cose);
            default:
                throw new IllegalArgumentException("Unsupported EdDSA curve: " + curveId);
        }
    }

    private static PublicKey importCoseEd25519PublicKey(CBORObject cose)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        final ByteArray rawKey = new ByteArray(cose.get(CBORObject.FromObject(-2)).GetByteString());
        final ByteArray x509Key =
                new ByteArray(new byte[] {0x30, (byte) (ED25519_CURVE_OID.size() + 3 + rawKey.size())})
                        .concat(ED25519_CURVE_OID)
                        .concat(new ByteArray(new byte[] {0x03, (byte) (rawKey.size() + 1), 0}))
                        .concat(rawKey);

        KeyFactory kFact = Crypto.getKeyFactory("EdDSA");
        return kFact.generatePublic(new X509EncodedKeySpec(x509Key.getBytes()));
    }

    static Optional<COSEAlgorithmIdentifier> getCoseKeyAlg(ByteArray key) {
        CBORObject cose = CBORObject.DecodeFromBytes(key.getBytes());
        final int alg = cose.get(CBORObject.FromObject(3)).AsInt32();
        return COSEAlgorithmIdentifier.fromId(alg);
    }

    static String getJavaAlgorithmName(COSEAlgorithmIdentifier alg) {
        switch (alg) {
            case EdDSA:
                return "EDDSA";
            case ES256:
                return "SHA256withECDSA";
            case RS256:
                return "SHA256withRSA";
            case RS1:
                return "SHA1withRSA";
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + alg);
        }
    }

    static String jwsAlgorithmNameToJavaAlgorithmName(String alg) {
        switch (alg) {
            case "RS256":
                return "SHA256withRSA";
        }
        throw new IllegalArgumentException("Unknown algorithm: " + alg);
    }
}