<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
	<center>
	<BR>
        ${msg("loginAccountTitle")}
	</center>
    <#elseif section = "socialProviders" >
        <#if realm.password && social.providers??>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
		<#list social.providers as p>
                        <a id="social-${p.alias}" class="button_idp" href="${p.loginUrl}">
                                    <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                        </a><BR><BR>
                </#list>
            </div>
        </#if>
        <div id="text">
		${kcSanitize(msg("idpMessage"))?no_esc}
        </div>
    </#if>
</@layout.registrationLayout>
