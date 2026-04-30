package com.skillsync.payment.consumer;

import com.skillsync.payment.config.RabbitMQConfig;
import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.repository.FailedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DlqConsumer Unit Tests")
class DlqConsumerTest {

    @Mock private FailedEventRepository failedEventRepository;
    private MeterRegistry meterRegistry;
    private Counter dlqCounter;
    private DlqConsumer consumer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        consumer = new DlqConsumer(failedEventRepository, meterRegistry);
        consumer.initMetrics();
        dlqCounter = meterRegistry.find("dlq.events.received").counter();
    }

    private Message createMessage(String payload, String eventId, Object xDeath) {
        MessageProperties props = new MessageProperties();
        if (eventId != null) {
            props.setHeader("x-event-id", eventId);
        }
        if (xDeath != null) {
            props.setHeader("x-death", xDeath);
        }
        return new Message(payload.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    @DisplayName("handleBusinessActionDlq: Success path")
    void handleBusinessActionDlq_Success() {
        Message msg = createMessage("payload1", "evt-1", List.of("reason1"));
        when(failedEventRepository.existsByEventId("evt-1")).thenReturn(false);

        consumer.handleBusinessActionDlq(msg);

        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        FailedEvent saved = captor.getValue();

        assertThat(saved.getEventId()).isEqualTo("evt-1");
        assertThat(saved.getErrorReason()).isEqualTo("reason1");
        assertThat(dlqCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("handleSuccessDlq: Duplicate check")
    void handleSuccessDlq_Duplicate() {
        Message msg = createMessage("payload2", "evt-2", null);
        when(failedEventRepository.existsByEventId("evt-2")).thenReturn(true);

        consumer.handleSuccessDlq(msg);

        verify(failedEventRepository, never()).save(any());
        assertThat(dlqCounter.count()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("handleFailedDlq: Missing headers defaults")
    void handleFailedDlq_MissingHeaders() {
        Message msg = createMessage("payload3", null, null);

        consumer.handleFailedDlq(msg);

        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        FailedEvent saved = captor.getValue();

        assertThat(saved.getEventId()).startsWith("unknown-");
        assertThat(saved.getErrorReason()).isEqualTo("Unknown failure");
    }

    @Test
    @DisplayName("extractDeathReason: xDeath is empty list")
    void extractDeathReason_EmptyList() {
        Message msg = createMessage("payload", "evt-4", Collections.emptyList());
        
        consumer.handleFailedDlq(msg);

        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorReason()).isEqualTo("Unknown failure");
    }

    @Test
    @DisplayName("extractDeathReason: xDeath is not a list")
    void extractDeathReason_NotAList() {
        Message msg = createMessage("payload", "evt-5", "not-a-list");
        
        consumer.handleFailedDlq(msg);

        ArgumentCaptor<FailedEvent> captor = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedEventRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorReason()).isEqualTo("Unknown failure");
    }
}
