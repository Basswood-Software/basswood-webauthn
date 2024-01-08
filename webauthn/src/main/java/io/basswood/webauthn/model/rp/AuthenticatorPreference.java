package io.basswood.webauthn.model.rp;

public final class AuthenticatorPreference {
    public enum Attestation{
        NONE, INDIRECT, DIRECT, ENTERPRISE
    }
    public enum Attachment{
        CROSS_PLATFORM, PLATFORM
    }
    public enum ResidentKey{
        DISCOURAGED, PREFERRED, REQUIRED
    }
    public enum UserVerification{
        DISCOURAGED, PREFERRED, REQUIRED
    }
}
