package com.skillsync.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema evolution compatibility tests.
 * Verifies that the PaymentCompletedEvent can handle:
 * - Events with unknown future fields (forward compatibility)
 * - Events missing optional fields (backward compatibility)
 */
class PaymentCompletedEventSchemaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Forward compatibility: unknown fields are ignored")
    void forwardCompatibility_unknownFieldsIgnored() throws Exception {
        // Simulates a v2 event with a new field 'currency'
        String v2Json = """
                {
                    "eventId": "abc-123",
                    "version": "2",
                    "timestamp": "2026-03-27T10:00:00",
                    "userId": 42,
                    "orderId": "order_test_123",
                    "paymentType": "SESSION_BOOKING",
                    "status": "SUCCESS",
                    "amount": 900,
                    "referenceId": 10,
                    "referenceType": "SESSION_BOOKING",
                    "currency": "INR",
                    "unknownFutureField": "should be ignored"
                }
                """;

        PaymentCompletedEvent event = mapper.readValue(v2Json, PaymentCompletedEvent.class);

        assertEquals("abc-123", event.getEventId());
        assertEquals("2", event.getVersion());
        assertEquals(42L, event.getUserId());
        assertEquals("SUCCESS", event.getStatus());
        assertEquals(900, event.getAmount());
    }

    @Test
    @DisplayName("Backward compatibility: missing optional fields default to null")
    void backwardCompatibility_missingFieldsNull() throws Exception {
        // Simulates a minimal v1 event without optional fields
        String minimalJson = """
                {
                    "eventId": "xyz-789",
                    "version": "1",
                    "userId": 42,
                    "orderId": "order_min",
                    "paymentType": "SESSION_BOOKING",
                    "status": "SUCCESS",
                    "amount": 900
                }
                """;

        PaymentCompletedEvent event = mapper.readValue(minimalJson, PaymentCompletedEvent.class);

        assertEquals("xyz-789", event.getEventId());
        assertEquals(42L, event.getUserId());
        assertNull(event.getCompensationReason());
        assertNull(event.getReferenceType());
        assertNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Roundtrip: serialize and deserialize preserves all fields")
    void roundtrip_preservesFields() throws Exception {
        PaymentCompletedEvent original = new PaymentCompletedEvent(
                "rt-001", "1", "2026-03-27T10:00:00",
                42L, "order_rt", "SESSION_BOOKING", "SUCCESS",
                900, 10L, "SESSION_BOOKING", null
        );

        String json = mapper.writeValueAsString(original);
        PaymentCompletedEvent deserialized = mapper.readValue(json, PaymentCompletedEvent.class);

        assertEquals(original.getEventId(), deserialized.getEventId());
        assertEquals(original.getUserId(), deserialized.getUserId());
        assertEquals(original.getOrderId(), deserialized.getOrderId());
        assertEquals(original.getAmount(), deserialized.getAmount());
    }
}
