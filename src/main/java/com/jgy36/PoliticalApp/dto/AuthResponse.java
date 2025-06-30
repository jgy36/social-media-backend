package com.jgy36.PoliticalApp.dto;

/**
 * ✅ DTO (Data Transfer Object) for returning authentication response.
 * This is sent back to the client after successful login.
 */
public class AuthResponse {

    private String token;

    // ✅ Default Constructor (Required for JSON serialization)
    public AuthResponse() {
    }

    // ✅ Parameterized Constructor (Fixes the error in AuthController)
    public AuthResponse(String token) {
        this.token = token;
    }

    // ✅ Getter and Setter

    /**
     * Gets the JWT token.
     *
     * @return JWT token as a String.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the JWT token.
     *
     * @param token JWT token value.
     */
    public void setToken(String token) {
        this.token = token;
    }
}
