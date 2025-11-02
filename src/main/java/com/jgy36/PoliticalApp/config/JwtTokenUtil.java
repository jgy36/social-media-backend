package com.jgy36.PoliticalApp.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:U29jaWFsTWVkaWFBcHBTZWNyZXRLZXlNdXN0QmVCYXNlNjRFbmNvZGVkIQ==}")
    private String secret;

    @Value("${jwt.expirationMs:900000}")
    private long accessTtlMs;

    @Value("${jwt.refreshExpirationMs:604800000}")
    private long refreshTtlMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private JwtParser parser() {
        return Jwts.parser().verifyWith(key()).build();
    }

    public String generateAccessToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTtlMs)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public String getSubjectIfValidAccess(String token) {
        Claims claims = parser().parseSignedClaims(token).getPayload();
        Object type = claims.get("type");
        if ("refresh".equals(type) || "temp".equals(type)) {
            throw new JwtException("Wrong token type for access");
        }
        return claims.getSubject();
    }

    public String generateRefreshToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTtlMs)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateTempToken(String email) {
        long tempTtlMs = 5 * 60 * 1000L;
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("type", "temp")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(tempTtlMs)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateTempToken(String token) {
        Claims claims = parser().parseSignedClaims(token).getPayload();
        Object type = claims.get("type");
        if (!"temp".equals(type)) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }

    public String getSubjectIfValidRefresh(String token) {
        Claims claims = parser().parseSignedClaims(token).getPayload();
        if (!"refresh".equals(claims.get("type"))) {
            throw new JwtException("Wrong token type");
        }
        return claims.getSubject();
    }

    public String getSubject(String token) {
        return parser().parseSignedClaims(token).getPayload().getSubject();
    }

    public String getUsernameFromToken(String token) {
        try {
            return getSubject(token);
        } catch (JwtException e) {
            return null;
        }
    }

    public long getExpirationFromToken(String token) {
        return parser().parseSignedClaims(token).getPayload().getExpiration().getTime();
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parser().parseSignedClaims(token).getPayload().getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException ex) {
            return true;
        }
    }

    public boolean validateTokenForUser(String token, UserDetails user) {
        try {
            Claims claims = parser().parseSignedClaims(token).getPayload();
            if (claims.getExpiration().before(new Date())) {
                return false;
            }
            return user.getUsername().equals(claims.getSubject());
        } catch (JwtException ex) {
            return false;
        }
    }

    public String generateToken(String email) {
        return generateAccessToken(email);
    }

    public String generateToken(String email, int expirationSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }
}
