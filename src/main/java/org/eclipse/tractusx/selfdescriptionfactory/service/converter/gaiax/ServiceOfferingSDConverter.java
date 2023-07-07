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
import org.eclipse.tractusx.selfdescriptionfactory.Utils;
import org.eclipse.tractusx.selfdescriptionfactory.dto.ServiceOfferingSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.ServiceOfferingSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.converter.TermsAndConditionsHelper;
import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianWallet;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Profile("gaia-x-ctx")
public class ServiceOfferingSDConverter implements Converter<ServiceOfferingSchema, ServiceOfferingSD> {

    private final CustodianWallet custodianWallet;
    private final TermsAndConditionsHelper termsAndConditionsHelper;

    @Override
    public ServiceOfferingSD convert(@NonNull ServiceOfferingSchema serviceOfferingSchema) {
        var serviceOfferingSD = new ServiceOfferingSD();
        serviceOfferingSD.put("id", custodianWallet.getWalletData(serviceOfferingSchema.getHolder()).get("did"));
        serviceOfferingSD.put("type", serviceOfferingSchema.getType());
        serviceOfferingSD.put("ctxsd:connector-url", "https://connector-placeholder.net");
        serviceOfferingSD.put("gx-service:providedBy", serviceOfferingSchema.getProvidedBy());
        Map<String, Object> dataAccountExportNode = new LinkedHashMap<>();
        dataAccountExportNode.put("gx-service:requestType", "email");
        dataAccountExportNode.put("gx-service:accessType", "digital");
        dataAccountExportNode.put("gx-service:formatType", "json");
        serviceOfferingSD.put("gx-service:dataAccountExport", List.of(dataAccountExportNode));
        var setter = new Object() {
            <T> Consumer<T> set(String fieldName) {
                return t -> serviceOfferingSD.put(fieldName, t);
            }
        };
        Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getAggregationOf(), Utils::uriFromStr).ifPresent(setter.set("gx-service:aggregationOf"));
        Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getTermsAndConditions(), url -> termsAndConditionsHelper.getTermsAndConditions(url, "gx-service:")).ifPresent(setter.set("gx-service:termsAndConditions"));
        Utils.getNonEmptyListFromCommaSeparated(serviceOfferingSchema.getPolicies(), Function.identity()).ifPresent(setter.set("gx-service:policy"));
        return serviceOfferingSD;
    }
}
