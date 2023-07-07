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

package org.eclipse.tractusx.selfdescriptionfactory.service.converter.vrel3;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.tractusx.selfdescriptionfactory.Utils;
import org.eclipse.tractusx.selfdescriptionfactory.dto.ServiceOfferingSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.v2210.DataAccountExportSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.v2210.TermsAndConditionsSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.misc.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Profile("catena-x-ctx")
public class ServiceOfferingSDConverter implements Converter<ServiceOfferingSchema, ServiceOfferingSD> {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${app.maxRedirect:5}")
    private int maxRedirect;

    @Override
    public ServiceOfferingSD convert(@NonNull ServiceOfferingSchema serviceOfferingSchema) {
        var aggregationOf = Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getAggregationOf(), Utils::uriFromStr).orElse(null);
        var termsAndConditions = Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getTermsAndConditions(), this::getTermsAndConditions).orElse(null);
        var policy = Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getPolicies(), Function.identity()).orElse(null);
        return validator.validated((Function<org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema, org.eclipse.tractusx.selfdescriptionfactory.model.v2210.ServiceOfferingSchema>)
                source -> new org.eclipse.tractusx.selfdescriptionfactory.model.v2210.ServiceOfferingSchema()
                        .type(source.getType())
                        .holder(source.getHolder())
                        .issuer(source.getIssuer())
                        .providedBy(source.getProvidedBy())
                        .aggregationOf(aggregationOf)
                        .termsAndConditions(termsAndConditions)
                        .policy(policy)
                        .dataAccountExport(List.of(
                                        new DataAccountExportSchema()
                                                .requestType(DataAccountExportSchema.RequestTypeEnum.EMAIL)
                                                .accessType(DataAccountExportSchema.AccessTypeEnum.DIGITAL)
                                                .formatType("json")
                                )
                        )
        ).andThen(m2210 -> objectMapper.convertValue(m2210, ServiceOfferingSD.class))
                .apply(serviceOfferingSchema);
    }

    private TermsAndConditionsSchema getTermsAndConditions(String urlStr) {
        return Try.of(() -> new URL(urlStr))
                .mapTry(url -> Utils.getConnectionIfRedirected(url, maxRedirect))
                .flatMap(urlConnection -> Try.withResources(urlConnection::getInputStream).of(DigestUtils::sha256Hex))
                .map(sha-> new TermsAndConditionsSchema()
                        .URL(URI.create(urlStr))
                        .hash(sha))
                .recoverWith(Utils.mapFailure(err ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Could not retrieve TermsAndConditions from '"+ urlStr +"'",
                                        err)
                        )
                ).get();
    }
}
