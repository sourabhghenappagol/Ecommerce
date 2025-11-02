package com.example.ecommerce.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI productServiceOpenAPI() {
        Server devServer = new Server()
            .url("http://localhost:" + serverPort)
            .description("Development server");

        Contact contact = new Contact()
            .name("Product Service Team")
            .email("support@example.com");

        Info info = new Info()
            .title("Product Service API")
            .version("1.0")
            .description("API documentation for Product Service")
            .contact(contact)
            .license(new License().name("Apache 2.0").url("http://springdoc.org"));

        return new OpenAPI()
            .info(info)
            .servers(List.of(devServer));
    }
}
