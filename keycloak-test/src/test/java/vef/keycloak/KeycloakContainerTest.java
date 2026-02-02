package vef.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class KeycloakContainerTest {
    private static final Network network = Network.newNetwork();
    // Integration test: Keycloak + Selenium
    private static KeycloakContainer keycloak;
    private static BrowserWebDriverContainer<?> browserContainer;

    @BeforeAll
    public static void setupKeycloakAndBrowser() {
        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.7")
                .withExposedPorts(8080, 9000)
                .withRealmImportFile("test-realm-realm.json")
                .withAdminUsername("admin")
                .withAdminPassword("admin")
                .withNetwork(network)
                .withNetworkAliases("keycloak")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("app")))
                .waitingFor(Wait.forListeningPort())
                //.withStartupTimeout(Duration.ofMinutes(10))
                .withVerboseOutput();
        keycloak.start();
        System.out.println(keycloak.getLogs());

        browserContainer = new BrowserWebDriverContainer<>()
                .withNetwork(network)
                .withCapabilities(new ChromeOptions())
                .dependsOn(keycloak)
                .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null);
        browserContainer.start();
    }

    @AfterAll
    public static void teardownKeycloakAndBrowser() {
        if (browserContainer != null) browserContainer.stop();
        if (keycloak != null) keycloak.stop();
    }

    @Test
    @Tag("integration")
    public void keycloakAuthFlow() throws Exception {
        String host = "keycloak";
        Integer port = 8080;
        String base = "http://" + host + ":8080";

        String authUrl = base + "/realms/test-realm/protocol/openid-connect/auth?client_id=test-client&response_type=code&redirect_uri=http://example.com&scope=openid";

        RemoteWebDriver driver = browserContainer.getWebDriver();
        driver.get(authUrl);

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
}
