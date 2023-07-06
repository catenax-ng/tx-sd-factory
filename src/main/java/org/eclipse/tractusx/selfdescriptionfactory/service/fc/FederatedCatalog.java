/********************************************************************************
 * Copyright (c) 2021,2022 T-Systems International GmbH
 * Copyright (c) 2021,2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.selfdescriptionfactory.service.fc;

import com.danubetech.verifiablecredentials.VerifiablePresentation;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.service.keycloak.KeycloakManager;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor
public abstract class FederatedCatalog {
    @Autowired
    protected KeycloakManager keycloakManager;

    public abstract void uploadLegalPerson(VerifiablePresentation payload, String token);

    public abstract void uploadServiceOffering(VerifiablePresentation payload, String token);



    public void uploadServiceOffering(VerifiablePresentation verifiableCredential) {
        uploadServiceOffering(verifiableCredential, "Bearer ".concat(keycloakManager.getToken("federatedCatalog")));
    }

    public void uploadLegalPerson(VerifiablePresentation verifiableCredential) {
        uploadLegalPerson(verifiableCredential, "Bearer ".concat(keycloakManager.getToken("federatedCatalog")));
    }
}
