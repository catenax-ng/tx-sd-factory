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

package org.eclipse.tractusx.selfdescriptionfactory.factory;

import com.danubetech.verifiablecredentials.CredentialSubject;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.dto.LegalPersonSD;
import org.eclipse.tractusx.selfdescriptionfactory.dto.ServiceOfferingSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class SDFactoryCommon {

    public abstract ConversionService getConversionService();

    static record CredentialSubjectDto (
            CredentialSubject credentialSubject,
            String holder,
            String issuer,
            String externalId,
            String type
    ) {};
    protected CredentialSubjectDto makeSubject(Object document) {
        Map<String, Object> claims;
        String holder;
        String externalId;
        String issuer;
        String type;
        if (document instanceof LegalPersonSchema legalPersonSchema) {
            claims = Optional.ofNullable(getConversionService().convert(document, LegalPersonSD.class)).orElseThrow();
            holder = legalPersonSchema.getHolder();
            issuer = legalPersonSchema.getIssuer();
            externalId = legalPersonSchema.getExternalId();
            type = "LegalPerson";
        } else if (document instanceof ServiceOfferingSchema serviceOfferingSchema) {
            claims = Optional.ofNullable(getConversionService().convert(document, ServiceOfferingSD.class)).orElseThrow();
            holder = serviceOfferingSchema.getHolder();
            issuer = serviceOfferingSchema.getIssuer();
            externalId = serviceOfferingSchema.getExternalId();;
            type = "ServiceOffering";
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Not supported SD-Document type"
            );
        }
        claims.remove("holder");
        claims.remove("issuer");
        claims.remove("externalId");
        var credentialSubject = CredentialSubject.fromJsonObject(claims);
        return new CredentialSubjectDto(credentialSubject, holder, issuer, externalId, type);
    }
}
