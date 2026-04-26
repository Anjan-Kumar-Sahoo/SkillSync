package com.skillsync.apigateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    @Test
    void shouldReturnGatewayHealthStatusUp() {
        HealthController controller = new HealthController();

        ResponseEntity<Map<String, String>> response = controller.healthCheck();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }
}
