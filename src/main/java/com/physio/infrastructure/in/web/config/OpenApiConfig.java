package com.physio.infrastructure.in.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Physio Manager API")
                        .version("1.0.0")
                        .description("API para gerenciamento de pacientes, serviços e agendamentos")
                        .contact(new Contact().name("Physio Team").email("dev@physio.local"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                );
    }
}

