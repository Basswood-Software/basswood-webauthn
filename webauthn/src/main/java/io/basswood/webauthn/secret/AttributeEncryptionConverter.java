package io.basswood.webauthn.secret;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @author shamualr
 * @since 1.0
 */
@Converter
public class AttributeEncryptionConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionService.getInstance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionService.getInstance().decrypt(dbData);
    }
}
