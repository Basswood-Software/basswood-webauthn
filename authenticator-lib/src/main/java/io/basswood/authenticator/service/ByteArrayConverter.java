package io.basswood.authenticator.service;

import com.upokecenter.cbor.CBORObject;
import com.yubico.webauthn.data.ByteArray;
import io.basswood.authenticator.exception.RootException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

public interface ByteArrayConverter {
    default ByteArray toByteArray(Object object) {
        return switch (object) {
            case Long longValue -> new ByteArray(ByteBuffer.allocate(8).putLong(longValue).array());
            case Integer intValue -> new ByteArray(ByteBuffer.allocate(4).putInt(intValue).array());
            case Short shortValue -> new ByteArray(ByteBuffer.allocate(2).putShort(shortValue).array());
            case Byte byteValue -> new ByteArray(ByteBuffer.allocate(1).put(byteValue).array());
            case BigInteger bigInteger -> new ByteArray(bigInteger.toByteArray());
            case UUID uuid -> toByteArray(uuid);
            case CBORObject cborObject -> toByteArray(cborObject);
            default ->
                    throw new IllegalArgumentException(String.format("Type %s not supported", object.getClass().getName()));
        };
    }

    default ByteArray toByteArray(UUID uuid) {
        return new ByteArray(
                ByteBuffer.allocate(16)
                        .putLong(uuid.getMostSignificantBits())
                        .putLong(uuid.getLeastSignificantBits())
                        .array()
        );
    }

    default ByteArray toByteArray(CBORObject cborObject) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            cborObject.WriteTo(baos);
            return new ByteArray(baos.toByteArray());
        } catch (IOException e) {
            throw new RootException(e);
        }
    }
}
