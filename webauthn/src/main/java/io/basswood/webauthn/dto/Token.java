package io.basswood.webauthn.dto;

import java.util.Date;

/**
 * @author shamualr
 * @since 1.0
 */
public record Token(String subject, String issuer, String audience, String jti, Date issueTime, Date notBeforeTime,
                    Date expirationTime, Object payload, String payloadClaimName) {
}
