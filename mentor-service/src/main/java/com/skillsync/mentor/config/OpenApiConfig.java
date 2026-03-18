package com.skillsync.mentor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mentorServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mentor Service API")
                        .description("SkillSync Mentor Service - Onboarding, Discovery, Availability, Admin Approval")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillSync Team")));
    }
}
