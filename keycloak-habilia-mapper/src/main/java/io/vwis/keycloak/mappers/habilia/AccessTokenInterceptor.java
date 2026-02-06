package io.vwis.keycloak.mappers.habilia;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;

public class AccessTokenInterceptor implements Interceptor {

    private static final Logger log = Logger.getLogger(AccessTokenInterceptor.class);
    private AccessTokenProvider tokenProvider;

    public AccessTokenInterceptor(AccessTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        log.info("Intercept");
        String token = tokenProvider.getToken();
        log.infof("token : %s", token);
        if (token == null) {
            return chain.proceed(chain.request());
        } else {
            log.info("replaying request with authz");
            Request authorizationRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer" + token)
                    .build();
            return chain.proceed(authorizationRequest);
        }
    }
}
