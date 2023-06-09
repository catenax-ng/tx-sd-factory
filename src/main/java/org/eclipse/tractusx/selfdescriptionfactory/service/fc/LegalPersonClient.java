package org.eclipse.tractusx.selfdescriptionfactory.service.fc;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "legalPersonFC", url = "${app.usersDetails.federatedCatalog.legalPersonUru}")
public interface LegalPersonClient {
    @PostMapping
    void uploadLegalPerson(@RequestBody VerifiableCredential verifiableCredential);
}
