package com.boon.bank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bankingOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Boon Bank API").version("1.0.0")
                        .description("Banking system — Customer, Account, Transaction"));
    }
}
