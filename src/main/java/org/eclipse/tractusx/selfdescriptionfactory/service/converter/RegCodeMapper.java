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

package org.eclipse.tractusx.selfdescriptionfactory.service.converter;

import org.eclipse.tractusx.selfdescriptionfactory.model.tagus.RegistrationNumberSchema.TypeEnum;

import java.util.Map;
import java.util.function.Function;

public class RegCodeMapper {
    private final String prefix;
    private RegCodeMapper(String prefix){
        this.prefix = prefix;
    }
    private static final Map<TypeEnum, String> regCodeMapper = Map.of(
            TypeEnum.TAXID, "local",
            TypeEnum.VATID, "vatID",
            TypeEnum.EUID, "EUID",
            TypeEnum.EORI, "EORI",
            TypeEnum.LEICODE, "leiCode"
    );

    public String get(TypeEnum type) {
        return prefix.concat(regCodeMapper.get(type));
    }

    public static Function<TypeEnum, String> getRegCodeMapper(String prefix) {
        return new RegCodeMapper(prefix)::get;
    }
}
