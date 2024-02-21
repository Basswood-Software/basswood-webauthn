package io.basswood.webauthn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequestDTO {
    /** Identifiers for the user creating a credential. */
    @NotNull
    private String username;

    private String displayName;
}