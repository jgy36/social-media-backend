package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for email verification
@Getter
public class VerifyEmailRequest {
    private String code;

    public VerifyEmailRequest() {
    }

    public VerifyEmailRequest(String code) {
        this.code = code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
