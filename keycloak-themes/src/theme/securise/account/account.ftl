<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section>
    <div class="row">
        <div class="col-md-10">
            <h2>${msg("editAccountHtmlTitle")}</h2>
        </div>
        <div class="col-md-2 subtitle">
            <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
        </div>
    </div>

    <form action="${url.accountUrl}" class="form-horizontal" method="post">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <#if !realm.registrationEmailAsUsername>
            <div class="form-group ${messagesPerField.printIfExists('username','has-error')}">
                <div class="col-sm-2 col-md-2">
                    <label for="username" class="control-label">${msg("username")}</label> <#if realm.editUsernameAllowed><span class="required">*</span></#if>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="username" name="username" readonly="readonly" value="${(account.username!'')}"/>
                </div>
            </div>
        </#if>

        <div class="form-group ${messagesPerField.printIfExists('email','has-error')}">
            <div class="col-sm-2 col-md-2">
            <label for="email" class="control-label">${msg("email")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="email" name="email" readonly="readonly" autofocus value="${(account.email!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('firstName','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="firstName" class="control-label">${msg("firstName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="firstName" name="firstName" readonly="readonly" value="${(account.firstName!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('lastName','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="lastName" class="control-label">${msg("lastName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="lastName" name="lastName" readonly="readonly" value="${(account.lastName!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('phone','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="phone" class="control-label">Téléphone</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="phone" name="phone" value="${(account.attributes.phone!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('mobile_number','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="mobile_number" class="control-label">Activer la double authentification par SMS</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <label class="switch">
                  <input type="checkbox" id="mobile_number_checkbox" />
                  <span class="slider round"></span>
                  <input type="hidden" id="mobile_number" name="mobile_number"/>
                </label>
            </div>
        </div>
        <script>

            var mobile_number_checkbox = document.getElementById("mobile_number_checkbox");
            var mobile_number = document.getElementById("mobile_number");
            if ("${account.attributes.mobile_number!''}".length !== 0 ){
                mobile_number_checkbox.checked = true;
                mobile_number.value = "${account.attributes.mobile_number!''}";
            }
            else{
                mobile_number_checkbox.checked = false;
                mobile_number.value = "${account.attributes.mobile_number!''}";
            }


            mobile_number_checkbox.onclick = function() {
                var mobile_number = document.getElementById("mobile_number");

                if (this.checked === true){
                    mobile_number.value =document.getElementById('phone').value;
                }
                else{
                    mobile_number.value = ""
                }
            }
        </script>


        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <#if url.referrerURI??><a href="${url.referrerURI}">${kcSanitize(msg("backToApplication")?no_esc)}</a></#if>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Cancel">${msg("doCancel")}</button>
                </div>
            </div>
        </div>
    </form>

</@layout.mainLayout>
