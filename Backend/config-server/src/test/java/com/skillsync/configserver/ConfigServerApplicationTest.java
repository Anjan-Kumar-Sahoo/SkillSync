package com.skillsync.configserver;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class ConfigServerApplicationTest {

    @Test
    void shouldInstantiateApplicationClass() {
        assertTrue(new ConfigServerApplication() instanceof ConfigServerApplication);
    }

    @Test
    void shouldBeAnnotatedAsSpringBootAndConfigServerApplication() {
        assertTrue(ConfigServerApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(ConfigServerApplication.class.isAnnotationPresent(EnableConfigServer.class));
    }

    @Test
    void shouldDelegateMainMethodToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(eq(ConfigServerApplication.class), any(String[].class)))
                    .thenReturn(null);

            ConfigServerApplication.main(new String[]{"--spring.main.web-application-type=none"});

            mocked.verify(() -> SpringApplication.run(eq(ConfigServerApplication.class), any(String[].class)));
        }
    }
}
