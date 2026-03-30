package com.skillsync.session.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sessionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Session Service API")
                        .description("SkillSync Session Service — Booking, Lifecycle, Conflict Detection, Reviews & Ratings.\n\n"
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
