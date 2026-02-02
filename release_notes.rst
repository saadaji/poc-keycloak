Keycloak Veolia Releases

Keycloak 26.0.7 (2025-01-16)
----------------------------


Keycloak 21.1.2.V2 (2024-04-22)
-------------------------------

Adaptation Lumapps V2
^^^^^^^^^^^^^^^^^^^^^
ajout quarkus.http.limits.max-initial-line-length=8192
ajout quarkus.http.limits.max-form-attribute-size=1M



Keycloak 21.1.2.V1 (2023-07-31)
-------------------------------

Adaptation
^^^^^^^^^^
retrait option: max_bundle_size=64K    dans les CACHES


Bug Fixes
^^^^^^^^^
21.1.1
#17165 Issue with "User-Initiated Action Lifespan" keycloak admin/ui
#19080 Vulnerable packages and or dependencies found in keycloak 21.0.1 quarkus distribution keycloak dist/quarkus
#19286 CVE-2022-1471 keycloak dependencies
#19491 Cannot set initial password for new users when using a custom UserFederation keycloak
#19689 SAML Encryption: Missing Support for http://www.w3.org/2009/xmlenc11#rsa-oaep keycloak saml
#19835 Keycloak issues on edge and after chrome upgarde to 112 (with experimental features) keycloak oidc
#19865 Enabling Dynamic Scope missing in UI keycloak admin/ui
#19879 Incorrect function is used in 'keycloak-admin-client' library in getToken function keycloak adapter/javascript
#19883 Saving client admin-cli in master realm gives a javascript error keycloak admin/ui
#19966 Paginating on the group tree view doesn't work keycloak admin/ui
#19974 Dropdown options on Documentation pointing to 21.1 endpoint instead of latest and throwing 404 when clicking on it. keycloak docs
#19981 Keycloak 21.1.1: Paging and filtering not working in "Assign roles" popup for Groups keycloak admin/ui
#19999 Keycloak 21.1.1: filter on Sessions gets stuck keycloak admin/ui
#20032 Processing of env variable references in config file broken keycloak dist/quarkus
#20068 LDAP Mapper Action Menu Error keycloak admin/ui
#20087 Event-Type: "User info request error" does not work keycloak admin/ui
#20096 Create new user UI: username is not marked with an asterisk keycloak admin/ui
#20140 role filter has no effect on roles list keycloak admin/ui
#20143 required fields don't show errors when user profile is enabled keycloak account/ui
#20258 OTP devices are not shown in the admin UI keycloak admin/ui
#20307 Test `InternationalizationTest` fails in CI keycloak testsuite
#20370 Deleting a client scope in the Admin UI should redirect to the list of ClientScopes keycloak admin/ui
#20379 SAML Protocol Mapper's NameIDFormat is null keycloak admin/ui
#20515 Headers is not defined keycloak admin/client-js
#20663 Fix for certificate revalidation keycloak

21.1.0
#8849 service-account leaking in get users API with "exact" query parameter set keycloak admin/api
#9564 Authentication Flow ID not imported keycloak core
#9896 Override of SSO Session Max for client does not work keycloak oidc
#9959 Unexpected invalid_grant error on offline session refresh when maximum number of offline sessions is configured keycloak storage
#10164 id_token_hint for external IDP not sent after token expiry keycloak oidc
#10412 Token contains old DB values with "Always Read Value From LDAP" mapper setting keycloak ldap
#11330 Theme can auto-select rememberMe even if disabled in a realm keycloak authentication
#11340 authentication checks cause 'Cookie not found' error keycloak authentication
#11517 POST /{realm}/users/{id}/role-mappings/realm is returning 500 keycloak core
#11730 LDAP user attribute is not updated in local database keycloak ldap
#12048 Items in dropdown menu for sharing resources are not visible keycloak account/ui
#12738 Revoking consent breaks for certain client IDs keycloak account/ui
#13835 Remove `ClearExpiredUserSessions` from services module keycloak storage
#14280 Subject's common name user identity extractor doesn't work with some certificate with RDN multi-valued keycloak authentication
#14613 414 Request-URI Too Long keycloak dist/quarkus
#14650 ciba authentication policy not found in keycloak 19 keycloak oidc
#14932 Default 'first broker login' default first login flow for identity providers ignores realm user registration settings keycloak docs
#14933 jwks endpoint for X/Y coordinates in EC keypair can return less bytes than expected keycloak oidc
#15098 IDENTITY_PROVIDER_FIRST_LOGIN is never triggered keycloak identity-brokering
#15476 NPE on welcome page if setting spi-theme-default and not providing theme keycloak core
#15624 UserInfo: Role name mapper is not respected for user info endpoint keycloak core
#16329 Service Accounts Client must create the Client ID mapper with Token Claim Name as client_id keycloak oidc
#16448 Failed to obtain JDBC connection with built-in H2 in start-dev keycloak storage
#16484 When hitting the account client with the referrer parameter ,the AccountConsole doesn't support the relative Client URLs keycloak account/api
#16587 Regression related to redirect url with port 80 keycloak oidc
#16844 Get UserInfo return 401 Unauthorized keycloak oidc
#16848 New user from identity provider not having attribute mapped to user federation (LDAP) keycloak ldap
#16851 v20.0.2 attempts to URL decode same string up to 5 times for unclear reasons keycloak core
#16888 Getting notification with unknown error when trying to create duplicated sub group. keycloak admin/api
#16965 direct naked impersonation documentation is wrong keycloak token-exchange
#17187 Docker auth: IllegalArgumentException on multiple resource scopes keycloak authentication
#17242 Typo in Outgoing HTTP requests documentation keycloak docs
#17253 Container image from FIPS docs doesn't work keycloak core
#17322 Disabling features with disabled dependencies fails "Feature account2 depends on disabled feature account-api" keycloak core
#17359 Connection string for ldap user federation with multiple hosts no longer supported keycloak core
#17374 User session limit make account console crash and logout the user keycloak authentication
#17403 Keycloak 21.0.1 - Paging and filtering not working in "Assign roles" popup" keycloak admin/ui
#17439 [User Profile Enabled] Email/Password fields disappear from registration when Email as Username is on keycloak user-profile
#17441 Redirect loop with authentication success but access denied at default identity provider keycloak identity-brokering
#17456 Bug in SAML Redirect Binding with 2 validating certificates keycloak saml
#17539 Stepup issue on "remember_me" authentication : alreadyLoggedIn keycloak authentication
#17549 SAML Signature metadata loses certificate info keycloak saml
#17561 group don't have any clickable link even though it have the access right permission on UI keycloak admin/ui
#17569 Theme resource common path is always /keycloak/common keycloak core
#17587 User with "view-clients" role cannot view credentials in Admin Console, but can still use the API to fetch them. keycloak admin/ui
#17588 admin-ui: authz unable to access child group when using fine grained auth keycloak admin/ui
#17591 Username field when creating user when email is set as username keycloak admin/ui
#17592 Admin console doesn't work in case realm name changed to name with space keycloak admin/ui
#17620 /users/count endpoint with search field has different behavior than /users query endpoint keycloak storage
#17635 Error creating realm keycloak admin/ui
#17671 docker image 21.0.1 lacks a Javascript engine keycloak core
#17686 Invalid Frontend URL leads to NullPointerException in OIDC Endpoints keycloak oidc
#17808 "SAML signature key name" attribute is not well forged keycloak admin/ui
#17811 Identity Provider hard coded role mapper does not allow selection of all roles keycloak admin/ui
#17850 New Admin Console does not import X509 Certificate from metadata keycloak admin/ui
#17933 Error! Failed to send email, and Error 400 API keycloak admin/ui
#19057 Experimental configuration options included in the documentation keycloak docs
#19083 [Keycloak 21.0.1] Identity provider JWKS public key is not editable via UI keycloak admin/ui
#19094 Unable to use SAML entity descriptor with transient NameIDFormat keycloak admin/ui
#19122 Read Only Attributes - Outdated configuration guide keycloak docs
#19126 Authentication flows first paragraph seems incomplete keycloak docs
#19128 UserFederationMapperFactory does not seem to exist anymore keycloak docs
#19134 client credentials tab not visible with "view-clients" role keycloak docs
#19145 Cannot produce an access token for the admin console keycloak docs
#19162 Entity collections in Hibernate 6 can't be replaced keycloak storage
#19254 Admin-UI does not show all custom attributes of Authorization Resource keycloak admin/ui
#19261 Flaky test: PhotozExampleLazyLoadPathsAdapterTest keycloak authorization-services
#19273 Adapters tests are failing for EAP and wildfly keycloak testsuite
#19321 Hibernate 6: UnsupportedOperationException: compare() not implemented for EntityType keycloak storage
#19324 Profile is created twice when resolving ignored artifacts keycloak core
#19335 Custom implemention of OIDC Login Protocol doesn't get executed keycloak oidc
#19346 Sending 'application/jwt' Accept header to GET userinfo endpoint returns a 406 error keycloak oidc
#19363 Incorrect documentation around password policies keycloak docs
#19396 memory leak when using ldap user federations keycloak ldap
#19397 Fix SSSDTest keycloak testsuite
#19404 Inconsistent use of Enum storage in legacy store keycloak storage
#19444 Client policies tab crashes in admin console. keycloak admin/ui
#19515 Remove access not working in new account v2 app keycloak account/ui
#19662 Invalid parameter redirect_uri when using an invalid client_id keycloak oidc








Keycloak 20.0.5.V2 (2023-05-02)
-------------------------------

Bug Fixes
^^^^^^^^^
diable redirect uri after forgot password





Keycloak 20.0.5.V2 (2023-04-13)
-------------------------------

Misc/Internal
^^^^^^^^^^^^^
guide utilisateur dans fenetre separee
email de creation de compte



Keycloak 20.0.5.V1 (2023-04-06)
-------------------------------

Bug Fixes
^^^^^^^^^
#14529 Escape character in SAML response keycloak saml
#16427 OfflineSessionPersistenceTest times out with session preloading keycloak storage
#17853 Default value of `NameID policy format` is not correct in SAML Identity Provider keycloak admin/ui
#17876 SAML client import cert pem file for "Encryption keys config" broken keycloak admin/ui
#17878 Keycloak 20.0.2-1 OutOfMemoryError in admin ui keycloak admin/ui
#17879 Not able to see the events UI keycloak admin/ui
#17913 Fine-grained authorization behavior for management of groups differs between version 18.0.3 and 19.03, 20.0 keycloak admin/ui





Keycloak 20.0.3.V5 (2023-03-23)
-------------------------------

Misc/Internal
^^^^^^^^^^^^^
Ajout Lien documenttation dans page login
Retrait hyperlink  après activation compte




Keycloak 20.0.3.V2 (2023-02-07)
-------------------------------

Misc/Internal
^^^^^^^^^^^^^
Ajout dans properties/css/js pour ajouter des scripts *.js pour collectivites login.
     _Ajout Show/Hide password chexbox:   Login
     _Modification Message password 12 car





Keycloak 20.0.3.V1 (2023-01-24)
-------------------------------

Bug Fixes
^^^^^^^^^
#3404 User role mapping tab: Show effective client roles for a user keycloak-ui section/users
#3604 ProviderConfigProperty.MAP_TYPE error in new UI keycloak-ui section/identity providers
#3714 Unable to turn on "Bypass identity confirmation" keycloak-ui section/authentication
#3727 Adding Form sub-flow broken on admin v2 keycloak-ui section/authentication
#3738 Custom User Provider SPI: There are no settings to configure the periodically synchronization of users keycloak-ui section/user federation
#3740 Assign roles to account - paging doesn't work keycloak-ui section/users
#3742 Realm selector requires two clicks to select something keycloak-ui section/realms
#3903 User management -> User in 2 subgroups with the same group name assignment does not work keycloak-ui section/users
#3911 Invalid language tag error when changing realm localization settings keycloak-ui section/realm settings
#3969 `Missing ":type" param` in the Events page when there are Client Scope events keycloak-ui section/events
#4048 Import client broken keycloak-ui section/clients
#4116 New Admin Console only, unable to add client profile in the first client policy keycloak-ui section/realm settings
#15287 Disabling hostname strict in prod doesn't disable https keycloak dist/quarkus
#15339 snakeyaml vulnerability GHSA-3mc7-4q67-w48m impacting CLI keycloak admin/cli
#15866 The redirect URI cannot be verified during logout in the case when client was removed keycloak oidc
#16252 Wrong auth session id being used when validating auth session id cookies keycloak core
#16276 Update XStream to 1.4.20 to fix CVE-2022-40151 & CVE-2022-41966 keycloak
#16287 Timeout when executing command PutMapCommand keycloak storage
#16380 Set OkHttp to 4.10.0 in parent pom keycloak
#16388 Lack of validation of access token on client registrations endpoint keycloak oidc






Keycloak 20.0.2.V1 (2023-01-10)
-------------------------------
Misc/Internal
^^^^^^^^^^^^^
Lien document Terms&Conditions



Keycloak 20.0.2.V1 (2023-01-06)
-------------------------------

Bug Fixes
^^^^^^^^^
#12942 KC Operator Test WatchedSecretsTest.testSecretsAreWatched is unstable keycloak operator
#15349 Error in Core? ( Clients Client details Dedicated scopes): Cannot read properties of undefined (reading 'helpText') keycloak admin/ui
#15392 NPE in theme.getName() via getMainPage keycloak core
#15528 Failed to run scheduled task ClearExpiredAdminEvents Oracle keycloak storage
#15565 Flaky test: OpenshiftClientStorageTest.testCodeGrantFlowWithServiceAccountUsingOAuthRedirectReference keycloak testsuite
#15639 SAMLFilterServletAdapterTest fails on Oracle keycloak storage
#15643 Internal pipelines don't tests run for OracleDB or other database keycloak storage
#15734 clientID are base64 encoded in state uri param with non URI friendly base64 keycloak storage


Misc/Internal
^^^^^^^^^^^^^
Modification Texte page Choix IDP

#12476 Expiration for Admin Events keycloak





Keycloak 20.0.1.V3 (2022-16-12)
-------------------------------

Misc/Internal
^^^^^^^^^^^^

Themes Collectivites: Modification TEXTES





Keycloak 20.0.1.V3 (2022-11-24)
-------------------------------

Extensions:
^^^^^^^^^^^
SMS: 2022 Nov 24: 2sv_sms_20221124.1.jar
	Modification code: required alternatif
	Retrait lien Back2Login
	Modification textes

EMAIL: 2022 Nov 23: master
	Modification code: required alternatif
	Modification textes

Misc/Internal
^^^^^^^^^^^^^
Modifications Textes






Keycloak 20.0.1.V2 (2022-11-09)
-------------------------------

Extensions:
^^^^^^^^^^^
SMS: 2022 Nov 09: 2sv_sms_20221109.1.jar





Keycloak 20.0.1.V1 (2022-11-08)
----------------------------

Bug Fixes
^^^^^^^^^
Correction theme KEYCLOAKV2


Misc/Internal
^^^^^^^^^^^^

WildFly distribution removed
New Keycloak Operator upgrade
Supported OpenJDK versions 17
Hostname provider now supports configuring the complete base URL
Improvements to kc.bat when running Keycloak on Windows
Upgrade of embedded H2 database
Feature guard for hosting the Keycloak JavaScript adapter
OTP Application SPI
Custom Identity Providers can now set an icon for the provider
FIPS 140-2 experimental support
Search groups by attribute
View group membership in the account console
Deprecated methods from data providers and models were removed





Keycloak 19.0.3.V5 (2022-11-08)
-------------------------------

Misc/Internal
^^^^^^^^^^^^

Themes Collectivites: Modification TEXTES
                      email activation
                      email password
                      texte password instruction
                      texte MAJ compte
                      Readonly account collectivite




Keycloak 19.0.3.V3 (2022-10-25)
-------------------------------

Misc/Internal
^^^^^^^^^^^^
Extension OTP SMS : Test sur Attribut mobile_number vide




Keycloak 19.0.3.V2 (2022-10-17)
-------------------------------

Bug Fixes
^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Messages Text FR divers Collectivites
correction lien retour "mire de connexion"


Keycloak 19.0.3.V1 (2022-10-13)
--------------------------

Bug Fixes
^^^^^^^^^
#3374 AdminV2 not using admin hostname keycloak-ui admin/ui
#13396 Clear expired events errors keycloak storage
#13803 CryptoIntegration fails to init on WebSphere Liberty keycloak
#13954 keycloak 19 legacy: unable to deploy war-packaged user storage provider keycloak storage
#14445 Missing variable declaration in AuthZ JS keycloak adapter/javascript
#14466 Browser-based flows not working properly due to 3rd party cookie policies on Firefox keycloak core

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Extension OTP MAIL : Message text FR


Keycloak 19.0.2.V5 (2022-10-06)
--------------------------

Bug Fixes
^^^^^^^^^

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Rename some Theme fields

Keycloak 19.0.2.V3 (2022-09-20)
--------------------------

Bug Fixes
^^^^^^^^^

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Rename Keycloak fields
Update Account fields

Keycloak 19.0.2.V2 (2022-09-14)
--------------------------

Bug Fixes
^^^^^^^^^

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
add KC feature declarative-user-profile,recovery-codes

Keycloak 19.0.2.V1 (2022-09-14)
--------------------------

Bug Fixes
^^^^^^^^^

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
upgrade to keycloak 19.0.2
Remove healtcheck logs

Keycloak 19.0.1.V4
--------------------------

Bug Fixes
^^^^^^^^^

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Remove button on Veolia account page
Ajout Sudo sur conteneur (Docekerfile)
Ajout certifcat SMTP (entrypoint.sh)
Modification Parametres lancement keycloak (entrypoint.sh)
Ajout quarkus.properties => logs


Keycloak 19.0.1.V3 (2022-08-18)
--------------------------

Bug Fixes
^^^^^^^^^
Fix veolia logo position on login themes

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Update favicon on custom theme & page title


Keycloak 19.0.1.V2 (2022-08-18)
--------------------------

Bug Fixes
^^^^^^^^^
Fix veolia logo position on login themes

Doc only changes
^^^^^^^^^^^^^^^^

Misc/Internal
^^^^^^^^^^^^
Add Account theme for vef & collectivite realm
