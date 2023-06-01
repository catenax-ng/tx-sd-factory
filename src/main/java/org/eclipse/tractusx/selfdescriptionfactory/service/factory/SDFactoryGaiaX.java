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

package org.eclipse.tractusx.selfdescriptionfactory.service.factory;

import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import foundation.identity.jsonld.JsonLDException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.selfdescriptionfactory.dto.LegalPersonSD;
import org.eclipse.tractusx.selfdescriptionfactory.dto.ServiceOfferingSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.SDFactory;
import org.eclipse.tractusx.selfdescriptionfactory.service.signer.LDSigner;
import org.eclipse.tractusx.selfdescriptionfactory.service.verifier.PredicateGenerator;
import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianWallet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * A service to create and manipulate of Self-Description document
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SDFactoryGaiaX implements SDFactory {
    @Value("${app.verifiableCredentials.durationDays:90}")
    private int duration;
    @Value("${app.signer.issuer}")
    private URI issuer;
    private final ConversionService conversionService;
    private final LDSigner ldSigner;
    private final PredicateGenerator predicateGenerator;
    private final CustodianWallet custodianWallet;

    @Override
    @PreAuthorize("hasAuthority(@securityRoles.createRole)")
    public void createVC(Object document) throws JsonLDException, GeneralSecurityException, IOException {
        Map<String, Object> claimsHolder;
        String holder;
        if (document instanceof LegalPersonSchema legalPersonSchema) {
            claimsHolder = Optional.ofNullable(conversionService.convert(document, LegalPersonSD.class)).orElseThrow();
            holder = legalPersonSchema.getHolder();
        } else if (document instanceof ServiceOfferingSchema serviceOfferingSchema) {
            claimsHolder = Optional.ofNullable(conversionService.convert(document, ServiceOfferingSD.class)).orElseThrow();
            holder = serviceOfferingSchema.getHolder();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Not supported SD-Document type"
            );
        }
        var credentialSubject = CredentialSubject.fromJsonObject(claimsHolder);
        var verifiableCredential = VerifiableCredential.builder()
                .issuer(issuer)
                .issuanceDate(new Date())
                .expirationDate(Date.from(Instant.now().plus(Duration.ofDays(duration))))
                .credentialSubject(credentialSubject)
                .build();
        var signedVerifiableCredential = ldSigner.sign(verifiableCredential);
        var verifiablePresentation = VerifiablePresentation.builder()
                .verifiableCredential(signedVerifiableCredential)
                .holder(URI.create(custodianWallet.getWalletData(holder).get("did").toString()))
                .build();
        var signedVerifiablePresentation = ldSigner.sign(verifiablePresentation);
        System.out.println(signedVerifiablePresentation.toJson(true));
        var verifier = predicateGenerator.getPredicate(signedVerifiablePresentation.getLdProof().getVerificationMethod());
        System.out.println(verifier.test(signedVerifiablePresentation));
        System.out.println(verifier.test(signedVerifiableCredential));
    }
}
