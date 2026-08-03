package com.example.careerpilot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey key;

    private final long expirationTime;


    public JwtUtils(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long expirationTime
    ) {

        this.key =
                Keys.hmacShaKeyFor(
                        jwtSecret.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        this.expirationTime =
                expirationTime;
    }


    
    
    

    public String generateToken(
            Long userId
    ) {

        Date now =
                new Date();


        Date expiry =
                new Date(
                        now.getTime()
                                + expirationTime
                );


        return Jwts.builder()

                .setSubject(
                        String.valueOf(userId)
                )

                .setIssuedAt(now)

                .setExpiration(expiry)

                .signWith(key)

                .compact();
    }


    
    
    

    public Long extractUserId(
            String token
    ) {

        String subject =
                extractClaims(token)
                        .getSubject();


        return Long.valueOf(subject);
    }


    
    
    

    public boolean isTokenValid(
            String token
    ) {

        try {

            Claims claims =
                    extractClaims(token);


            Date expiration =
                    claims.getExpiration();


            return expiration != null
                    && expiration.after(
                    new Date()
            );

        } catch (Exception exception) {

            return false;
        }
    }


    
    
    

    private Claims extractClaims(
            String token
    ) {

        return Jwts.parserBuilder()

                .setSigningKey(key)

                .build()

                .parseClaimsJws(token)

                .getBody();
    }
}