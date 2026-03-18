//package com.lpu.java.auth_service.service;
//
//import java.time.LocalDateTime;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import com.lpu.java.auth_service.entity.Otp;
//import com.lpu.java.auth_service.repository.OtpRepository;
//
//@Service
//public class OtpService {
//
//    @Autowired
//    private OtpRepository otpRepository;
//
//    public String generateOtp() {
//        return String.valueOf((int)(Math.random() * 900000) + 100000);
//    }
//
//    public void saveOtp(String identifier, String otp) {
//        Otp otpEntity = new Otp();
//        otpEntity.setIdentifier(identifier);
//        otpEntity.setOtp(otp);
//        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(10));
//        otpEntity.setUsed(false);
//
//        otpRepository.save(otpEntity);
//    }
//
//    public void verifyOtp(String identifier, String otp) {
//
//        Otp savedOtp = otpRepository
//                .findTopByIdentifierAndIsUsedFalseOrderByIdDesc(identifier)
//                .orElseThrow(() -> new RuntimeException("OTP not found"));
//
//        if (savedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
//            throw new RuntimeException("OTP expired");
//        }
//
//        if (!savedOtp.getOtp().equals(otp)) {
//            throw new RuntimeException("Invalid OTP");
//        }
//
//        savedOtp.setUsed(true);
//        otpRepository.save(savedOtp);
//    }
//}
