package com.skillsync.eurekaserver;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class EurekaServerApplicationTest {

    @Test
    void shouldInstantiateApplicationClass() {
        assertTrue(new EurekaServerApplication() instanceof EurekaServerApplication);
    }

    @Test
    void shouldBeAnnotatedAsSpringBootAndEurekaServerApplication() {
        assertTrue(EurekaServerApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(EurekaServerApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }

    @Test
    void shouldDelegateMainMethodToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(eq(EurekaServerApplication.class), any(String[].class)))
                    .thenReturn(null);

            EurekaServerApplication.main(new String[]{"--spring.main.web-application-type=none"});

            mocked.verify(() -> SpringApplication.run(eq(EurekaServerApplication.class), any(String[].class)));
        }
    }
}
