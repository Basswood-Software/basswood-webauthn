package io.basswood.webauthn.model.credential;

import io.basswood.webauthn.model.rp.AuthenticatorPreference;
import io.basswood.webauthn.model.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "webauthn_registered_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisteredCredentialEntity {

    @Id
    @Column(name = "credentialId")
    private String credentialId;

    @Column(name = "type")
    @Builder.Default
    private String type = "public-key";

    @ManyToOne
    @JoinColumn(name = "userHandle", referencedColumnName = "userHandle")
    private User user;

    @Column(name = "publicKeyCose")
    private String publicKeyCose;

    @Column(name = "clientDataJSON")
    private String clientDataJSON;

    @Column(name = "attestationObject")
    private String attestationObject;

    @Column(name = "authenticatorAttachment")
    private AuthenticatorPreference.Attachment authenticatorAttachment;


    @Column(name = "signatureCount")
    private Long signatureCount;

    @Column(name = "discoverable")
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean discoverable;

    @OneToMany(mappedBy = "registeredCredentialEntity", cascade = CascadeType.ALL)
    private Set<AuthenticatorTransportEntity> transports;
}