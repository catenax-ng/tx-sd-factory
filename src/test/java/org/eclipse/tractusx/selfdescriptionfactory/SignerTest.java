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

package org.eclipse.tractusx.selfdescriptionfactory;

import com.danubetech.verifiablecredentials.VerifiablePresentation;
import org.eclipse.tractusx.selfdescriptionfactory.service.fc.FederatedCatalogRemote;
import org.eclipse.tractusx.selfdescriptionfactory.service.verifier.PredicateGenerator;
import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SignerTest {

    @MockBean
    CustodianClient custodianClient;
    @MockBean
    FederatedCatalogRemote federatedCatalog;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    PredicateGenerator predicateGenerator;


    @Test
    @WithMockUser(username = "fulladmin", authorities={"add_self_descriptions"})
    public void testLegalPerson() throws Exception {
        given(custodianClient.getWalletData("BPNL000000000000"))
                .willReturn(
                        Map.of( "did", "did:test:BPNL000000000000",
                                "name", "Test Company"
                        )
                );
        var payload = """
        {
          "externalId": "ID01234-123-4321",
          "type": "LegalPerson",
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
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/api/rel3/selfdescription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                ).andExpect(status().isAccepted())
                 .andReturn();
        final ArgumentCaptor<VerifiablePresentation> captor = ArgumentCaptor.forClass(VerifiablePresentation.class);
        verify(federatedCatalog).uploadLegalPerson(captor.capture());
        var sdocument = captor.getValue();
        System.out.println(sdocument.toJson(true));
        var verifier = predicateGenerator.getPredicate(sdocument.getLdProof().getVerificationMethod());
        System.out.println(verifier.test(sdocument));
        System.out.println(verifier.test(sdocument.getVerifiableCredential()));
    }

    @Test
    @WithMockUser(username = "fulladmin", authorities={"add_self_descriptions"})
    public void testServiceOffering() throws Exception {
        given(custodianClient.getWalletData("BPNL000000000000"))
                .willReturn(
                        Map.of( "did", "did:test:BPNL000000000000",
                                "name", "Test Company"
                        )
                );
        var payload = """
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
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/api/rel3/selfdescription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                ).andExpect(status().isAccepted())
                .andReturn();
        final ArgumentCaptor<VerifiablePresentation> captor = ArgumentCaptor.forClass(VerifiablePresentation.class);
        verify(federatedCatalog).uploadServiceOffering(captor.capture());
        var sdocument = captor.getValue();
        System.out.println(sdocument.toJson(true));
        var verifier = predicateGenerator.getPredicate(sdocument.getLdProof().getVerificationMethod());
        System.out.println(verifier.test(sdocument));
        System.out.println(verifier.test(sdocument.getVerifiableCredential()));
    }

}
