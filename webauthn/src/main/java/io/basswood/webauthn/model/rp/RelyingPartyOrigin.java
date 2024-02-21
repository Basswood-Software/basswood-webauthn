package io.basswood.webauthn.model.rp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webauthn_relying_party_origins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelyingPartyOrigin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rpId")
    @JsonIgnore
    private RelyingPartyEntity relyingPartyEntity;
}
