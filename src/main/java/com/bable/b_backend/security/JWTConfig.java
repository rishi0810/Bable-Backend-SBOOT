package com.bable.b_backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bable.b_backend.mappers.JWTBody;
import com.bable.b_backend.utils.Constants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTConfig {   

    // 32 Bit base 64 Secret
    @Value("${app.jwt.secret}")
    private String secret;

    // Function for generation of JWT token
    public String generateJWTToken (JWTBody entity){

        // Convert secret to UTF-8 - SHA 256 
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        // Defining Expiration time to 15 minutes
        Date exp = new Date(System.currentTimeMillis() + Constants.EXPIRY_TIME);

        // return signed JWT with JWTBody DTO
        return Jwts.builder()
                .claim("_id", entity.getId())
                .claim("name", entity.getName())
                .claim("email", entity.getEmail())
                .signWith(key, Jwts.SIG.HS256)
                .expiration(exp)
                .compact();

    }

    // Helper function to parse JWT and extract signed Payload from Secret
    private Claims extractClaims (String token){
       try {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

    } catch (ExpiredJwtException e) {
        throw new JwtException("Expired JWT", e);
    } catch (JwtException | IllegalArgumentException e) {
        throw new JwtException("Invalid JWT", e);
    }

    }
    // Function to get JWTBody of current signed in user via claim -> validator
    public JWTBody getCurrentUser (String token){
        JWTBody currUser = new JWTBody();
        Claims claim = extractClaims(token);


        String userId = claim.get("_id", String.class);
        String userName = claim.get("name", String.class);
        String userEmail = claim.get("email", String.class);

        currUser.setId(userId);
        currUser.setName(userName);
        currUser.setEmail(userEmail);

        return currUser;
    }
    
    // Function to get the remaining time for the current token
    public long remainingTime (String token){
        Claims claim = extractClaims(token);
        return claim.getExpiration().getTime()-System.currentTimeMillis();
    }
}
