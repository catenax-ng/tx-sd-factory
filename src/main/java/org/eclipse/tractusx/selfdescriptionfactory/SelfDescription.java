package org.eclipse.tractusx.selfdescriptionfactory;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
public class SelfDescription {
    private final String externalId;
    private final List<VerifiableCredential> verifiableCredentialList = new ArrayList<>();
}
