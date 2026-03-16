<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
    <#elseif section = "form">
    <div id="kc-terms-text">
        	${kcSanitize(msg("termsText"))?no_esc}

		<#if client.baseUrl = "https://keycloak-dev.dir.istefr.fr/realms/collectivites/account/">
			<p><a href="https://keycloak-dev-stockage.dir.istefr.fr/Hubgrade Pass - Politique de protection des données.pdf" target="_blank"><u><B>les mentions légales et la politique de protection des données personnelles de Hubgrade Pass.</B></u></a></p>
		<#elseif client.baseUrl = "https://keycloak-hom.dir.istefr.fr/realms/collectivites/account/">
			<p><a href="https://keycloak-hom-stockage.dir.istefr.fr/Hubgrade Pass - Politique de protection des données.pdf" target="_blank"><u><B>les mentions légales et la politique de protection des données personnelles de Hubgrade Pass.</B></u></a></p>
		<#elseif client.baseUrl = "https://keycloak.istefr.fr/realms/collectivites/account/">
			<p><a href="https://keycloak-stockage.istefr.fr/Hubgrade Pass - Politique de protection des données.pdf" target="_blank"><u><B>les mentions légales et la politique de protection des données personnelles de Hubgrade Pass.</B></u></a></p>
		</#if>
    </div>
    <form class="form-actions" action="${url.loginAction}" method="POST">
        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}"/>
    </form>
    <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
