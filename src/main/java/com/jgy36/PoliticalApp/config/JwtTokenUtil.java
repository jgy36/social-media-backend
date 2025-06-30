package com.jgy36.PoliticalApp.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}") // ✅ Inject secret from properties
    private String secret;

    @Value("${jwt.expirationMs}") // ✅ Inject expiration time from properties
    private long expirationMs;

    /**
     * ✅ Generates a JWT token for an authenticated user.
     *
     * @param email The user's email (unique identifier).
     * @return A JWT token as a String.
     */
    public String generateToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * ✅ Generates a JWT token with custom expiration time
     *
     * @param email             The user's email
     * @param expirationSeconds Custom expiration time in seconds
     * @return JWT token
     */
    public String generateToken(String email, int expirationSeconds) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000L));

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * ✅ Generates a temporary token for 2FA verification
     */
    public String generateTempToken(String email) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 300000); // 5 minutes

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "temp")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * ✅ Validates temporary token and returns claims
     */
    public Claims validateTempToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"temp".equals(claims.get("type"))) {
                throw new RuntimeException("Invalid token type");
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Verification token has expired");
        } catch (Exception e) {
            throw new RuntimeException("Invalid verification token");
        }
    }

    /**
     * ✅ Extracts the username (email) from a JWT token.
     *
     * @param token The JWT token.
     * @return The extracted email.
     */
    public String getUsernameFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            System.out.println("✅ Extracted Username from Token: " + claims.getSubject());
            return claims.getSubject();
        } catch (Exception e) {
            System.out.println("❌ Error extracting username from JWT: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Extracts the expiration date from a JWT token.
     *
     * @param token The JWT token.
     * @return The expiration date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * ✅ Extracts a specific claim from a JWT token.
     *
     * @param token          The JWT token.
     * @param claimsResolver A function to extract a claim.
     * @param <T>            The claim type.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * ✅ Parses the JWT token and retrieves all claims.
     *
     * @param token The JWT token.
     * @return The claims inside the token.
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        JwtParser parser = Jwts.parser().verifyWith(key).build();

        return parser.parseSignedClaims(token).getPayload();
    }

    /**
     * ✅ Checks if a JWT token is expired.
     *
     * @param token The JWT token.
     * @return True if expired, False if valid.
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * ✅ Validates a JWT token by checking username and expiration.
     *
     * @param token       The JWT token.
     * @param userDetails The authenticated user details.
     * @return True if valid, False otherwise.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();

            if (expiration.before(new Date())) {
                System.out.println("🚨 Token expired at: " + expiration);
                return false;
            }

            String email = getUsernameFromToken(token);
            return (email.equals(userDetails.getUsername()));
        } catch (ExpiredJwtException e) {
            System.out.println("🚨 Token is expired!");
            return false;
        } catch (JwtException e) {
            System.out.println("🚨 Invalid token!");
            return false;
        }
    }

    /**
     * ✅ Extracts expiration time from a JWT token.
     *
     * @param token The JWT token.
     * @return The expiration timestamp.
     */
    public long getExpirationFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration().getTime();
    }
}
