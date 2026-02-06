package io.vwis.keycloak.mappers.habilia;

public interface AccessTokenProvider {

    /**
     * Returns an access token. In the event that you don't have a token return null.
     */
    String getToken();

    /**
     * Refreshes the token and returns it. This call should be made synchronously.
     * In the event that the token could not be refreshed return null.
     */
    String refreshToken();
}
