//package com.lpu.java.auth_service.config;
//
//import java.util.Date;
//
//import org.springframework.stereotype.Component;
//
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//
//@Component
//public class JwtUtil {
//
//    private String secret = "b7d4f8a9e6c2d1f5b9a7c3e6f2d8a1b4b7d4f8a9e6c2d1f5b9a7c3e6f2d8a1";
//
//    public String generateToken(String username) {
//
//        return Jwts.builder()
//                .setSubject(username)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis()+1000*60*60))
//                .signWith(SignatureAlgorithm.HS256, secret)
//                .compact();
//    }
//
//    public String extractUsername(String token) {
//        return Jwts.parser()
//                .setSigningKey(secret)
//                .parseClaimsJws(token)
//                .getBody()
//                .getSubject();
//    }
//}
