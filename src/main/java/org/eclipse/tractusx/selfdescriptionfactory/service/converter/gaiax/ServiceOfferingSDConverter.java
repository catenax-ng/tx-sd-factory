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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.eclipse.tractusx.selfdescriptionfactory.SelfDescription;
import org.eclipse.tractusx.selfdescriptionfactory.Utils;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.converter.TermsAndConditionsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "app.verifiable-credentials")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Validated
public class ServiceOfferingSDConverter implements Converter<ServiceOfferingSchema, SelfDescription> {

    @Setter private Map<String, String> gaiaXDataAccountExport;
    @Setter private List<String> gaiaXDataProtectionRegime;
    @Setter @NotNull(message = "app.verifiableCredentials.gaia-x-participant-schema shall be defined in the configuration file") private URI gaiaXServiceSchema;
    @Setter @NotNull(message = "app.verifiableCredentials.gaia-x-policy shall be defined in the configuration file") private URI gaiaXPolicy;
    @Setter @Positive(message = "app.verifiableCredentials.durationDays shall be defined in the configuration file") private int durationDays;
    @Setter @NotNull(message = "app.verifiableCredentials.catena-x-ns shall be defined in the configuration file") private String catenaXNs;
    private final TermsAndConditionsHelper termsAndConditionsHelper;

    /**
     * Convert ServiceOfferingSchema to SelfDescription
     * @param serviceOfferingSchema the service offering schema
     * @return the self description
     */
    @Override
    public SelfDescription convert(ServiceOfferingSchema serviceOfferingSchema) {
        // Create a self description with the external id from the service offering schema
        var selfDescription = new SelfDescription(serviceOfferingSchema.getExternalId());

        // Create a map for the service offering self description
        var serviceOfferingSD = new LinkedHashMap<String, Object>();
        serviceOfferingSD.put("@context", Map.of("ctxsd", catenaXNs));
        serviceOfferingSD.put("id", "http://catena-x.net/bpn/".concat(serviceOfferingSchema.getHolder()));
        serviceOfferingSD.put("type", "gx:ServiceOffering");
        serviceOfferingSD.put("ctxsd:connector-url", "https://connector-placeholder.net");

        // Create a setter function to add values to the service offering self description
        var setter = new Object() {
            <T> Consumer<T> set(String fieldName) {
                return t -> serviceOfferingSD.put(fieldName, t);
            }
        };

        // Add aggregationOf field if non-empty
        Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getAggregationOf(), Utils::uriFromStr)
                .ifPresent(setter.set("gx:aggregationOf"));

        // Add termsAndConditions field if non-empty
        Utils.getNonEmptyListFromCommaSeparated(
                serviceOfferingSchema.getTermsAndConditions(),
                url -> termsAndConditionsHelper.getTermsAndConditions(
                        url,
                        u -> Map.of("gx:URL", u),
                        h -> Map.of("gx:hash", h)
                )
        ).ifPresent(setter.set("gx:termsAndConditions"));

        // Add policies field if non-empty, using policyUri
        serviceOfferingSD.put(
                "gx:policy",
                Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getPolicies(), Function.identity())
                    .map(l -> Stream.concat(Stream.of(gaiaXPolicy), l.stream()).toList())
                    .map(Object.class::cast)
                    .orElse(gaiaXPolicy)
        );

        // Add dataProtectionRegime and dataAccountExport fields
        serviceOfferingSD.put("gx:dataProtectionRegime", gaiaXDataProtectionRegime);
        serviceOfferingSD.put("gx:dataAccountExport", gaiaXDataAccountExport);

        // Create a verifiable credential using the service offering self description
        var vc = VerifiableCredential.builder()
                .context(gaiaXServiceSchema)
                .id(URI.create("http://catena-x.net/service-offering/".concat(UUID.randomUUID().toString())))
                .issuanceDate(new Date())
                .expirationDate(Date.from(Instant.now().plus(Duration.ofDays(durationDays))))
                .credentialSubject(CredentialSubject.fromMap(serviceOfferingSD))
                .build();

        // Add the verifiable credential to the self description and return it
        selfDescription.getVerifiableCredentialList().add(vc);
        return selfDescription;
    }
}
