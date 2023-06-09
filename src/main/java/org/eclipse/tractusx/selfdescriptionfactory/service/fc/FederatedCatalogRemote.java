package org.eclipse.tractusx.selfdescriptionfactory.service.fc;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FederatedCatalogRemote extends FederatedCatalog {
    private final LegalPersonClient legalPersonClient;
    private final ServiceOfferingClient serviceOfferingClient;

    @Override
    @SneakyThrows
    public void uploadLegalPerson(VerifiableCredential payload, String token) {
        legalPersonClient.uploadLegalPerson(payload);
    }

    @Override
    @SneakyThrows
    public void uploadServiceOffering(VerifiableCredential payload, String token) {
        serviceOfferingClient.uploadServiceOffering(payload);
    }


}
