package com.jgy36.PoliticalApp.service;

import com.jgy36.PoliticalApp.entity.Role;
import com.jgy36.PoliticalApp.entity.User;
import com.jgy36.PoliticalApp.repository.UserRepository;
import com.jgy36.PoliticalApp.utils.OAuth2Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService, OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;
    private final OAuth2Util oAuth2Util;

    // We'll create our own PasswordEncoder here instead of injecting from SecurityConfig
    private final PasswordEncoder passwordEncoder;

    public UserDetailsServiceImpl(
            UserRepository userRepository,
            OAuth2Util oAuth2Util) {
        this.userRepository = userRepository;
        this.oAuth2Util = oAuth2Util;

        // Create a new instance of BCryptPasswordEncoder directly
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name().substring(5)) // Extract "USER" from "ROLE_USER"
                .build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Default OAuth2UserService to load the user
        org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService delegate =
                new org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService();

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // Extract provider information
        String providerName = userRequest.getClientRegistration().getRegistrationId();
        logger.info("OAuth2 Login - Provider: {}", providerName);

        // Extract user details
        String email = oAuth2Util.getUserEmail(oAuth2User, providerName);
        String name = oAuth2Util.getUserName(oAuth2User, providerName);

        // If no email, throw an exception
        if (email == null) {
            logger.error("No email found for OAuth2 user");
            throw new OAuth2AuthenticationException("No email found for OAuth2 user");
        }

        // Try to find existing user or create a new one
        User user = findOrCreateUser(email, name, providerName);

        // Create UserDetails with OAuth2 user information
        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().toString())),
                oAuth2User.getAttributes(),
                "email"
        );
    }

    private User findOrCreateUser(String email, String name, String providerName) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Create a new user if not exists
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(generateUniqueUsername(name));

        // Generate a random password for OAuth users
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Set default role
        newUser.setRole(Role.ROLE_USER);

        // Set email as verified for OAuth users
        newUser.setVerified(true);

        return userRepository.save(newUser);
    }

    private String generateUniqueUsername(String name) {
        // Create username based on name and random suffix
        String baseUsername = name.replaceAll("\\s+", "").toLowerCase();
        String username = baseUsername;
        int attempt = 1;

        // Make sure username is unique
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + attempt;
            attempt++;
        }

        return username;
    }
}
