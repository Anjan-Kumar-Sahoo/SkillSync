package com.lpu.java.auth_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lpu.java.auth_service.entity.Otp;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByIdentifierAndIsUsedFalseOrderByIdDesc(String identifier);

    void deleteByExpiryTimeBefore(LocalDateTime time);
   
    Optional<Otp> findTopByIdentifierOrderByIdDesc(String identifier);
}