package com.jgy36.PoliticalApp.config;

import com.jgy36.PoliticalApp.service.TokenBlacklistService;
import com.jgy36.PoliticalApp.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsServiceImpl userDetailsService;

    // Using constructor injection instead of @Autowired field injection
    public JwtTokenFilter(
            JwtTokenUtil jwtTokenUtil,
            TokenBlacklistService tokenBlacklistService,
            UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        // Log detailed request information
        logger.info("🔍 Request Details: {} {}", request.getMethod(), request.getRequestURI());

        // Skip token validation for specific endpoints
        String requestURI = request.getRequestURI();
        String[] skipPaths = {
                "/api/auth/register",
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/auth/check-username", // Add the new endpoint here
                "/swagger-ui",
                "/v3/api-docs",
                "/api/auth/verify",  // Add this line

        };

        for (String path : skipPaths) {
            if (requestURI.startsWith(path)) {
                logger.info("🔓 Skipping token validation for path: {}", requestURI);
                chain.doFilter(request, response);
                return;
            }
        }

        // Rest of your existing code remains unchanged
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.debug("🔑 Authorization Header: {}", header);

        if (header == null || !header.startsWith("Bearer ")) {
            logger.warn("❌ No valid Authorization header found");
            chain.doFilter(request, response);
            return;
        }

        final String token = header.substring(7);
        logger.debug("✅ Extracted Token: {}", token);

        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            logger.warn("🚫 Blacklisted token attempt: {}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token has been blacklisted");
            return;
        }

        try {
            // Extract and validate username from token
            String username = jwtTokenUtil.getUsernameFromToken(token);
            logger.info("✅ Extracted Username: {}", username);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Set authentication
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authToken);
            logger.info("🔐 User authenticated successfully: {}", username);

            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("❌ Token validation error", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
        }
    }

}
