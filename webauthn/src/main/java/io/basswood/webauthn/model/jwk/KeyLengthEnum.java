package io.basswood.webauthn.model.jwk;

/**
 * @author shamualr
 * @since 1.0
 */
public enum KeyLengthEnum {
    KEY_LENGTH_2048(2048), KEY_LENGTH_3072(3072), KEY_LENGTH_4096(4096);

    KeyLengthEnum(int length) {
        this.length = length;
    }

    final int length;

    public int length(){
        return length;
    }
}
