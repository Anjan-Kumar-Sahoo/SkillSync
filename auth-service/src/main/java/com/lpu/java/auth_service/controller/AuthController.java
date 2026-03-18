//package com.lpu.java.auth_service.controller;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.lpu.java.auth_service.external.dto.LoginRequest;
//import com.lpu.java.auth_service.external.dto.RegisterRequest;
//import com.lpu.java.auth_service.payload.ApiResponse;
//import com.lpu.java.auth_service.service.AuthService;
//import com.lpu.java.auth_service.service.OtpService;
//
//@RestController
//@RequestMapping("/auth")
//public class AuthController {
//
//    @Autowired
//    private AuthService authService;
//
//    @Autowired
//    private OtpService otpService;
//
//    @PostMapping("/register")
//    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest request) {
//        return authService.register(request);
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {
//        return authService.login(request); // 🔥 FIXED
//    }
//
//    @PostMapping("/verify-otp")
//    public ResponseEntity<?> verifyOtp(
//            @RequestParam String email,
//            @RequestParam String otp) {
//
//        otpService.verifyOtp(email, otp);
//        return ResponseEntity.ok("OTP verified successfully");
//    }
//
//    @GetMapping("/home")
//    public ResponseEntity<String> home() {
//        return ResponseEntity.ok("WELCOME TO HOME PAGE");
//    }
//}