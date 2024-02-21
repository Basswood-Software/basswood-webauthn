package io.basswood.webauthn.secret;

/**
 * @author shamualr
 * @since 1.0
 */
public record KeystoreConfig(
        String storeType, String storepass,
        String aesKeyPassword, String aesKeyAlias,
        String dbKeyPassword, String dbKeyAlias
) {
}
