package com.jgy36.PoliticalApp.dto;

import lombok.Data;

@Data
public class TwoFAVerificationRequest {
    private String tempToken;
    private String code;
}
