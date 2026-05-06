package tech.arhr.quingo.auth_service.enums;

import lombok.Getter;

public enum VerificationTokenType {
    VERIFY_EMAIL("verify-email"),
    RESET_PASSWORD("reset-password"),
    ;

    @Getter
    private final String prefix;

    VerificationTokenType(String s) {
        this.prefix = s;
    }
}
