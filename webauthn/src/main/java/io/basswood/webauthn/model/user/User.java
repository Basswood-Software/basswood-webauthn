package io.basswood.webauthn.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.basswood.webauthn.model.credential.RegisteredCredentialEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "webauthn_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String userHandle;

    @Column(name = "displayName")
    private String displayName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Username> usernames;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<RegisteredCredentialEntity> registeredCredentials;
}
