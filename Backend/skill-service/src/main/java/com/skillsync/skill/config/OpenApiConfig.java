package com.skillsync.skill.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${APP_PUBLIC_BASE_URL:https://skillsync.mraks.dev}")
    private String publicBaseUrl;

    @Bean
    public OpenAPI skillServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url(publicBaseUrl).description("Public API Gateway")))
                .info(new Info()
                        .title("Skill Service API")
                        .description("SkillSync Skill Service - Centralized Skill Catalog & Category Management")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillSync Team")));
    }
}
