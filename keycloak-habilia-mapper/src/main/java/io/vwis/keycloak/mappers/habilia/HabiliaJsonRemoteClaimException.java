package io.vwis.keycloak.mappers.habilia;

public class HabiliaJsonRemoteClaimException extends RuntimeException {

    public HabiliaJsonRemoteClaimException(String message, String url) {
        super(message + " - Configured URL: " + url);
    }

    public HabiliaJsonRemoteClaimException(String message, String url, Throwable cause) {
        super(message + " - Configured URL: " + url, cause);
    }

}
