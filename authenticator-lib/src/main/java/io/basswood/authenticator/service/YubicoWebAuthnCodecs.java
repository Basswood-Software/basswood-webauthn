package io.basswood.authenticator.service;

import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.exception.AuthenticatorException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.interfaces.ECPublicKey;

import static io.basswood.authenticator.exception.AuthenticatorException.DEFAULT_ERROR_CODE;

/**
 * This class exposes few package private static methods in the yubico class {@link com.yubico.webauthn.WebAuthnCodecs}
 * @author shamualr
 * @since 1.0
 */
public class YubicoWebAuthnCodecs {
    private static final String YUBICO_CODEC_CLASS = "com.yubico.webauthn.WebAuthnCodecs";
    private static final String METHOD_NAME_ecPublicKeyToRaw = "ecPublicKeyToRaw";
    private static final String METHOD_NAME_rawEcKeyToCose = "rawEcKeyToCose";

    private static Method METHOD_ecPublicKeyToRaw;
    private static Method METHOD_rawEcKeyToCose;

    static {
        setupClass();
    }

    private static void setupClass() {
        try {
            Class<?> classWebAuthnCodecs = Class.forName(YUBICO_CODEC_CLASS);
            METHOD_ecPublicKeyToRaw = classWebAuthnCodecs.getDeclaredMethod(METHOD_NAME_ecPublicKeyToRaw, ECPublicKey.class);
            METHOD_ecPublicKeyToRaw.setAccessible(true);
            METHOD_rawEcKeyToCose = classWebAuthnCodecs.getDeclaredMethod(METHOD_NAME_rawEcKeyToCose, ByteArray.class);
            METHOD_rawEcKeyToCose.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new AuthenticatorException(e);
        }
    }

    public static ByteArray ecPublicKeyToRaw(ECPublicKey ecPublicKey) {
        try {
            Object object = METHOD_ecPublicKeyToRaw.invoke(null, ecPublicKey);
            if (!(object instanceof ByteArray byteArray)) {
                throw new AuthenticatorException("Reflective call on WebAuthnCodecs returned wrong type of result");
            }
            return byteArray;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new AuthenticatorException("Reflective call on WebAuthnCodecs.ecPublicKeyToRaw() failed", e, DEFAULT_ERROR_CODE, 500);
        }
    }

    public static ByteArray rawEcKeyToCose(ByteArray key) {
        try {
            Object object = METHOD_rawEcKeyToCose.invoke(null, key);
            if (!(object instanceof ByteArray byteArray)) {
                throw new AuthenticatorException("Reflective call on WebAuthnCodecs.rawEcKeyToCose() returned wrong type of result");
            }
            return byteArray;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new AuthenticatorException("Reflective call on WebAuthnCodecs.rawEcKeyToCose() failed", e, DEFAULT_ERROR_CODE, 500);
        }
    }
}
