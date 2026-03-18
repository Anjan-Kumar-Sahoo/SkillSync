package com.lpu.java.auth_service.service;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.lpu.java.auth_service.entity.AuthUser;

public class AuthUserDetails implements UserDetails{
	
	 private AuthUser authUser;
	 public  AuthUserDetails(AuthUser authUser){
		  this.authUser=authUser;
	  }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		  String role = authUser.getRole();

		    if (role == null || role.isBlank()) {
		        role = "ROLE_USER"; // default role
		    }
		return Collections.singleton(new SimpleGrantedAuthority(role));
	}

	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return authUser.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return authUser.getEmail();
	}

}
