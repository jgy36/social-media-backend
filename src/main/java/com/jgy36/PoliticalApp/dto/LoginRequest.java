package com.jgy36.PoliticalApp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ✅ DTO for login requests.
 */
@Getter
@Setter
@NoArgsConstructor // ✅ Fix: Ensures Spring can deserialize JSON requests
public class LoginRequest {

    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
