package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for connecting social account
@Getter
public class SocialConnectRequest {
    private String token;

    public SocialConnectRequest() {
    }

    public SocialConnectRequest(String token) {
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
