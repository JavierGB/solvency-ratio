package com.santander.solvencyratio.config;

import com.santander.digital.verifiedid.impl.VerifiedIdClientImp;
import com.santander.digital.verifiedid.impl.VerifiedIdClientImpBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RatioConfig {

    @Bean
    public VerifiedIdClientImp verifiedIdClient(RatioProperties properties) {
        final VerifiedIdClientImp verifiedIdClient = new VerifiedIdClientImpBuilder()
                .withWellKnownURI(properties.getConfigUri())
                .withPrivateJWKFromFile(properties.getJwk())
                .withClientId(properties.getClientId())
                .build();
        verifiedIdClient.setUpClient();
        return verifiedIdClient;
    }


}
