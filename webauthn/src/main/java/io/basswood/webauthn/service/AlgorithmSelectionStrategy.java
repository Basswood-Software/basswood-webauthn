package io.basswood.webauthn.service;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.RSAKey;
import io.basswood.webauthn.exception.RootException;

public interface AlgorithmSelectionStrategy {
    default JWSAlgorithm signatureAlgorithm(JWK jwk) {
        if (jwk.getKeyType() == KeyType.RSA) {
            return JWSAlgorithm.RS256;
        } else if (jwk.getKeyType() == KeyType.EC) {
            ECKey ecKey = (ECKey) jwk;
            if (ecKey.getCurve() == Curve.P_256) {
                return JWSAlgorithm.ES256;
            } else if (ecKey.getCurve() == Curve.P_384) {
                return JWSAlgorithm.ES384;
            } else if (ecKey.getCurve() == Curve.P_521) {
                return JWSAlgorithm.ES512;
            } else {
                throw new RootException("Unsupported Curve: " + ecKey.getCurve());
            }
        }
        throw new RootException("Unsupported KeyType: " + jwk.getKeyType());
    }

    default JWEAlgorithm encryptionAlgorithm(JWK jwk) {
        if (jwk.getKeyType() == KeyType.RSA) {
            return JWEAlgorithm.RSA_OAEP_256;
        } else if (jwk.getKeyType() == KeyType.EC) {
            return JWEAlgorithm.ECDH_ES_A256KW;
        }
        throw new RootException("Unsupported KeyType: " + jwk.getKeyType());
    }

    default JWSSigner signer(JWK jwk) {
        try {
            return jwk.getKeyType() == KeyType.RSA
                    ? new RSASSASigner((RSAKey) jwk)
                    : new ECDSASigner((ECKey) jwk);
        } catch (JOSEException e) {
            throw new RootException("No signer could be identified", e);
        }
    }

    default JWSVerifier verifier(JWK jwk) {
        try {
            return jwk.getKeyType() == KeyType.RSA
                    ? new RSASSAVerifier((RSAKey) jwk)
                    : new ECDSAVerifier((ECKey) jwk);
        } catch (JOSEException e) {
            throw new RootException("No signer could be identified", e);
        }
    }

    default JWEEncrypter encrypter(JWK jwk) {
        try {
            return jwk.getKeyType() == KeyType.RSA ?
                    new RSAEncrypter((RSAKey) jwk) :
                    new ECDHEncrypter((ECKey) jwk);
        } catch (JOSEException e) {
            throw new RootException("No encrypter could be identified", e);
        }
    }

    default JWEDecrypter decrypter(JWK jwk) {
        try {
            return jwk.getKeyType() == KeyType.RSA ?
                    new RSADecrypter((RSAKey) jwk) :
                    new ECDHDecrypter((ECKey) jwk);
        } catch (JOSEException e) {
            throw new RootException("No decrypter could be identified", e);
        }
    }

    default EncryptionMethod encryptionMethod() {
        return EncryptionMethod.A256GCM;
    }
}
