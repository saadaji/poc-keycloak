<#import "template.ftl" as layout>
<#import "passkeys.ftl" as passkeys>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        <center>
        ${msg("loginAccountTitleVeolia")}
        </center>
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <#if !usernameHidden??>
                            <div class="${properties.kcFormGroupClass!}">
                                <label for="username"
                                       class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOr
Email")}<#else>${msg("email")}</#if></label>

                                <input tabindex="1" id="username"
                                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" autofocus
                                       autocomplete="${(enableWebAuthnConditionalUI?has_content)?then('username webauthn', 'username')}"
                                       dir="ltr"/>

                                <#if messagesPerField.existsError('username')>
                                    <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                    </span>
                                </#if>
                            </div>
                        </#if>

                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input tabindex="4"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
	<div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <br><hr>
                <h2>${msg("identity-provider-login-label")}</h2>
                <#list social.providers as p>
                        <a id="social-${p.alias}" class="button_idp" href="${p.loginUrl}">
                                    <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                        </a><BR><BR>
                </#list>
        </div>
        <@passkeys.conditionalUIData />
	<div id="text">
                ${kcSanitize(msg("idpMessage_veolia"))?no_esc}
        </div>
    </#if>

</@layout.registrationLayout>

