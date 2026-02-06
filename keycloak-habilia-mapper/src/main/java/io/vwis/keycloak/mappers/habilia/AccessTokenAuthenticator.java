package io.vwis.keycloak.mappers.habilia;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.jboss.logging.Logger;

import javax.annotation.Nullable;
import java.io.IOException;

public class AccessTokenAuthenticator implements Authenticator {

    private static final Logger log = Logger.getLogger(AccessTokenAuthenticator.class);
    private AccessTokenProvider tokenProvider;

    public AccessTokenAuthenticator(AccessTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }
    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response) throws IOException {
        log.info("Authenticate");
        String token = tokenProvider.getToken();
        log.infof("token : %s", token);
        if (token == null) {
            return null;
        }

        synchronized (this) {
            String newToken = tokenProvider.getToken();
            // Check if the request made was previously made as an authenticated request.
            if (response.request().header("Authorization") != null) {
                log.info("previous request alredy have an Authorization header");
                // If the token has changed since the request was made, use the new token.
                if (!token.equals(newToken)) {
                    log.info("there is a new token");
                    return response.request()
                            .newBuilder()
                            .removeHeader("Authorization")
                            .addHeader("Authorization", "Bearer " + newToken)
                            .build();
                }

                String updatedToken = tokenProvider.refreshToken();
                log.infof("updatedToken : %s", updatedToken);
                if (updatedToken == null) {
                    return null;
                }

                // Retry the request with the new token.
                return response.request()
                        .newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer " + updatedToken)
                        .build();
            }
        }

        return null;
    }
}
