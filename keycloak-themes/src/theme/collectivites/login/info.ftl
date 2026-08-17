<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
	<center>
        ${messageHeader}
	</center>
        <#else>
	<center>
        ${message.summary}
	</center>
        </#if>
    <#elseif section = "form">
    <div id="kc-info-message" style="line-height:normal" align="justify">
		<#if requiredActions??>
			<p class="instruction">${kcSanitize(msg("blablastart"))?no_esc}
			<#list requiredActions><b><#items as reqActionItem> <#sep></#items></b></#list>
			</p>
		<#else>
			${kcSanitize(msg("blabla"))?no_esc}
		</#if>
	<#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <p><a href="${pageRedirectUri}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            <#elseif actionUri?has_content>
                <p><a href="${actionUri}">${kcSanitize(msg("proceedWithAction"))?no_esc}</a></p>
            <#elseif (client.baseUrl)?has_content>
                <p><a href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a></p>
            </#if>
	</#if>
    </div>
    </#if>
</@layout.registrationLayout>

