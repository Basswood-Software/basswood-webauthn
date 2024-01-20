package io.basswood.webauthn.model.token;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * @author shamualr
 * @since 1.0
 */
public record Token(String subject, String issuer, String audience, String jti, Date issueTime, Date notBeforeTime,
                    Date expirationTime, Map<String, Object> claimSet) {
    public Token(String subject, String issuer, String audience, String jti, Date issueTime, Date notBeforeTime, Date expirationTime, Map<String, Object> claimSet) {
        this.subject = subject;
        this.issuer = issuer;
        this.audience = audience;
        this.jti = jti != null ? jti : UUID.randomUUID().toString();
        this.issueTime = issueTime != null ? issueTime : new Date();
        this.notBeforeTime = notBeforeTime != null ? notBeforeTime : new Date();
        this.expirationTime = expirationTime;
        this.claimSet = claimSet;
    }
}
