package com.jgy36.PoliticalApp.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private boolean requires2FA;
    private String tempToken; // Temporary token for 2FA verification

    public LoginResponse(String token) {
        this.token = token;
        this.requires2FA = false;
    }

    public LoginResponse(boolean requires2FA, String tempToken) {
        this.requires2FA = requires2FA;
        this.tempToken = tempToken;
    }
}
