<#import "template.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("changePasswordHtmlTitle")}</h2>
        </div>
        <div class="col-md-2 subtitle">
            <span class="subtitle">${msg("allFieldsRequired")}</span>
        </div>
    </div>

    <script type="text/javascript" src="${url.resourcesPath}/js/password.js"></script>
    <form action="${url.passwordUrl}" class="form-horizontal" method="post">
        <input type="text" id="username" name="username" value="${(account.username!'')}" autocomplete="username" readonly="readonly" style="display:none;">

	<div class="form-group">
		<div class="col-sm-10 col-md-10">
			${msg("textepassword")}
		</div>
		<div class="col-sm-10 col-md-10">
			${msg("textepassword01")}
		</div>
	</div>


        <#if password.passwordSet>
            <div class="form-group">
                <div class="col-sm-2 col-md-2">
                    <label for="password" class="control-label">${msg("passwordAc")}</label>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="password" class="form-control" id="password" name="password" autofocus autocomplete="current-password">
                </div>
            </div>
        </#if>

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="password-new" class="control-label">${msg("passwordNew")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="password" class="form-control" id="password-new" name="password-new" autocomplete="new-password">
            </div>
        </div>

        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="password-confirm" class="control-label" class="two-lines">${msg("passwordConfirm")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="password" class="form-control" id="password-confirm" name="password-confirm" autocomplete="new-password">
		<div class="checkbox">
                   <label>
                      <input tabindex="2" type="checkbox" id="password-toggle" name="password-toggle" onclick="togglePasswords(['password', 'password-confirm', 'password-new'])"> ${msg("passShow")}
                   </label>
                </div>
            </div>
        </div>

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                </div>
            </div>
        </div>
    </form>

</@layout.mainLayout>

