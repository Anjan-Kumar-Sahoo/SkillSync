package com.lpu.java.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lpu.java.auth_service.entity.AuthUser;
import com.lpu.java.auth_service.entity.Otp;


@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser,Long>{

	 AuthUser findByEmail(String email);

	// Otp findTopByIdentifierOrderByIdDesc(String email);
}
