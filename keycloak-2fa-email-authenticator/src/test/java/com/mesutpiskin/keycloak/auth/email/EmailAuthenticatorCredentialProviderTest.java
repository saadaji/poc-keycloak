package com.mesutpiskin.keycloak.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailAuthenticatorCredentialProviderTest {

    private EmailAuthenticatorCredentialProvider provider;
    private KeycloakSession session;
    private RealmModel realm;
    private UserModel user;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        provider = new EmailAuthenticatorCredentialProvider(session);
    }

    @Test
    void testIsConfiguredFor_WithValidEmail() {
        when(user.getEmail()).thenReturn("test@example.com");
        
        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);
        
        assertTrue(result, "Should be configured when user has a valid email");
    }

    @Test
    void testIsConfiguredFor_WithNullEmail() {
        when(user.getEmail()).thenReturn(null);
        
        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);
        
        assertFalse(result, "Should not be configured when user has null email");
    }

    @Test
    void testIsConfiguredFor_WithEmptyEmail() {
        when(user.getEmail()).thenReturn("");
        
        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);
        
        assertFalse(result, "Should not be configured when user has empty email");
    }
    
    @Test
    void testIsConfiguredFor_WithBlankEmail() {
        when(user.getEmail()).thenReturn("  ");
        
        boolean result = provider.isConfiguredFor(realm, user, EmailAuthenticatorCredentialModel.TYPE_ID);
        
        assertFalse(result, "Should not be configured when user has blank email");
    }

    @Test
    void testIsConfiguredFor_WrongCredentialType() {
        boolean result = provider.isConfiguredFor(realm, user, "wrong-type");
        
        assertFalse(result, "Should return false for unsupported credential type");
    }
}
