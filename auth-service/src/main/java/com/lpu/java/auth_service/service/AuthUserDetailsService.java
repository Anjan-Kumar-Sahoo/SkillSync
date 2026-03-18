package com.lpu.java.auth_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lpu.java.auth_service.entity.AuthUser;
import com.lpu.java.auth_service.repository.AuthUserRepository;

@Service
public class AuthUserDetailsService implements UserDetailsService{

	   @Autowired
	   private AuthUserRepository repo;

	   @Override
	   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		  
		    AuthUser au=repo.findByEmail(username);
		    return new AuthUserDetails(au);
	   }
}
