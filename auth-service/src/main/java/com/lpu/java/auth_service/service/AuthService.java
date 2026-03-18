//package com.lpu.java.auth_service.service;
//
//import java.time.LocalDateTime;
//import java.util.Random;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import com.lpu.java.auth_service.config.JwtUtil;
//import com.lpu.java.auth_service.entity.AuthUser;
//import com.lpu.java.auth_service.entity.Otp;
//import com.lpu.java.auth_service.external.dto.LoginRequest;
//import com.lpu.java.auth_service.external.dto.RegisterRequest;
//import com.lpu.java.auth_service.payload.ApiResponse;
//import com.lpu.java.auth_service.repository.AuthUserRepository;
//
//@Service
//public class AuthService {
//
//    @Autowired
//    private AuthUserRepository repo;
//
//    @Autowired
//    private PasswordEncoder encoder;
//
//    @Autowired
//    private AuthenticationManager manager;
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    @Autowired
//    private OtpService otpService;
//
//    @Autowired
//    private EmailService emailService;
//
////    // ================= REGISTER =================
////    public ResponseEntity<ApiResponse<String>> register(RegisterRequest req) {
////
////        if (repo.findByEmail(req.getEmail()) != null) {
////            throw new RuntimeException("User already exists");
////        }
////
////        AuthUser user = new AuthUser();
////        user.setEmail(req.getEmail());
////        user.setFirstName(req.getName());
////        user.setPasswordHash(encoder.encode(req.getPassword()));
////        user.setLastName("NA"); 
////        user.setRole("ROLE_USER");
////        user.setActive(true);
////        user.setVerified(false);
////
////        repo.save(user);
////
////        // 🔥 Send OTP
////        String otp = otpService.generateOtp();
////        otpService.saveOtp(req.getEmail(), otp);
////        emailService.sendOtpEmail(req.getEmail(), otp);
////
////        return ResponseEntity.status(HttpStatus.CREATED)
////                .body(new ApiResponse<>("SUCCESS", "OTP sent to email", null));
////    }
//
// 
//
//    // ================= LOGIN =================
//    public ResponseEntity<ApiResponse<String>> login(LoginRequest req) {
//
//        AuthUser user = repo.findByEmail(req.getEmail());
//
//        // ❌ USER NOT FOUND
//        if (user == null) {
//            throw new RuntimeException("User not found");
//        }
//
//        // ❌ NOT VERIFIED
//        if (!user.isVerified()) {
//            throw new RuntimeException("Please verify OTP first");
//        }
//
//        // ❌ ACCOUNT DISABLED
//        if (!user.isActive()) {
//            throw new RuntimeException("Account is disabled");
//        }
//
//        // 🔐 AUTHENTICATE
//        manager.authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        req.getEmail(),
//                        req.getPassword()
//                )
//        );
//
//        // 🔑 GENERATE JWT
//        String token = jwtUtil.generateToken(req.getEmail());
//
//        return ResponseEntity.ok(
//                new ApiResponse<>("SUCCESS", "Login successful", token)
//        );
//    }
//
//    // ================= HELPER =================
//    public AuthUser findByEmail(String email) {
//        return repo.findByEmail(email);
//    }
//}