package org.eclipse.tractusx.selfdescriptionfactory;

import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianClient;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SignerTest {

    @MockBean
    CustodianClient custodianClient;

    @Autowired
    private MockMvc mockMvc;

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
    }
}
