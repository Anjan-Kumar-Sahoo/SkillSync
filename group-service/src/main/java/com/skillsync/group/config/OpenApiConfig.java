package com.skillsync.group.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI groupServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Group Service API")
                        .description("SkillSync Group Service - Peer Learning Groups, Membership, Discussions")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillSync Team")));
    }
}
