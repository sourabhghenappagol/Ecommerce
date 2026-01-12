package com.ecommerce.productservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenAPI() {

        // Define JWT security scheme
        SecurityScheme securityScheme = new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Product Service API")
                        .description("APIs for Product management with JWT security")
                        .version("1.0.0")
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme)
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
