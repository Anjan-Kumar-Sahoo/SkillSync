package com.lpu.java.auth_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lpu.java.auth_service.external.dto.LoginRequest;
import com.lpu.java.auth_service.external.dto.RegisterRequest;
import com.lpu.java.auth_service.payload.ApiResponse;
import com.lpu.java.auth_service.service.AuthService1;

@RestController
@RequestMapping("/auth")
public class AuthController1 {

    @Autowired
    private AuthService1 authService;

    // 🔥 1. SEND OTP
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestParam String email) {
        return authService.sendOtp(email);
    }

    // 🔥 2. VERIFY OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        return authService.verifyOtp(email, otp);
    }

    // 🔥 3. REGISTER (ONLY AFTER OTP VERIFIED)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    // 🔥 4. LOGIN (JWT)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}