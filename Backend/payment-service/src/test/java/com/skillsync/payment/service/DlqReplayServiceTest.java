package com.skillsync.payment.service;

import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.repository.FailedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DlqReplayService Unit Tests")
class DlqReplayServiceTest {

    @Mock private FailedEventRepository failedEventRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    private MeterRegistry meterRegistry;
    
    private DlqReplayService dlqReplayService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dlqReplayService = new DlqReplayService(failedEventRepository, rabbitTemplate, meterRegistry);
        dlqReplayService.initMetrics();
    }

    @Test
    @DisplayName("replayEvent: Success path")
    void replayEvent_Success() {
        FailedEvent event = FailedEvent.builder()
                .eventId("evt-1")
                .routingKey("rk")
                .payload("payload")
                .replayStatus(FailedEvent.ReplayStatus.PENDING_REVIEW)
                .build();
        when(failedEventRepository.findByEventId("evt-1")).thenReturn(Optional.of(event));

        Map<String, String> result = dlqReplayService.replayEvent("evt-1");

        assertThat(result.get("status")).isEqualTo("REPLAYED");
        verify(rabbitTemplate).convertAndSend(anyString(), eq("rk"), eq("payload"), any(MessagePostProcessor.class));
        assertThat(event.getReplayStatus()).isEqualTo(FailedEvent.ReplayStatus.REPLAYED);
    }

    @Test
    @DisplayName("replayEvent: Already replayed skips")
    void replayEvent_Skipped() {
        FailedEvent event = FailedEvent.builder()
                .eventId("evt-2")
                .replayStatus(FailedEvent.ReplayStatus.REPLAYED)
                .build();
        when(failedEventRepository.findByEventId("evt-2")).thenReturn(Optional.of(event));

        Map<String, String> result = dlqReplayService.replayEvent("evt-2");

        assertThat(result.get("status")).isEqualTo("SKIPPED");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("replayEvent: Not found throws exception")
    void replayEvent_NotFound() {
        when(failedEventRepository.findByEventId("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dlqReplayService.replayEvent("none"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("skipEvent: Success path")
    void skipEvent_Success() {
        FailedEvent event = FailedEvent.builder().eventId("evt-3").build();
        when(failedEventRepository.findByEventId("evt-3")).thenReturn(Optional.of(event));

        Map<String, String> result = dlqReplayService.skipEvent("evt-3");

        assertThat(result.get("status")).isEqualTo("SKIPPED");
        assertThat(event.getReplayStatus()).isEqualTo(FailedEvent.ReplayStatus.SKIPPED);
    }
}
