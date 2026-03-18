package com.lpu.java.auth_service.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lpu.java.auth_service.entity.Otp;
import com.lpu.java.auth_service.exception.InvalidOtpException;
import com.lpu.java.auth_service.exception.OtpAttemptsExceededException;
import com.lpu.java.auth_service.exception.OtpExpiredException;
import com.lpu.java.auth_service.exception.OtpNotFoundException;
import com.lpu.java.auth_service.repository.OtpRepository;


@Service
public class OtpService1 {

	@Autowired
	private OtpRepository repo;

//	 🔥 GENERATE OTP
	public String generateOtp() {
		return String.valueOf(100000 + new Random().nextInt(900000));
	}

	// 🔥 SAVE OTP
	public void saveOtp(String email, String otp) {

		Otp entity = new Otp();
		entity.setIdentifier(email);
		entity.setOtp(otp);
		entity.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // 🔥 expiry
		entity.setCreatedAt(LocalDateTime.now());
		entity.setUsed(false);
		entity.setAttempts(0);

		repo.save(entity);
	}

	// 🔥 COOLDOWN (30 sec)
	public void checkCooldown(String email) {

		Optional<Otp> lastOtp = repo.findTopByIdentifierOrderByIdDesc(email);

		if (lastOtp.isPresent()) {

			LocalDateTime lastTime = lastOtp.get().getCreatedAt();

			if (lastTime.plusSeconds(30).isAfter(LocalDateTime.now())) {
				throw new OtpAttemptsExceededException("Wait 30 seconds before requesting OTP again");
			}
		}
	}
	
	 @Transactional
	    public void deleteExpiredOtps() {
	        repo.deleteByExpiryTimeBefore(LocalDateTime.now());
	    }

	// 🔥 VERIFY OTP
	public void verifyOtp(String email, String otp) {

		Otp savedOtp = repo.findTopByIdentifierAndIsUsedFalseOrderByIdDesc(email)
				.orElseThrow(() -> new OtpNotFoundException("No OTP found for email"));

		// ❌ EXPIRY
		if (savedOtp.getExpiryTime().isBefore(LocalDateTime.now())) {
			throw new OtpExpiredException("OTP expired. Please request a new one.");
		}

		// ❌ ATTEMPTS LIMIT
		if (savedOtp.getAttempts() >= 3) {
			throw new OtpAttemptsExceededException("Maximum attempts reached");
		}

		// ❌ INVALID OTP
		if (!savedOtp.getOtp().equals(otp)) {

		    savedOtp.setAttempts(savedOtp.getAttempts() + 1);
		    repo.save(savedOtp);

		    throw new InvalidOtpException("Invalid OTP entered");
		}
	}

	// 🔥 MARK VERIFIED
	public void markVerified(String email) {

		Otp otp = repo.findTopByIdentifierAndIsUsedFalseOrderByIdDesc(email)
				.orElseThrow(() -> new RuntimeException("OTP not found"));

		otp.setUsed(true);
		repo.save(otp);
	}

	// 🔥 CHECK VERIFIED
	public boolean isOtpVerified(String email) {

		Optional<Otp> latestOtp = repo.findTopByIdentifierOrderByIdDesc(email);

		if (latestOtp.isEmpty()) {
			return false; // ❌ No OTP generated
		}

		return latestOtp.get().isUsed(); // ✅ Only true if verified
	}
}