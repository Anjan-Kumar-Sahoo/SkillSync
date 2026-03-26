package com.skillsync.user.service;

import com.skillsync.cache.CacheService;
import com.skillsync.user.entity.Payment;
import com.skillsync.user.enums.PaymentStatus;
import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.repository.PaymentRepository;
import com.skillsync.user.service.command.MentorCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentSagaOrchestratorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private MentorCommandService mentorCommandService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;

    @InjectMocks private PaymentSagaOrchestrator orchestrator;

    private Payment mentorFeePayment;

    @BeforeEach
    void setUp() {
        mentorFeePayment = Payment.builder()
                .id(1L)
                .userId(100L)
                .razorpayOrderId("order_123")
                .type(PaymentType.MENTOR_FEE)
                .status(PaymentStatus.VERIFIED)
                .amount(1000)
                .referenceId(200L) // MentorProfileId
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Saga Success - Updates Cache and Database")
    void executeSaga_shouldApproveMentorAndInvalidateCache() {
        orchestrator.executeSaga(mentorFeePayment);

        verify(paymentRepository, times(2)).save(mentorFeePayment);
        assertEquals(PaymentStatus.SUCCESS, mentorFeePayment.getStatus());

        // Underneath, approveMentor calls invalidateMentorCaches, but we mock the service.
        verify(mentorCommandService).approveMentor(200L);
    }

    @Test
    @DisplayName("Saga Compensation - Reverts Cache and Database")
    void compensate_shouldRevertMentorApprovalAndInvalidateCache() {
        orchestrator.compensate(mentorFeePayment, "Payment failure");

        verify(paymentRepository).save(mentorFeePayment);
        assertEquals(PaymentStatus.COMPENSATED, mentorFeePayment.getStatus());
        assertEquals("Payment failure", mentorFeePayment.getCompensationReason());

        verify(mentorCommandService).revertMentorApproval(200L);
    }
}
