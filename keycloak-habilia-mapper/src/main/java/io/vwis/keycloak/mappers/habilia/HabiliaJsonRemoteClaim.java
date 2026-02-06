package io.vwis.keycloak.mappers.habilia;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class HabiliaJsonRemoteClaim extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final Logger log = Logger.getLogger(HabiliaJsonRemoteClaim.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private final static String REMOTE_URL = "remote.url";
    private final static String REMOTE_AUTH_URL = "remote.auth.url";
    private final static String REMOTE_PARAMETERS_CLIENTID = "remote.parameters.clientid";

    private final static String REMOTE_PARAMETERS_CLIENTSECRET = "remote.parameters.clientsecret";

    private final static String REMOTE_PARAMETERS_MODULEID = "remote.parameters.moduleid";

    /**
     * Inner configuration to cache retrieved authorization for multiple tokens
     */
    private final static String HABILIA_AUTHORIZATION_ATTR = "habilia-authorizations";

    /*
     * ID of the token mapper.
     * Must be public
     */
    public final static String PROVIDER_ID = "habilia-json-remote-claim";

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, HabiliaJsonRemoteClaim.class);
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        ProviderConfigProperty property;

        // Client_id
        property = new ProviderConfigProperty();
        property.setName(REMOTE_PARAMETERS_CLIENTID);
        property.setLabel("Client ID");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("The client_id used to authenticate to Habilia (param: client_id).");
        property.setDefaultValue("XXXXXXXXXXXXXXX");
        configProperties.add(property);

        // Client_secret
        property = new ProviderConfigProperty();
        property.setName(REMOTE_PARAMETERS_CLIENTSECRET);
        property.setLabel("Client secret");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("The client_secret used to authenticate to Habilia (param: client_secret).");
        property.setDefaultValue("");
        property.setSecret(true);
        configProperties.add(property);

        // Module_id
        property = new ProviderConfigProperty();
        property.setName(REMOTE_PARAMETERS_MODULEID);
        property.setLabel("Module id to query");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Send the client_id as query parameter (param: client_id).");
        property.setDefaultValue("XXXXXXX");
        configProperties.add(property);

        // URL
        property = new ProviderConfigProperty();
        property.setName(REMOTE_URL);
        property.setLabel("URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Full URL of the remote service endpoint.");
        configProperties.add(property);

        // URL
        property = new ProviderConfigProperty();
        property.setName(REMOTE_AUTH_URL);
        property.setLabel("Authentication URL");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Full URL of the remote service endpoint for authentication.");
        configProperties.add(property);

    }

    @Override
    public String getDisplayCategory() {
        return "Token mapper";
    }

    @Override
    public String getDisplayType() {
        return "Habilia JSON Remote claim";
    }

    @Override
    public String getHelpText() {
        return "Retrieve authorization to include from Habilia endpoint.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        JsonNode claims = clientSessionCtx.getAttribute(HABILIA_AUTHORIZATION_ATTR, JsonNode.class);
        if (claims == null) {
            claims = getRemoteAuthorizations(mappingModel, userSession);
            clientSessionCtx.setAttribute(HABILIA_AUTHORIZATION_ATTR, claims);
        }

        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, claims);
    }

    /**
     * Deprecated, added for older versions
     * <p>
     * Caution: This version does not allow to minimize request number
     *
     * @deprecated override {@link #setClaim(IDToken, ProtocolMapperModel, UserSessionModel, KeycloakSession, ClientSessionContext)} instead.
     */
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        JsonNode claims = getRemoteAuthorizations(mappingModel, userSession);
        OIDCAttributeMapperHelper.mapClaim(token, mappingModel, claims);
    }

    private JsonNode getRemoteAuthorizations(ProtocolMapperModel mappingModel, UserSessionModel userSession) {

        String url = mappingModel.getConfig().get(REMOTE_URL) + "/services/v1/habilitations/modules/" + mappingModel.getConfig().get(REMOTE_PARAMETERS_MODULEID) + "/users/" + userSession.getLoginUsername() + "/";
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("view", "full");

        // Build the request object, with method, headers
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        // Perform the request, this potentially throws an IOException
        okhttp3.Response response = null;
        try {
            final HabiliaTokenProvider tokenProvider = new HabiliaTokenProvider(
                    mappingModel.getConfig().get(REMOTE_AUTH_URL),
                    mappingModel.getConfig().get(REMOTE_PARAMETERS_CLIENTID),
                    mappingModel.getConfig().get(REMOTE_PARAMETERS_CLIENTSECRET)
            );

            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .authenticator(new AccessTokenAuthenticator(tokenProvider))
                    .addInterceptor(new AccessTokenInterceptor(tokenProvider))
                    .build();

            log.infof("habilitation request to %s", request.url().url().toString());
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Check response status
        if (response.code() != 200) {
            response.close();
            throw new HabiliaJsonRemoteClaimException("Wrong status received for remote claim - Expected: 200, Received: " + response.code(), url);
        }

        // Bind JSON response
        try {
            JsonNode jsonNode = new ObjectMapper().readValue(response.body().byteStream(), JsonNode.class);
            // we only keep the accesses part
            jsonNode = jsonNode.get("accesses");
            return jsonNode;
        } catch (JsonMappingException e) {
            throw new HabiliaJsonRemoteClaimException("Error when parsing response for remote claim", url, e);
        } catch (JsonParseException e) {
            throw new HabiliaJsonRemoteClaimException("Error when parsing response for remote claim", url, e);
        } catch (IOException e) {
            throw new HabiliaJsonRemoteClaimException("Error when parsing response for remote claim", url, e);
        } catch (RuntimeException e) {
            // exceptions are thrown to prevent token from being delivered without all information
            throw new HabiliaJsonRemoteClaimException("Error when parsing response for remote claim", url, e);
        } finally {
            response.close();
        }

    }
}
