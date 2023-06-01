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

package org.eclipse.tractusx.selfdescriptionfactory.service.verifier.impl.local;

import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.tractusx.selfdescriptionfactory.service.verifier.PredicateGenerator;
import org.eclipse.tractusx.selfdescriptionfactory.service.verifier.VerifierFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;


@Configuration
@RequiredArgsConstructor
public class LocalVerifierFactoryImpl implements VerifierFactory {
    @Value("#{${app.verifier}}")
    private Map<String, String> certificates;

    private final Map<URI, RSAPublicKey> publicKeyMap = new HashMap<>();
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException, CertificateException {
        for(var conf: certificates.entrySet()){
            try (InputStream certStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(conf.getValue()))) {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                var certs = certFactory.generateCertificates(certStream);
                if (certs.size() == 1) {
                    if (certs.iterator().next().getPublicKey() instanceof RSAPublicKey rsaPublicKey) {
                        publicKeyMap.put(URI.create(conf.getKey()), rsaPublicKey);
                    }
                }
            }
        }
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Override
    public PredicateGenerator predicateGenerator() {
        return verificationMethod -> {
            PublicKeyVerifier<RSAPublicKey> pkVerifier = new RSA_PS256_PublicKeyVerifier(
                    publicKeyMap.get(verificationMethod)
            );
            var verifier = new JsonWebSignature2020LdVerifier(pkVerifier);
            return new Predicate<>() {
                @Override
                @SneakyThrows
                public boolean test(JsonLDObject jsonLd) {
                    return verifier.verify(objectMapper.convertValue(jsonLd, jsonLd.getClass()));
                }
            };
        };
    }
}
