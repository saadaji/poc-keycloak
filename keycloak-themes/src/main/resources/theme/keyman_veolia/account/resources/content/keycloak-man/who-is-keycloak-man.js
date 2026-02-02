/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";

// No JSX - no compilation needed
class KeycloakManHistory extends React.Component {
    // static contextType = AccountServiceContext;

    constructor(props) {
        super(props);
        this.state = {firstName: 'you', lastName: ''};
    }

    render() {
        const e = React.createElement;
        return e('div', {class: 'pf-c-card'}, [
            e('div', {class: 'pf-c-card__header'}, [
                e('div', {class: 'pf-c-card__header-main'}, [
                    e('center', null, e("img", {class: 'pf-c-brand', src: 'public/Hubgrade-by-Veolia-BLUE.png', alt: 'Hubgrade Logo'})),
                ])
            ]),

            e('div', {class: 'pf-c-card__body'}, [
                e('p', null, `Hubgrade by veolia   VEAOLIA EAU FRANCE   Version 26.0.7_V1.3 <beta>
                              `),
            ])

        ]);
    }
};

export default KeycloakManHistory;
