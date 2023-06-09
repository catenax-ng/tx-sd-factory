package org.eclipse.tractusx.selfdescriptionfactory.service.fc;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.service.keycloak.KeycloakManager;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor
public abstract class FederatedCatalog {
    @Autowired
    protected KeycloakManager keycloakManager;

    public abstract void uploadLegalPerson(VerifiableCredential payload, String token);

    public abstract void uploadServiceOffering(VerifiableCredential payload, String token);



    public void uploadServiceOffering(VerifiableCredential verifiableCredential) {
        uploadLegalPerson(verifiableCredential, "Bearer ".concat(keycloakManager.getToken("federatedCatalog")));
    }

    public void uploadLegalPerson(VerifiableCredential verifiableCredential) {
        uploadServiceOffering(verifiableCredential, "Bearer ".concat(keycloakManager.getToken("federatedCatalog")));
    }
}
