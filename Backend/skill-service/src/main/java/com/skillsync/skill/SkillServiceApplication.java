package com.skillsync.skill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.skillsync.skill", "com.skillsync.cache"})
public class SkillServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkillServiceApplication.class, args);
    }
}
