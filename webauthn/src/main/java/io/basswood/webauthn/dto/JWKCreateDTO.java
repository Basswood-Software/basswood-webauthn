package io.basswood.webauthn.dto;

import io.basswood.webauthn.model.jwk.CurveEnum;
import io.basswood.webauthn.model.jwk.KeyLengthEnum;
import io.basswood.webauthn.model.jwk.KeyTypeEnum;
import io.basswood.webauthn.model.jwk.KeyUseEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author shamualr
 * @since 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class JWKCreateDTO {
    @Builder.Default
    private KeyTypeEnum  keyTypeEnum = KeyTypeEnum.EC;
    @Builder.Default
    private KeyUseEnum keyUseEnum = KeyUseEnum.SIGNATURE;
    @Builder.Default
    private CurveEnum curveEnum = CurveEnum.P_256;
    @Builder.Default
    private KeyLengthEnum keyLengthEnum = KeyLengthEnum.KEY_LENGTH_2048;
}
