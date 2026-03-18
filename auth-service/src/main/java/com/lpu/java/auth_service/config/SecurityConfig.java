package com.lpu.java.auth_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.lpu.java.common_security.config.JwtFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private JwtFilter jwtFilter;

	@Bean
	public SecurityFilterChain filter(HttpSecurity http) throws Exception {

	    http.csrf(csrf -> csrf.disable());

	    http.authorizeHttpRequests(auth ->
	            auth.requestMatchers("/auth/register","/auth/login","/auth/verify-otp","/auth/send-otp").permitAll()
	                .anyRequest().authenticated()
	    );

	    http.sessionManagement(session ->
	            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	    );
	    
	    http.addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

	    return http.build();
	}
	   
	 @Bean
	    public AuthenticationManager authenticationManager(
	            AuthenticationConfiguration config) throws Exception {

	        return config.getAuthenticationManager();
	    }

	   @Bean
	   public PasswordEncoder encoder() {
		    return new BCryptPasswordEncoder();
	   }
}