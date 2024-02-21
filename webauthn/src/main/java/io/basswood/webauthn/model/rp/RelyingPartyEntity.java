package io.basswood.webauthn.model.rp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "webauthn_relying_party")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelyingPartyEntity {
    @Id
    private String id;

    @Column(name = "name")
    private String name;

    /* none, indirect, direct, enterprise */
    @Column(name = "attestation")
    @Enumerated(EnumType.STRING)
    private AuthenticatorPreference.Attestation attestation;

    /* cross_platform, platform */
    @Column(name = "authenticatorAttachment")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthenticatorPreference.Attachment authenticatorAttachment = AuthenticatorPreference.Attachment.CROSS_PLATFORM;

    /* discouraged, preferred, required */
    @Column(name = "residentKey")
    @Enumerated(EnumType.STRING)
    private AuthenticatorPreference.ResidentKey residentKey;

    /* discouraged, preferred, required */
    @Column(name = "userVerification")
    @Enumerated(EnumType.STRING)
    private AuthenticatorPreference.UserVerification userVerification;

    @Column(name = "allowOriginPort", nullable = false)
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean allowOriginPort;

    @Column(name = "allowOriginSubdomain", nullable = false)
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean allowOriginSubdomain;

    @OneToMany(mappedBy = "relyingPartyEntity", cascade = CascadeType.ALL)
    private Set<RelyingPartyOrigin> origins;

    @Column(name = "timeout")
    private Long timeout;
}
