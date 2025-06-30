package com.jgy36.PoliticalApp.dto;

import lombok.Getter;

// DTO for changing email
@Getter
public class ChangeEmailRequest {
    private String newEmail;

    public ChangeEmailRequest() {
    }

    public ChangeEmailRequest(String newEmail) {
        this.newEmail = newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
