package io.basswood.webauthn.model.request;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author shamualr
 * @since 1.0
 */
@Entity(name = "webauthn_request_cache")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebAuthnRequestEntity {
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.of(10, ChronoUnit.MINUTES);
    @Id
    @Column(name = "requestId")
    private String requestId;
    @Column(name = "requestType")
    @Enumerated(EnumType.STRING)
    private RequestType requestType;
    @Column(name = "createdTime")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date createdTime = new Date();
    @Column(name = "expiryTime")
    @Temporal(TemporalType.TIMESTAMP)
    @Builder.Default
    private Date expiryTime = Date.from(Instant.now().plusMillis(DEFAULT_REQUEST_TIMEOUT.toMillis()));
    @Column(name = "request", columnDefinition = "json")
    private String request;
}
