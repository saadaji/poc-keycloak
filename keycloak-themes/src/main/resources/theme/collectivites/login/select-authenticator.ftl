<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header" || section = "show-username">
        <#if section = "header">
            ${msg("loginChooseAuthenticator")}
        </#if>
    <#elseif section = "form">

        <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
	<BR>
            <div>
                <#list auth.authenticationSelections as authenticationSelection>
                    <button class="button" type="submit" name="authenticationExecution" value="${authenticationSelection.authExecId}">

                        <div class="${properties.kcSelectAuthListItemIconClass!}">
                            <i class="${properties['${authenticationSelection.iconCssClass}']!authenticationSelection.iconCssClass} ${properties.kcSelectAuthListItemIconPropertyClass!}"></i>
                        </div>

                        <div class="${properties.kcSelectAuthListItemBodyClass!}">
                            <div class="${properties.kcSelectAuthListItemHeadingClass!}">
                                ${msg('${authenticationSelection.displayName}')}
                            </div>
                            <div class="${properties.kcSelectAuthListItemDescriptionClass!}">
                                ${msg('${authenticationSelection.helpText}')}
                            </div>
                        </div>

                        <div class="${properties.kcSelectAuthListItemFillClass!}"></div>

                        <div class="${properties.kcSelectAuthListItemArrowClass!}">
                            <i class="${properties.kcSelectAuthListItemArrowIconClass!}"></i>
                        </div>

                    </button><BR>
                </#list>
            </div>
	<BR>
        </form>

    </#if>
</@layout.registrationLayout>

