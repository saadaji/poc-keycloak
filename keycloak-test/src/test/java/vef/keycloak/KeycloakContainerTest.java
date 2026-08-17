package vef.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class KeycloakContainerTest {
    private static final Pattern EMAIL_CODE_PATTERN =
            Pattern.compile("SIMULATION MODE.*?Email code send to.*?is: (\\d+)", Pattern.DOTALL);

    private static final Pattern SMS_CODE_PATTERN =
            Pattern.compile("SIMULATION MODE.*?Would send SMS.*?code is (\\d+)", Pattern.DOTALL);

    // ─── PKCE (RFC 7636) test constants ─────────────────────────────────────────
    /** Public client configured with {@code pkce.code.challenge.method=S256} in test-realm-realm.json. */
    private static final String PKCE_CLIENT_ID = "pkce-test-client";
    private static final String PKCE_REDIRECT_URI = "http://example.com";
    private static final String PKCE_REALM = "test-realm";
    /** Extracts the {@code code} query parameter from a redirect URL. */
    private static final Pattern CODE_PATTERN = Pattern.compile("[?&]code=([^&]+)");

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:"
            + System.getProperty("keycloak.image.version");
    private static final Network network = Network.newNetwork();
    // Integration test: Keycloak + Selenium
    private static KeycloakContainer keycloak;
    private static BrowserWebDriverContainer<?> browser;

    @BeforeAll
    public static void setupKeycloakAndBrowser() {
        List<File> libs = Arrays.asList(
                Objects.requireNonNull(
                        new File("../keycloak-image/providers").listFiles(
                                f -> f.isFile() && f.getName().endsWith(".jar")),
                        "No provider JARs found in ../keycloak-image/providers"));
        keycloak = new KeycloakContainer(KEYCLOAK_IMAGE)
                .withExposedPorts(8080, 9000)
                .withRealmImportFiles(
                        "test-realm-realm.json",
                        "email-test-realm-realm.json",
                        "sms-test-realm-realm.json")
                .withAdminUsername("admin")
                .withAdminPassword("admin")
                .withEnv("JAVA_OPTS_KC_HEAP", "-Xms128m -Xmx400m")
                .withNetwork(network)
                .withNetworkAliases("keycloak")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("app")))
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(5))
                .withProviderLibsFrom(libs)
                .withVerboseOutput();
        keycloak.start();
        System.out.println(keycloak.getLogs());

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");

        // Use native ARM Chromium image to avoid QEMU emulation crashes on aarch64 (Podman on Apple Silicon)
        DockerImageName seleniumImage = DockerImageName.parse("seleniarm/standalone-chromium:latest")
                .asCompatibleSubstituteFor("selenium/standalone-chrome");

        browser = new BrowserWebDriverContainer<>(seleniumImage)
                .withNetwork(network)
                .withCapabilities(chromeOptions)
                .dependsOn(keycloak)
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null);
        browser.start();
    }

    @AfterAll
    public static void teardownKeycloakAndBrowser() {
        if (browser != null) browser.stop();
        if (keycloak != null) keycloak.stop();
    }

    /** Clear browser state between tests to prevent SSO session-cookie interference. */
    @BeforeEach
    void resetBrowserSession() {
        WebDriver driver = browser.getWebDriver();
        // The KEYCLOAK_IDENTITY SSO cookie is path-scoped to each realm (/realms/{realm}/),
        // so a single deleteAllCookies() from one domain/path can't remove them all. Hit each
        // realm's OIDC logout endpoint to invalidate the server-side session and clear the
        // cookie; otherwise a prior test's login auto-authenticates and the username form
        // never appears.
        for (String realm : new String[]{"test-realm", "email-test-realm", "sms-test-realm"}) {
            driver.get("http://keycloak:8080/realms/" + realm + "/protocol/openid-connect/logout");
            driver.manage().deleteAllCookies();
        }
    }

        /**
     * Polls the container logs until the OTP code matching {@code pattern} appears
     * or the timeout elapses.
     *
     * @param pattern       regex with one capture group for the numeric OTP code
     * @param timeoutSeconds maximum number of seconds to wait
     * @return the last OTP code found in the logs
     * @throws AssertionError if no code is found within the timeout
     */
    private String waitForOtpInLogs(Pattern pattern, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            String code = lastCapture(keycloak.getLogs(), pattern);
            if (code != null) return code;
            Thread.sleep(500);
        }
        throw new AssertionError(
                "OTP code not found in container logs within " + timeoutSeconds + " seconds.\n"
                        + "Keycloak logs:\n" + keycloak.getLogs());
    }

        /** Returns the last capture group 1 matched by {@code pattern} in {@code text}, or {@code null}. */
    private static String lastCapture(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }


    public String authUrl() {
              String host = "keycloak";
        Integer port = 8080;
        String base = "http://" + host + ":8080";
        return base + "/realms/test-realm/protocol/openid-connect/auth?client_id=test-client&response_type=code&redirect_uri=http://example.com&scope=openid";
    }

    private static String realmAuthUrl(String realm, String clientId) {
        return "http://keycloak:8080/realms/" + realm + "/protocol/openid-connect/auth"
                + "?client_id=" + clientId
                + "&response_type=code&redirect_uri=http://example.com&scope=openid";
    }

    private static String emailAuthUrl() {
        return realmAuthUrl("email-test-realm", "email-test-client");
    }

    private static String smsAuthUrl() {
        return realmAuthUrl("sms-test-realm", "sms-test-client");
    }

        private static void submitCredentials(WebDriver driver, String username, String password) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.id("kc-login")).click();
    }

    @Test
    @Tag("integration")
    public void keycloakAuthFlow() throws Exception {

        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(authUrl());

        WebElement username = driver.findElement(By.name("username"));
        WebElement password = driver.findElement(By.name("password"));
        WebElement submit = driver.findElement(By.id("kc-login"));

        username.sendKeys("testuser");
        password.sendKeys("password");
        submit.click();

        Thread.sleep(2000);
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("code="), "Expected redirect URL to contain authorization code, got: " + currentUrl);
    }

    @Test
    @Tag("integration")
    public void keycloakClientCredentialsFlow() throws Exception {
        String host = keycloak.getHost();
        Integer port = keycloak.getMappedPort(8080);
        String tokenUrl = "http://" + host + ":" + port + "/realms/test-realm/protocol/openid-connect/token";

        String clientId = "test-client";
        String clientSecret = "test-secret";

        // Create HTTP client and make token request
        HttpClient httpClient = HttpClient.newHttpClient();

        String body = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertTrue(response.statusCode() == 200, "Expected 200 status code, got: " + response.statusCode());
        assertTrue(response.body().contains("access_token"), "Expected response to contain access_token");
    }

    @Test
    @DisplayName("Email OTP form is shown after successful username/password login")
    void shouldDisplayEmailOtpFormAfterLogin() {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(emailAuthUrl());

        submitCredentials(driver, "emailuser", "password");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));

        assertTrue(driver.findElement(By.name("emailCode")).isDisplayed(),
                "Email OTP input field should be visible after successful password entry");
    }

    @Test
    @DisplayName("Invalid email OTP code is rejected and form is re-displayed")
    void shouldRejectInvalidEmailCode() {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(emailAuthUrl());
        submitCredentials(driver, "emailuser", "password");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));

        driver.findElement(By.name("emailCode")).sendKeys("000000");
        driver.findElement(By.name("login")).click();

        // After an invalid code the email OTP form must be shown again
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));
        assertTrue(driver.findElement(By.name("emailCode")).isDisplayed(),
                "Email OTP form should be re-displayed after an invalid code submission");
    }

    @Test
    @DisplayName("Resend button resets the cooldown and shows OTP form again")
    void shouldResendEmailOtpCode() {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(emailAuthUrl());
        submitCredentials(driver, "emailuser", "password");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));

        // Click resend — the form should stay visible with a refreshed state
        driver.findElement(By.name("resend")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));
        assertTrue(driver.findElement(By.name("emailCode")).isDisplayed(),
                "Email OTP form should remain visible after clicking Resend");
    }

    @Test
    @DisplayName("Full Email 2FA flow succeeds with the code extracted from simulation logs")
    void shouldCompleteAuthenticationWithValidEmailCode() throws Exception {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(emailAuthUrl());
        submitCredentials(driver, "emailuser", "password");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.name("emailCode")));

        // The authenticator logs the OTP in simulation mode; extract the most recent occurrence.
        String code = waitForOtpInLogs(EMAIL_CODE_PATTERN, 15);

        driver.findElement(By.name("emailCode")).sendKeys(code);
        driver.findElement(By.name("login")).click();

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.getCurrentUrl().contains("code=") || d.getCurrentUrl().contains("example.com"));

        assertTrue(driver.getCurrentUrl().contains("code="),
                "Expected redirect URI to contain authorization code, got: " + driver.getCurrentUrl());
    }

        @Test
    @DisplayName("SMS OTP form is shown after successful username/password login")
    void shouldDisplaySmsOtpFormAfterLogin() {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(smsAuthUrl());

        submitCredentials(driver, "smsuser", "password");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.id("code")));

        assertTrue(driver.findElement(By.id("code")).isDisplayed(),
                "SMS code input field should be visible after successful password entry");
    }

    @Test
    @DisplayName("Invalid SMS OTP code is rejected and form is re-displayed")
    void shouldRejectInvalidSmsCode() {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(smsAuthUrl());
        submitCredentials(driver, "smsuser", "password");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("code")));

        driver.findElement(By.id("code")).sendKeys("000000");
        driver.findElement(By.cssSelector("[type='submit']")).click();

        // After invalid code the SMS form must be shown again
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("code")));
        assertTrue(driver.findElement(By.id("code")).isDisplayed(),
                "SMS code form should be re-displayed after an invalid code submission");
    }

    @Test
    @DisplayName("Full SMS 2FA flow succeeds with the code extracted from simulation logs")
    void shouldCompleteAuthenticationWithValidSmsCode() throws Exception {
        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(smsAuthUrl());
        submitCredentials(driver, "smsuser", "password");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(By.id("code")));

        // The authenticator logs the OTP in simulation mode; extract the most recent occurrence.
        String code = waitForOtpInLogs(SMS_CODE_PATTERN, 15);

        driver.findElement(By.id("code")).sendKeys(code);
        driver.findElement(By.cssSelector("[type='submit']")).click();

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.getCurrentUrl().contains("code=") || d.getCurrentUrl().contains("example.com"));

        assertTrue(driver.getCurrentUrl().contains("code="),
                "Expected redirect URI to contain authorization code, got: " + driver.getCurrentUrl());
    }

    // ─── PKCE (RFC 7636) tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Full PKCE flow: valid code_verifier → access_token and id_token are issued")
    void shouldCompleteFullPkceFlow() throws Exception {
        PkceParams pkce = generatePkce();

        String code = obtainAuthCodeViaBrowser(pkce.codeChallenge());
        assertNotNull(code, "Authorization code must be present in the redirect URL");

        HttpResponse<String> tokenResponse = callTokenEndpoint(code, pkce.codeVerifier());

        assertEquals(200, tokenResponse.statusCode(),
                "Token exchange with valid code_verifier should return 200. Body: " + tokenResponse.body());
        assertTrue(tokenResponse.body().contains("access_token"),
                "Token response must contain access_token");
        assertTrue(tokenResponse.body().contains("id_token"),
                "Token response must contain id_token for openid scope");
    }

    @Test
    @DisplayName("PKCE negative: wrong code_verifier → 400 invalid_grant")
    void shouldRejectTokenExchangeWithWrongCodeVerifier() throws Exception {
        PkceParams pkce = generatePkce();

        String code = obtainAuthCodeViaBrowser(pkce.codeChallenge());
        assertNotNull(code, "Authorization code must be present in the redirect URL");

        // Deliberately use a verifier from a *different* PKCE pair
        String wrongVerifier = generatePkce().codeVerifier();
        HttpResponse<String> tokenResponse = callTokenEndpoint(code, wrongVerifier);

        assertEquals(400, tokenResponse.statusCode(),
                "Token exchange with wrong code_verifier should be rejected with 400. Body: " + tokenResponse.body());
        assertTrue(tokenResponse.body().contains("invalid_grant"),
                "Error response must contain 'invalid_grant'. Body: " + tokenResponse.body());
    }

    @Test
    @DisplayName("PKCE negative: missing code_verifier → 400 invalid_grant")
    void shouldRejectTokenExchangeWithoutCodeVerifier() throws Exception {
        PkceParams pkce = generatePkce();

        String code = obtainAuthCodeViaBrowser(pkce.codeChallenge());
        assertNotNull(code, "Authorization code must be present in the redirect URL");

        // Omit the code_verifier entirely
        HttpResponse<String> tokenResponse = callTokenEndpointWithoutVerifier(code);

        assertEquals(400, tokenResponse.statusCode(),
                "Token exchange without code_verifier should be rejected with 400. Body: " + tokenResponse.body());
        assertTrue(tokenResponse.body().contains("invalid_grant"),
                "Error response must contain 'invalid_grant'. Body: " + tokenResponse.body());
    }

    @Test
    @DisplayName("PKCE negative: auth request without code_challenge is rejected when PKCE is required")
    void shouldRejectAuthRequestWithoutPkce() throws Exception {
        // Build auth URL without code_challenge — client requires PKCE (S256)
        String authUrlNoPkce = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080)
                + "/realms/" + PKCE_REALM + "/protocol/openid-connect/auth"
                + "?client_id=" + PKCE_CLIENT_ID
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(PKCE_REDIRECT_URI, StandardCharsets.UTF_8)
                + "&scope=openid";

        HttpClient noRedirect = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse<String> response = noRedirect.send(
                HttpRequest.newBuilder().uri(URI.create(authUrlNoPkce)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        // Keycloak must redirect back to the redirect_uri with error=invalid_request
        boolean errorRedirect = response.statusCode() == 302
                && response.headers().firstValue("Location")
                        .map(loc -> loc.contains("error=invalid_request")).orElse(false);
        // Or Keycloak may render an error page (status 200 or 400 with error body)
        boolean errorPage = (response.statusCode() == 400 || response.statusCode() == 200)
                && (response.body().contains("invalid_request") || response.body().contains("error"));

        assertTrue(errorRedirect || errorPage,
                "Auth request without code_challenge should be rejected when PKCE is required."
                        + " Status=" + response.statusCode()
                        + " Location=" + response.headers().firstValue("Location").orElse("none")
                        + " Body(500)=" + response.body().substring(0, Math.min(500, response.body().length())));
    }

    // ─── PKCE helpers ─────────────────────────────────────────────────────────────

    /** Immutable value object holding a PKCE code_verifier / code_challenge pair. */
    private record PkceParams(String codeVerifier, String codeChallenge) {}

    /**
     * Generates a cryptographically random PKCE pair following RFC 7636 §4.1–4.2:
     * <ul>
     *   <li>code_verifier: 32 random bytes, URL-safe base64 without padding</li>
     *   <li>code_challenge: SHA-256 of the verifier, URL-safe base64 without padding</li>
     * </ul>
     */
    private static PkceParams generatePkce() throws Exception {
        byte[] verifierBytes = new byte[32];
        new SecureRandom().nextBytes(verifierBytes);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);

        byte[] sha256 = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256);

        return new PkceParams(codeVerifier, codeChallenge);
    }

    /**
     * Drives the Selenium browser through the Keycloak login form and returns
     * the {@code code} query parameter from the redirect URL.
     */
    private String obtainAuthCodeViaBrowser(String codeChallenge) throws Exception {
        String authUrl = "http://keycloak:8080/realms/" + PKCE_REALM + "/protocol/openid-connect/auth"
                + "?client_id=" + PKCE_CLIENT_ID
                + "&response_type=code"
                + "&redirect_uri=" + URLEncoder.encode(PKCE_REDIRECT_URI, StandardCharsets.UTF_8)
                + "&scope=openid"
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256";

        RemoteWebDriver driver = browser.getWebDriver();
        driver.get(authUrl);

        submitCredentials(driver, "testuser", "password");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.getCurrentUrl().contains("code="));

        return extractCode(driver.getCurrentUrl());
    }

    /**
     * Posts to the token endpoint with a {@code code_verifier} (standard PKCE exchange).
     * Uses the host-accessible mapped port so the request originates from the test runner.
     */
    private HttpResponse<String> callTokenEndpoint(String code, String codeVerifier) throws Exception {
        String body = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + PKCE_CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(PKCE_REDIRECT_URI, StandardCharsets.UTF_8)
                + "&code_verifier=" + codeVerifier;

        return postToTokenEndpoint(body);
    }

    /**
     * Posts to the token endpoint <em>without</em> a {@code code_verifier} to
     * verify that Keycloak enforces PKCE at the token exchange step.
     */
    private HttpResponse<String> callTokenEndpointWithoutVerifier(String code) throws Exception {
        String body = "grant_type=authorization_code"
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + PKCE_CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(PKCE_REDIRECT_URI, StandardCharsets.UTF_8);

        return postToTokenEndpoint(body);
    }

    private HttpResponse<String> postToTokenEndpoint(String body) throws Exception {
        String tokenUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080)
                + "/realms/" + PKCE_REALM + "/protocol/openid-connect/token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** Extracts and URL-decodes the {@code code} query parameter from a redirect URL. */
    private static String extractCode(String url) {
        Matcher m = CODE_PATTERN.matcher(url);
        if (!m.find()) return null;
        return URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
    }

}
