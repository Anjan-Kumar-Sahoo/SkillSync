package com.skillsync.apigateway;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class ApiGatewayApplicationTest {

    @Test
    void shouldDeclareExpectedApplicationAnnotations() {
        assertTrue(ApiGatewayApplication.class.isAnnotationPresent(SpringBootApplication.class));
        assertTrue(ApiGatewayApplication.class.isAnnotationPresent(EnableScheduling.class));
    }

//    @Test
//    void shouldDelegateMainMethodToSpringApplicationRun() {
//        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
//            mocked.when(() -> SpringApplication.run(eq(ApiGatewayApplication.class), any(String[].class)))
//                    .thenReturn(null);
//
//            ApiGatewayApplication.main(new String[]{"--spring.main.web-application-type=none"});
//
//            mocked.verify(() -> SpringApplication.run(eq(ApiGatewayApplication.class), any(String[].class)));
//        }
//    }
}
