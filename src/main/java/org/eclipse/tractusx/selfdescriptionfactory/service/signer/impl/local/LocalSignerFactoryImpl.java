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

package org.eclipse.tractusx.selfdescriptionfactory.service.signer.impl.local;

import com.danubetech.keyformats.crypto.PrivateKeySigner;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PrivateKeySigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords;
import info.weboftrust.ldsignatures.signer.JsonWebSignature2020LdSigner;
import jakarta.annotation.PostConstruct;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.eclipse.tractusx.selfdescriptionfactory.service.signer.LDSigner;
import org.eclipse.tractusx.selfdescriptionfactory.service.signer.SignerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.security.*;
import java.util.Date;
import java.util.Objects;


@SuppressWarnings("unchecked")
@Configuration
@RequiredArgsConstructor
public class LocalSignerFactoryImpl implements SignerFactory {
    private  PrivateKeySigner<KeyPair> privateKeySigner;
    @Value("${app.signer.privateKey}")
    private String privateKeyFileName;
    @Value("${app.signer.verificationMethod}")
    private URI verificationMethod;
    private final ObjectMapper objectMapper;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @PostConstruct
    public void init() throws IOException, InvalidAlgorithmParameterException {
        @Cleanup Reader reader = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(privateKeyFileName)));
        PEMParser pemParser = new PEMParser(reader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());
        PrivateKey prk = converter.getPrivateKey(privateKeyInfo);
        if (!prk.getAlgorithm().equals("RSA")) {
            throw new InvalidAlgorithmParameterException("RSA key is required");
        }
        KeyPair kp = new KeyPair(null, prk);
        privateKeySigner = new RSA_PS256_PrivateKeySigner(kp);
    }

    @Override
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public LDSigner ldSigner() {
        return new LDSigner() {
            @SuppressWarnings("Unchecked")
            public <T extends JsonLDObject> T sign(T jsonLDObject) throws JsonLDException, GeneralSecurityException, IOException {
                var signer = new JsonWebSignature2020LdSigner(privateKeySigner);
                signer.setCreated(new Date());
                signer.setProofPurpose(LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD);
                signer.setVerificationMethod(verificationMethod);
                signer.setCreated(new Date());
                T copy = (T)objectMapper.convertValue(jsonLDObject, jsonLDObject.getClass());
                signer.sign(copy);
                return copy;
            }
        };
    }
}
