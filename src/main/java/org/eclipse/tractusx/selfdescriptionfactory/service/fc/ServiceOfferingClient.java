package org.eclipse.tractusx.selfdescriptionfactory.service.fc;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "serviceOfferingFC", url = "${app.usersDetails.federatedCatalog.serviceOfferingUri}")
public interface ServiceOfferingClient {
    @PostMapping
    void uploadServiceOffering(@RequestBody VerifiableCredential verifiableCredential);
}
