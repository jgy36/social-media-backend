package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for verifying 2FA code
@Getter
public class VerifyTwoFaRequest {
    private String code;
    private String secret;

    public VerifyTwoFaRequest() {
    }

    public VerifyTwoFaRequest(String code, String secret) {
        this.code = code;
        this.secret = secret;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
