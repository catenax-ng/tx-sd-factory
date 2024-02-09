/********************************************************************************
 * Copyright (c) 2022,2023 T-Systems International GmbH
 * Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.selfdescriptionfactory.service.converter.gaiax;

import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.SelfDescription;
import org.eclipse.tractusx.selfdescriptionfactory.model.tagus.LegalParticipantSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.tagus.RegistrationNumberSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.converter.RegCodeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.eclipse.tractusx.selfdescriptionfactory.Utils.mapOf;

@Component
@RequiredArgsConstructor
public class LegalParticipantSDConverter implements Converter<LegalParticipantSchema, SelfDescription> {
    private final Function<RegistrationNumberSchema.TypeEnum, String> regCodeMapper = RegCodeMapper.getRegCodeMapper("gx:");

    @Value("${app.verifiableCredentials.gaia-x-participant-schema}")
    private URI contextUri;
    @Value("${app.verifiableCredentials.catena-x-ns}")
    private String ctxsd;

    @Value("${app.verifiableCredentials.durationDays:90}")
    private int duration;

    @Override
    public SelfDescription convert(LegalParticipantSchema legalParticipantSchema) {
        var selfDescription = new SelfDescription(legalParticipantSchema.getExternalId());
        var regNumbers = legalParticipantSchema.getRegistrationNumber().stream()
                .map(registrationNumberSchema -> getLegalRegistrationNumberVc(legalParticipantSchema, registrationNumberSchema)).toList();
        var legalParticipant = getLegalPersonVc(legalParticipantSchema, regNumbers.stream().map(VerifiableCredential::getId).toList());
        selfDescription.getVerifiableCredentialList().add(legalParticipant);
        selfDescription.getVerifiableCredentialList().addAll(regNumbers);
        selfDescription.getVerifiableCredentialList().addAll(getGxTermsAndConditionsVc(legalParticipantSchema));
        return selfDescription;
    }

    /**
     * Generates a Verifiable Credential for a legal person based on the provided legal participant schema and registration numbers.
     *
     * @param legalParticipantSchema The legal participant schema containing the necessary information.
     * @param regNumbers The list of registration numbers for the legal person.
     * @return The generated Verifiable Credential for the legal person.
     */
    private VerifiableCredential getLegalPersonVc(LegalParticipantSchema legalParticipantSchema, List<URI> regNumbers) {
        // Create a map to hold the verifiable credential data
        var legalParticipantSD = new LinkedHashMap<String, Object>();

        // Set the context and id of the verifiable credential
        legalParticipantSD.put("@context", Map.of("ctxsd", ctxsd));
        legalParticipantSD.put("id", "http://catena-x.net/bpn/".concat(legalParticipantSchema.getHolder()));

        // Set the type and additional properties of the legal participant
        legalParticipantSD.put("type", "gx:LegalParticipant");
        legalParticipantSD.put("ctxsd:bpn", legalParticipantSchema.getHolder());
        legalParticipantSD.put("gx:legalName", legalParticipantSchema.getName());

        // Set the legal registration number based on the number of registration numbers provided
        legalParticipantSD.put("gx:legalRegistrationNumber",
                regNumbers.size() == 1
                        ? Map.of("id", regNumbers.get(0))
                        : regNumbers.stream().map(regNum -> Map.of("gx:legalRegistrationNumber", Map.of("id", regNum))).toList()
        );

        // Set the headquarter and legal addresses
        legalParticipantSD.put("gx:headquarterAddress", Map.of("gx:countrySubdivisionCode", legalParticipantSchema.getHeadquarterAddressCountry()));
        legalParticipantSD.put("gx:legalAddress", Map.of("gx:countrySubdivisionCode", legalParticipantSchema.getLegalAddressCountry()));

        // Build and return the Verifiable Credential
        return VerifiableCredential.builder()
                .context(contextUri)
                .id(URI.create("http://catena-x.net/legal-participant/".concat(UUID.randomUUID().toString())))
                .issuanceDate(new Date())
                .expirationDate(Date.from(Instant.now().plus(Duration.ofDays(duration))))
                .credentialSubject(CredentialSubject.fromMap(legalParticipantSD))
                .build();
    }

    /**
     * Generates a Verifiable Credential for a legal registration number.
     *
     * @param legalParticipantSchema The legal participant schema
     * @param registrationNumberSchema The registration number schema
     * @return The Verifiable Credential for the legal registration number
     */
    private VerifiableCredential getLegalRegistrationNumberVc(
            LegalParticipantSchema legalParticipantSchema,
            RegistrationNumberSchema registrationNumberSchema
    ) {
        // Construct the Verifiable Credential with builder pattern
        return VerifiableCredential.builder()
                .id(URI.create("http://catena-x.net/legal-registration-number/".concat(UUID.randomUUID().toString())))
                .issuanceDate(new Date())
                .expirationDate(Date.from(Instant.now().plus(Duration.ofDays(duration))))
                .credentialSubject(
                        CredentialSubject.builder()
                                .context(contextUri)
                                .type("gx:legalRegistrationNumber")
                                .id(URI.create("http://catena-x.net/bpn/".concat(legalParticipantSchema.getHolder())))
                                .properties(
                                        // Conditionally set properties based on registration number type
                                        registrationNumberSchema.getType().equals(RegistrationNumberSchema.TypeEnum.VATID)
                                                ? mapOf("gx:vatID", registrationNumberSchema.getValue(), "gx:vatID-countryCode", "DE")
                                                : Map.of(regCodeMapper.apply(registrationNumberSchema.getType()), registrationNumberSchema.getValue()))
                                .build()
                ).build();
    }

    private static final String GX_TNC =
            """
            The PARTICIPANT signing the Self-Description agrees as follows:
            - to update its descriptions about any changes, be it technical, organizational, or legal - especially but not limited to contractual in regards to the indicated attributes present in the descriptions.
            
            The keypair used to sign Verifiable Credentials will be revoked where Gaia-X Association becomes aware of any inaccurate statements in regards to the claims which result in a non-compliance with the Trust Framework and policy rules defined in the Policy Rules and Labelling Document (PRLD).
            """;

    /**
     * Checks if attachment contains appropriate Gx Terms and Conditions verifiable credential and generates one if it does not.
     *
     * @param  legalParticipantSchema   the legal participant schema
     * @return                          the list of VC from attachment including Gx Terms and Conditions verifiable credentials
     */
    private List<VerifiableCredential> getGxTermsAndConditionsVc(LegalParticipantSchema legalParticipantSchema) {
        var attachment = Optional.ofNullable(legalParticipantSchema.getAttachment()).stream().flatMap(List::stream).map(Map.class::cast).map(VerifiableCredential::fromMap).toList();
        if (attachment.stream().flatMap(vc -> Optional.ofNullable(vc.getCredentialSubject()).stream())
                .anyMatch(subj ->
                        "gx:GaiaXTermsAndConditions".equals(subj.getType())
                                && URI.create("http://catena-x.net/bpn/".concat(legalParticipantSchema.getHolder())).equals(subj.getId())
                )) {
            return attachment;
        } else {
            return Stream.concat(
                    attachment.stream(),
                    Stream.of(VerifiableCredential.builder()
                            .id(URI.create("http://catena-x.net/terms-and-conditions/".concat(UUID.randomUUID().toString())))
                            .issuanceDate(new Date())
                            .expirationDate(Date.from(Instant.now().plus(Duration.ofDays(duration))))
                            .credentialSubject(
                                    CredentialSubject.builder()
                                            .context(contextUri)
                                            .type("gx:GaiaXTermsAndConditions")
                                            .id(URI.create("http://catena-x.net/bpn/".concat(legalParticipantSchema.getHolder())))
                                            .properties(Map.of("gx:termsAndConditions", GX_TNC))
                                            .build()
                            ).build()
                    )
            ).toList();
        }
    }
}
