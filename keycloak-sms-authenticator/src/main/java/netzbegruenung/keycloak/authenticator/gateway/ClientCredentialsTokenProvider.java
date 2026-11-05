package netzbegruenung.keycloak.authenticator.gateway;

import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches (and caches) an OAuth2 access token using the client_credentials grant.
 * Tokens are cached per (tokenUrl, clientId) and refreshed shortly before they expire.
 */
class ClientCredentialsTokenProvider {

	private static final Logger logger = Logger.getLogger(ClientCredentialsTokenProvider.class);
	private static final long EXPIRY_SKEW_SECONDS = 30;
	private static final long DEFAULT_LIFETIME_SECONDS = 60;
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private static final Map<String, CachedToken> CACHE = new ConcurrentHashMap<>();
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

	private record CachedToken(String value, long expiresAtMillis) {
		boolean isValid() {
			return System.currentTimeMillis() < expiresAtMillis;
		}
	}

	private final String tokenUrl;
	private final String clientId;
	private final String clientSecret;
	private final String scope;

	ClientCredentialsTokenProvider(String tokenUrl, String clientId, String clientSecret, String scope) {
		this.tokenUrl = tokenUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scope = scope;
	}

	String getToken() throws IOException, InterruptedException {
		String key = tokenUrl + '|' + clientId;
		CachedToken cached = CACHE.get(key);
		if (cached != null && cached.isValid()) {
			return cached.value();
		}
		synchronized (CACHE) {
			cached = CACHE.get(key);
			if (cached != null && cached.isValid()) {
				return cached.value();
			}
			CachedToken fresh = fetch();
			CACHE.put(key, fresh);
			return fresh.value();
		}
	}

	@SuppressWarnings("unchecked")
	private CachedToken fetch() throws IOException, InterruptedException {
		StringBuilder form = new StringBuilder("grant_type=client_credentials");
		if (scope != null && !scope.isBlank()) {
			form.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
		}

		String basic = Base64.getEncoder().encodeToString(
			(URLEncoder.encode(clientId, StandardCharsets.UTF_8) + ':' + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8))
				.getBytes(StandardCharsets.UTF_8));

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(tokenUrl))
			.timeout(TIMEOUT)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.header("Accept", "application/json")
			.header("Authorization", "Basic " + basic)
			.POST(HttpRequest.BodyPublishers.ofString(form.toString()))
			.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Token endpoint " + tokenUrl + " returned HTTP " + response.statusCode());
		}

		Map<String, Object> body = JsonSerialization.readValue(response.body(), Map.class);
		Object accessToken = body.get("access_token");
		if (accessToken == null) {
			throw new IOException("Token endpoint " + tokenUrl + " response has no access_token");
		}
		long lifetime = body.get("expires_in") instanceof Number n ? n.longValue() : DEFAULT_LIFETIME_SECONDS;
		long expiresAt = System.currentTimeMillis() + Math.max(0, lifetime - EXPIRY_SKEW_SECONDS) * 1000;
		logger.debugf("Obtained access token for client %s (expires in %ds)", clientId, lifetime);
		return new CachedToken(accessToken.toString(), expiresAt);
	}
}
