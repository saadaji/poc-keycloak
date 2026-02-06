package io.vwis.keycloak.mappers.habilia;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HabiliaTokenProvider implements AccessTokenProvider {

    private static final Logger log = Logger.getLogger(HabiliaTokenProvider.class);

    private String authenticationUrl;
    private String clientId;
    private String clientSecret;

    private OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public HabiliaTokenProvider(String authenticationUrl, String clientId, String clientSecret) {
        this.authenticationUrl = authenticationUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getToken() {
        log.info("getToken");
        // Create the request body
        FormBody formBody = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build();

        // Build the request object, with method, headers
        Request request = new Request.Builder()
                .url(authenticationUrl)
                .method("POST", formBody)
                .build();

        // Perform the request, this potentially throws an IOException
        Response response = null;
        try {
            log.infof("authenticate : %s", request.url().url().toString());
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Read the body of the response into a hashmap
        Map<String, Object> responseMap = null;
        try {
            responseMap = new ObjectMapper().readValue(response.body().byteStream(), HashMap.class);
            log.infof("response : %s", responseMap);
            // Read the value of the "access_token" key from the hashmap
            String accessToken = (String) responseMap.get("access_token");
            log.infof("accessToken : %s", accessToken);
            return accessToken;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String refreshToken() {
        log.info("refreshToken");
        return getToken();
    }
}
