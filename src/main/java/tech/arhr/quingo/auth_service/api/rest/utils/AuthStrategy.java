package tech.arhr.quingo.auth_service.api.rest.utils;

import tech.arhr.quingo.auth_service.exceptions.auth.InvalidAuthStrategyException;

public enum AuthStrategy {
    COOKIE,
    JSON;

    public static AuthStrategy fromString(String strategy) {
        if (strategy == null) return COOKIE;

        try {
            return valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAuthStrategyException(strategy);
        }
    }

    public boolean isJson() {
        return this == JSON;
    }

    public boolean isCookie() {
        return this == COOKIE;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
