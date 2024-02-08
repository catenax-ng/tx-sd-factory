/********************************************************************************
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.selfdescriptionfactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.SelfdescriptionPostRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SelfDescriptionFactoryApplicationTests {

    @Autowired
    ConversionService conversionService;
    ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }


    static final String legalPersonStr =
            """
                    {
                      "externalId": "ID01234-123-4321",
                      "type": "LegalParticipant",
                      "holder": "BPNL000000000000",
                      "issuer": "CAXSDUMMYCATENAZZ",
                      "registrationNumber": [
                        {
                          "type": "taxID",
                          "value": "o12345678"
                        }
                      ],
                      "headquarterAddress.country": "DE",
                      "legalAddress.country": "DE",
                      "bpn": "BPNL000000000000"
                    }
                    """;
    static final String serviceOfferingStr =
            """
                    {
                      "externalId": "ID01234-123-4321",
                      "type": "ServiceOffering",
                      "holder": "BPNL000000000000",
                      "issuer": "CAXSDUMMYCATENAZZ",
                      "providedBy": "https://participant.url",
                      "aggregationOf": "https://aggr1.url, https://aggr2.url",
                      "termsAndConditions": "https://raw.githubusercontent.com/eclipse-tractusx/sd-factory/main/LICENSE",
                      "policies": "policy1, policy2"
                    }
                    """;

    static Stream<Arguments> inputProvider() {
        return Stream.of(
                Arguments.of(legalPersonStr, "Legal Participant"),
                Arguments.of(serviceOfferingStr, "Service Offering")
        );
    }

    @ParameterizedTest(name = "{index} - {1}")
    @MethodSource("inputProvider")
    public void testLegalPersonConverter(String selfdescriptionRequestStr, String type) throws JsonProcessingException {
        var selfdescriptionPostRequest = objectMapper.readValue(selfdescriptionRequestStr, SelfdescriptionPostRequest.class);
        var converted = conversionService.convert(selfdescriptionPostRequest, SelfDescription.class);
        assertNotNull(converted, "converted object should not be null");
        assertFalse(converted.getVerifiableCredentialList().isEmpty());
        System.out.println(objectMapper.writeValueAsString(converted.getVerifiableCredentialList()));
    }
}
