package com.lpu.java.auth_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lpu.java.auth_service.service.OtpService1;

@Component
public class OtpCleanupScheduler {

    @Autowired
    private OtpService1 otpService;

    @Scheduled(fixedRate = 60000)
    public void cleanup() {
        otpService.deleteExpiredOtps();
        System.out.println("🧹 Expired OTPs cleaned");
    }
}