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

package org.eclipse.tractusx.selfdescriptionfactory.service.converter.gaiax;

import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.dto.LegalPersonSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.RegistrationNumberSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.converter.RegCodeMapper;
import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianWallet;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Profile("gaia-x-ctx")
public class LegalPersonSDConverter implements Converter<LegalPersonSchema, LegalPersonSD> {

    private final CustodianWallet custodianWallet;
    private Map<RegistrationNumberSchema.TypeEnum, String> regCodeMapper = RegCodeMapper.getRegCodeMapper("gx:");


    @Override
    public LegalPersonSD convert(LegalPersonSchema legalPersonSchema) {
        LegalPersonSD legalPersonSD = new LegalPersonSD();
        legalPersonSD.put("id", custodianWallet.getWalletData(legalPersonSchema.getBpn()).get("did"));
        legalPersonSD.put("type", legalPersonSchema.getType());
        legalPersonSD.put("ctxsd:bpn", legalPersonSchema.getBpn());
        legalPersonSD.put("gx:name", custodianWallet.getWalletData(legalPersonSchema.getBpn()).get("name"));
        legalPersonSD.put(
                "gx:legalRegistrationNumber",
                legalPersonSchema.getRegistrationNumber().stream()
                        .map(
                                regNum -> Map.of(regCodeMapper.get(regNum.getType()), regNum.getValue())
                        ).toList()
        );
        legalPersonSD.put("gx:headquarterAddress", Map.of("gx:addressCountryCode", legalPersonSchema.getHeadquarterAddressCountry()));
        legalPersonSD.put("gx:legalAddress", Map.of("gx:addressCountryCode", legalPersonSchema.getLegalAddressCountry()));
        return legalPersonSD;
    }
}
