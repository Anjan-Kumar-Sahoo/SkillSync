package com.skillsync.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Value("${APP_PUBLIC_BASE_URL:https://skillsync.mraks.dev}")
        private String publicBaseUrl;

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                                .servers(List.of(new Server().url(publicBaseUrl).description("SkillSync Public Gateway URL")))
                .info(new Info()
                        .title("Payment Service API")
                        .description("SkillSync Payment Service — Razorpay integration, payment lifecycle, saga orchestration.\n\n"
                                + "**Note:** Pass `X-User-Id` header manually when testing directly (bypassing Gateway).")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillSync Team")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .schemaRequirement("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .bearerFormat("JWT")
                                .scheme("bearer"));
    }
}
