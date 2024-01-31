package io.basswood.webauthn.model.jwk;

import io.basswood.webauthn.secret.AttributeEncryptionConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * @author shamualr
 * @since 1.0
 */
@Entity
@Table(name = "webauthn_jwk")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JWKEntity {
    public static final Long ONE_MONTH_IN_MILLIS = 30 * 24 * 3600 * 1000L;
    @Id
    @Column(name = "kid")
    private String kid;
    @Column(name = "kty")
    @Enumerated(EnumType.STRING)
    private KeyTypeEnum kty;
    @Column(name = "keyUse")
    @Enumerated(EnumType.STRING)
    private KeyUseEnum keyUse;
    @Column(name = "createdTime")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdTime = new Date();
    @Column(name = "expiryTime")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date expiryTime = new Date(System.currentTimeMillis() + ONE_MONTH_IN_MILLIS);
    @Column(name = "jwkData")
    @Convert(converter = AttributeEncryptionConverter.class)
    private String jwkData;
}
