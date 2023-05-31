package org.eclipse.tractusx.selfdescriptionfactory.service.converter.fcformat;

import lombok.RequiredArgsConstructor;
import org.eclipse.tractusx.selfdescriptionfactory.dto.LegalPersonSD;
import org.eclipse.tractusx.selfdescriptionfactory.model.vrel3.LegalPersonSchema;
import org.eclipse.tractusx.selfdescriptionfactory.service.wallet.CustodianWallet;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LegalPersonSDConverter implements Converter<LegalPersonSchema, LegalPersonSD> {

    private final CustodianWallet custodianWallet;

    @Override
    public LegalPersonSD convert(@NonNull LegalPersonSchema legalPersonSchema) {
        LegalPersonSD legalPersonSD = new LegalPersonSD();
        if (legalPersonSchema.getRegistrationNumber().size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Registration Number must have single element for Federated Catalog"
            );
        }
        legalPersonSD.put("@context",
                Map.of( "gx", "https://w3id.org/gaia-x/gax-trust-framework#",
                        "xsd", "http://www.w3.org/2001/XMLSchema#",
                        "vcard", "http://www.w3.org/2006/vcard/ns#",
                        "ctxsd", "https://w3id.org/catena-x/core#")
        );
        legalPersonSD.put("@id", custodianWallet.getWalletData(legalPersonSchema.getHolder()).get("did"));
        legalPersonSD.put("@type", "gx:LegalPerson");
        legalPersonSD.put("ctxsd:bpn", legalPersonSchema.getBpn());
        legalPersonSD.put("gx:name", custodianWallet.getWalletData(legalPersonSchema.getBpn()).get("name"));
        legalPersonSD.put("gx:registrationNumber", legalPersonSchema.getRegistrationNumber().iterator().next().getValue());
        legalPersonSD.put("gx:headquarterAddress",
                Map.of( "@type", "vcard:Address",
                        "vcard:country-name", legalPersonSchema.getHeadquarterAddressCountry()
                )
        );
        legalPersonSD.put("gx:legalAddress",
                Map.of( "@type", "vcard:Address",
                        "vcard:country-name", legalPersonSchema.getLegalAddressCountry()
                )
        );
        return legalPersonSD;
    }
}
