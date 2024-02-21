package io.basswood.webauthn.model.jwk;

/**
 * @author shamualr
 * @since 1.0
 */

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import io.basswood.webauthn.exception.JWKException;

import java.text.ParseException;

/**
 * Converts a JWKEntity to and from com.nimbusds.jose.jwk.JWK
 */
public class JWKEntityConverter {
    public JWK toJWK(JWKEntity entity) {
        try {
            return JWK.parse(entity.getJwkData());
        } catch (ParseException e) {
            throw new JWKException("Converter error - while converting to JWK", e);
        }
    }

    public JWKEntity toEntity(JWK jwk) {
        return JWKEntity.builder()
                .kid(jwk.getKeyID())
                .kty(jwk.getKeyType() != null ? toKeyTypeEnum(jwk.getKeyType()) : null)
                .keyUse(jwk.getKeyUse() != null ? toKeuUseEnum(jwk.getKeyUse()) : null)
                .createdTime(jwk.getIssueTime() != null ? jwk.getIssueTime() : null)
                .expiryTime(jwk.getExpirationTime() != null ? jwk.getExpirationTime() : null)
                .jwkData(jwk.toJSONString())
                .build();
    }

    public KeyUseEnum toKeuUseEnum(KeyUse keyUse) {
        if (keyUse == KeyUse.SIGNATURE) {
            return KeyUseEnum.SIGNATURE;
        } else if (keyUse == KeyUse.ENCRYPTION) {
            return KeyUseEnum.ENCRYPTION;
        } else {
            throw new JWKException("Converter Error: unsupported KeyUse value:" + keyUse.getValue());
        }
    }

    public KeyUse toKeyUse(KeyUseEnum keyUseEnum) {
        return switch (keyUseEnum) {
            case ENCRYPTION -> KeyUse.ENCRYPTION;
            case SIGNATURE -> KeyUse.SIGNATURE;
        };
    }

    public KeyTypeEnum toKeyTypeEnum(KeyType keyType) {
        if (keyType == KeyType.EC) {
            return KeyTypeEnum.EC;
        } else if (keyType == KeyType.RSA) {
            return KeyTypeEnum.RSA;
        } else {
            throw new JWKException("Converter Error: unsupported KeyType value:" + keyType.getValue());
        }
    }

    public KeyType toKeyType(KeyTypeEnum keyTypeEnum) {
        return switch (keyTypeEnum) {
            case EC -> KeyType.EC;
            case RSA -> KeyType.RSA;
        };
    }

    public CurveEnum toCurveEnum(Curve curve) {
        if (curve == Curve.P_256) {
            return CurveEnum.P_256;
        } else if (curve == Curve.P_384) {
            return CurveEnum.P_384;
        } else if (curve == Curve.P_521) {
            return CurveEnum.P_521;
        } else {
            throw new JWKException("Converter Error: unsupported Curve value:" + curve.getName());
        }
    }

    public Curve toCurve(CurveEnum curveEnum) {
        return switch (curveEnum) {
            case P_256 -> Curve.P_256;
            case P_384 -> Curve.P_384;
            case P_521 -> Curve.P_521;
        };
    }
}
