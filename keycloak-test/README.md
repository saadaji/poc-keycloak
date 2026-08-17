# keycloak-test

Integration test suite for the VEF Keycloak custom providers. Tests run against a real Keycloak instance started inside Docker/Podman via [Testcontainers](https://testcontainers.com/), with a headless Selenium Chromium browser for UI-driven flows.

## Stack

| Library | Purpose |
|---|---|
| JUnit 5 | Test runner |
| Testcontainers (core + JUnit 5) | Docker lifecycle management |
| `testcontainers-keycloak` (dasniko) | Keycloak-specific container wrapper |
| Testcontainers Selenium | Containerised browser (`seleniarm/standalone-chromium`) |
| Selenium WebDriver 4 | Browser automation |

## Prerequisites

- Docker (or Podman — see [Running with Podman](#running-with-podman)) accessible from the test JVM
- The `keycloak-image` module must be built first so that provider JARs are present in `../keycloak-image/providers/`

## Running the tests

```bash
# From the project root — builds all modules then runs tests
mvn clean install

# From this module only (providers must already be built)
mvn test -Dkeycloak.image.version=26.6.4
```

The Keycloak image version defaults to the `keycloak.version` property defined in the parent `pom.xml` and is forwarded to the container via the `keycloak.image.version` system property.

### Running with Podman

```bash
mvn test -Ppodman -Ddocker.host=unix:///run/user/$(id -u)/podman/podman.sock
```

## Test infrastructure

`KeycloakContainerTest` is the single test class. A shared `@BeforeAll` sets up:

- **KeycloakContainer** — starts `quay.io/keycloak/keycloak:<version>` with three pre-imported realms and all provider JARs mounted.
- **BrowserWebDriverContainer** — starts a headless Chromium container on the same Docker network (`keycloak` alias) so the browser can reach Keycloak by hostname.

A `@BeforeEach` hook hits the OIDC logout endpoint for every realm and clears all browser cookies to prevent SSO session leakage between tests.

### Test realms

| Realm | JSON file | Users |
|---|---|---|
| `test-realm` | `test-realm-realm.json` | `testuser / password` |
| `email-test-realm` | `email-test-realm-realm.json` | `emailuser / password` |
| `sms-test-realm` | `sms-test-realm-realm.json` | `smsuser / password` |

---

## Tests

### Standard auth flows

#### `keycloakAuthFlow`
**Tag:** `integration`

Verifies the standard **Authorization Code Flow** end-to-end using the Selenium browser.

1. Opens the `test-realm` authorisation URL for `test-client`.
2. Submits credentials (`testuser / password`).
3. Asserts that the browser is redirected to `http://example.com?code=…` — confirming Keycloak issued an authorisation code.

---

#### `keycloakClientCredentialsFlow`
**Tag:** `integration`

Verifies the **Client Credentials Flow** using a plain HTTP client (no browser).

1. Posts `grant_type=client_credentials` with `test-client` / `test-secret` to the `test-realm` token endpoint.
2. Asserts HTTP `200`.
3. Asserts the JSON response body contains `access_token`.

---

### Email 2FA tests

All email tests target `email-test-realm` / `email-test-client`. The Email OTP authenticator runs in **simulation mode**: instead of sending a real email it prints the code to the Keycloak container log, where the tests can extract it via regex (`SIMULATION MODE … Email code send to … is: <digits>`).

#### `shouldDisplayEmailOtpFormAfterLogin`
After a successful password login the Email OTP challenge form (`emailCode` input) must be displayed.

---

#### `shouldRejectInvalidEmailCode`
Submits the deliberately wrong code `000000`. Asserts that Keycloak rejects it and re-displays the OTP form (i.e. the `emailCode` input is still present).

---

#### `shouldResendEmailOtpCode`
Clicks the **Resend** button on the OTP form. Asserts that the OTP form remains visible with a refreshed state.

---

#### `shouldCompleteAuthenticationWithValidEmailCode`
Full happy-path Email 2FA flow:

1. Logs in with password → OTP form appears.
2. Polls Keycloak container logs (up to 15 s) for the simulated OTP code.
3. Submits the extracted code.
4. Asserts the browser is redirected to `http://example.com?code=…` — confirming successful second-factor authentication.

---

### SMS 2FA tests

All SMS tests target `sms-test-realm` / `sms-test-client`. The SMS authenticator runs in **simulation mode**: instead of sending a real SMS it prints the code to the container log (`SIMULATION MODE … Would send SMS … code is <digits>`).

#### `shouldDisplaySmsOtpFormAfterLogin`
After a successful password login the SMS OTP challenge form (`id="code"` input) must be displayed.

---

#### `shouldRejectInvalidSmsCode`
Submits the wrong code `000000`. Asserts the SMS form is re-displayed.

---

#### `shouldCompleteAuthenticationWithValidSmsCode`
Full happy-path SMS 2FA flow:

1. Logs in with password → SMS code form appears.
2. Polls container logs (up to 15 s) for the simulated OTP code.
3. Submits the extracted code.
4. Asserts the browser is redirected to `http://example.com?code=…`.

---

### PKCE tests (RFC 7636)

PKCE tests use `pkce-test-client` in `test-realm`. This client requires `code_challenge_method=S256`. All positive flows use the Selenium browser to complete the login form, then exchange the authorisation code via a direct HTTP call from the test runner.

#### `shouldCompleteFullPkceFlow`
**Happy path — valid `code_verifier`**

1. Generates a fresh PKCE pair: 32-byte random `code_verifier` (URL-safe base64) + SHA-256 `code_challenge`.
2. Drives the browser through the login form with `code_challenge` in the auth URL.
3. Extracts the authorisation code from the redirect URL.
4. Calls the token endpoint with the matching `code_verifier`.
5. Asserts HTTP `200` and that the response contains both `access_token` and `id_token`.

---

#### `shouldRejectTokenExchangeWithWrongCodeVerifier`
**Negative — mismatched `code_verifier`**

1. Generates PKCE pair A; obtains an authorisation code using pair A's `code_challenge`.
2. Generates a separate PKCE pair B and sends pair B's `code_verifier` in the token exchange.
3. Asserts HTTP `400` and that the body contains `invalid_grant`.

---

#### `shouldRejectTokenExchangeWithoutCodeVerifier`
**Negative — missing `code_verifier`**

1. Obtains an authorisation code using a valid `code_challenge`.
2. Posts to the token endpoint without any `code_verifier` parameter.
3. Asserts HTTP `400` and that the body contains `invalid_grant`.

---

#### `shouldRejectAuthRequestWithoutPkce`
**Negative — auth request without `code_challenge` when PKCE is required**

Sends an authorisation request to `pkce-test-client` that omits `code_challenge` entirely.  
Asserts that Keycloak either:
- redirects back with `error=invalid_request`, **or**
- returns an error page with `invalid_request` / `error` in the body.
