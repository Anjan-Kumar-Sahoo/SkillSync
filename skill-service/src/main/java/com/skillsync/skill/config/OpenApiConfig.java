package com.skillsync.skill.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skill Service API")
                        .description("SkillSync Skill Service - Centralized Skill Catalog & Category Management")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillSync Team")));
    }
}
