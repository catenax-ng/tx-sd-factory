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
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.dto.LegalPersonSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.v2210.AddressSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.misc.Validator;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Profile("catena-x-ctx")
public class LegalPersonSDConverter implements Converter<LegalPersonSchema, LegalPersonSD> {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public LegalPersonSD convert(LegalPersonSchema lp) {
        return validator.validated(
                (Function<org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema, org.eclipse.tractusx.selfdescriptionfactory.model.v2210.LegalPersonSchema>)
                        source -> new org.eclipse.tractusx.selfdescriptionfactory.model.v2210.LegalPersonSchema()
                            .type(source.getType())
                            .holder(source.getHolder())
                            .issuer(source.getIssuer())
                            .bpn(source.getBpn())
                            .registrationNumber(source.getRegistrationNumber().stream()
                                    .map(rNum -> objectMapper.convertValue(rNum, org.eclipse.tractusx.selfdescriptionfactory.model.v2210.RegistrationNumberSchema.class))
                                    .collect(Collectors.toSet()))
                            .headquarterAddress(new AddressSchema().countryCode(source.getHeadquarterAddressCountry()))
                            .legalAddress(new AddressSchema().countryCode(source.getLegalAddressCountry()))
        ).andThen(m2210 -> objectMapper.convertValue(m2210, LegalPersonSD.class))
                .apply(lp);
    }
}
