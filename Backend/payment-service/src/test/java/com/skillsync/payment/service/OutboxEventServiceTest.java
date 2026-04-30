package com.skillsync.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.payment.entity.OutboxEvent;
import com.skillsync.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventService Unit Tests")
class OutboxEventServiceTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OutboxEventService outboxEventService;

    @Test
    @DisplayName("saveEvent: Success path")
    void saveEvent_Success() throws JsonProcessingException {
        Object payload = new Object();
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"key\":\"value\"}");

        String eventId = outboxEventService.saveEvent("ex", "rk", "type", payload);

        assertThat(eventId).isNotNull();
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getPayload()).isEqualTo("{\"key\":\"value\"}");
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("saveEvent: Serialization failure throws RuntimeException")
    void saveEvent_SerializationFailure() throws JsonProcessingException {
        Object payload = new Object();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Fail") {});

        assertThatThrownBy(() -> outboxEventService.saveEvent("ex", "rk", "type", payload))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize");
    }
}
