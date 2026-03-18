package com.lpu.java.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lpu.java.auth_service.entity.AuthUser;
import com.lpu.java.auth_service.exception.InvalidCredentialsException;
import com.lpu.java.auth_service.exception.OtpNotVerifiedException;
import com.lpu.java.auth_service.exception.UserAlreadyExistException;
import com.lpu.java.auth_service.external.dto.LoginRequest;
import com.lpu.java.auth_service.external.dto.RegisterRequest;
import com.lpu.java.auth_service.payload.ApiResponse;
import com.lpu.java.auth_service.repository.AuthUserRepository;
import com.lpu.java.common_security.config.JwtUtil;

@Service
public class AuthService1 {

    @Autowired
    private AuthUserRepository repo;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpService1 otpService;

    @Autowired
    private EmailService emailService;

    // ================= SEND OTP =================
    public ResponseEntity<ApiResponse<String>> sendOtp(String email) {

        // 🔥 COOLDOWN CHECK (30 sec)
        otpService.checkCooldown(email);

        String otp = otpService.generateOtp();

        otpService.saveOtp(email, otp);

        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "OTP sent to email", null)
        );
    }

    // ================= VERIFY OTP =================
    public ResponseEntity<ApiResponse<String>> verifyOtp(String email, String otp) {

        otpService.verifyOtp(email, otp);

        otpService.markVerified(email);

        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "OTP verified successfully", null)
        );
    }

    // ================= REGISTER =================
    public ResponseEntity<ApiResponse<String>> register(RegisterRequest req) {

    	if (!otpService.isOtpVerified(req.getEmail())) {
    	    throw new OtpNotVerifiedException("Please verify OTP before registering");
    	}

    	if (repo.findByEmail(req.getEmail()) != null) {
    	    throw new UserAlreadyExistException("User already exists with email: " + req.getEmail());
    	}

        AuthUser user = new AuthUser();
        user.setEmail(req.getEmail());
        user.setFirstName(req.getName());
        user.setLastName("NA");
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setRole("ROLE_USER");
        user.setActive(true);
        user.setVerified(true);

        repo.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "User registered successfully", null)
        );
    }

    // ================= LOGIN =================
    public ResponseEntity<ApiResponse<String>> login(LoginRequest req) {

        AuthUser user = repo.findByEmail(req.getEmail());

        if (user == null) throw new RuntimeException("User not found");

        try {
            manager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    req.getEmail(),
                    req.getPassword()
                )
            );
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        String token = jwtUtil.generateToken(user.getEmail(),user.getRole());

        return ResponseEntity.ok(
                new ApiResponse<>("SUCCESS", "Login successful", token)
        );
    }
}